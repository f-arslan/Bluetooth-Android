package com.espressodev.bluetooth.data.impl

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.espressodev.bluetooth.data.BTController
import com.espressodev.bluetooth.data.model.BTDevice
import com.espressodev.bluetooth.data.model.BTMessage
import com.espressodev.bluetooth.data.model.ConnectionResult
import com.espressodev.bluetooth.data.model.toBTDevice
import com.espressodev.bluetooth.data.model.toByteArray
import com.espressodev.bluetooth.receiver.BTStateReceiver
import com.espressodev.bluetooth.receiver.FoundDeviceReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BTControllerImpl(private val context: Context) : BTController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var dataTransferService: BTDataTransferServiceImpl? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val _scannedDevices = MutableStateFlow<List<BTDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BTDevice>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BTDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BTDevice>>
        get() = _pairedDevices.asStateFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBTDevice()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    private val btStateReceiver = BTStateReceiver { isConnected, bluetoothDevice ->
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.emit("Can't connect to a non-paired device.")
            }
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        updatePairedDevices()
        context.registerReceiver(
            btStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return

        context.registerReceiver(foundDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return

        bluetoothAdapter?.cancelDiscovery()
    }


    override fun startBTServer(): Flow<ConnectionResult> = flow {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) throw SecurityException("No BLUETOOTH_CONNECT permission")

        currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
            "chat_service",
            UUID.fromString(SERVICE_UUID)
        )

        var shouldLoop = true
        while (shouldLoop) {
            currentClientSocket = try {
                currentServerSocket?.accept()
            } catch (e: IOException) {
                shouldLoop = false
                null
            }
            emit(ConnectionResult.ConnectionEstablished)
            currentClientSocket?.let { btSocket ->
                currentServerSocket?.close()
                val service = BTDataTransferServiceImpl(btSocket)
                dataTransferService = service

                emitAll(
                    service
                        .listenIncomingMessages()
                        .map { btMessage -> ConnectionResult.TransferSucceeded(btMessage) }
                )
            }
        }
    }.onCompletion {
        closeConnection()
    }.flowOn(Dispatchers.IO)

    override fun connectToDevice(device: BTDevice): Flow<ConnectionResult> = flow {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) throw SecurityException("No BLUETOOTH_CONNECT permission")

        currentClientSocket = bluetoothAdapter
            ?.getRemoteDevice(device.address)
            ?.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))

        stopDiscovery()

        currentClientSocket?.let { socket ->
            try {
                socket.connect()
                emit(ConnectionResult.ConnectionEstablished)

                BTDataTransferServiceImpl(socket).also {
                    dataTransferService = it
                    emitAll(
                        it.listenIncomingMessages()
                            .map { btMessage -> ConnectionResult.TransferSucceeded(btMessage) }
                    )
                }
            } catch (e: IOException) {
                socket.close()
                currentClientSocket = null
                emit(ConnectionResult.Error("Connection was interrupted"))
            }
        }
    }.onCompletion {
        closeConnection()
    }.flowOn(Dispatchers.IO)

    override suspend fun trySendMessage(message: String): BTMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return null
        if (dataTransferService == null) return null

        val btMessage = BTMessage(
            message = message,
            senderName = bluetoothAdapter?.name ?: "Unknown name",
            isFromLocalUser = true
        )

        dataTransferService?.sendMessage(btMessage.toByteArray())
        return btMessage
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(btStateReceiver)
        closeConnection()
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        bluetoothAdapter?.bondedDevices
            ?.map { it.toBTDevice() }
            ?.also { devices -> _pairedDevices.update { devices } }
    }


    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SERVICE_UUID = "6182f8b8-7d70-11ee-b962-0242ac120002"
    }
}