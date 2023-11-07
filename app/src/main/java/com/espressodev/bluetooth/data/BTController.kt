package com.espressodev.bluetooth.data

import com.espressodev.bluetooth.data.model.BTDevice
import com.espressodev.bluetooth.data.model.BTMessage
import com.espressodev.bluetooth.data.model.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BTController {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BTDevice>>
    val pairedDevices: StateFlow<List<BTDevice>>
    val errors: SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun startBTServer(): Flow<ConnectionResult>
    fun connectToDevice(device: BTDevice): Flow<ConnectionResult>

    suspend fun trySendMessage(message: String): BTMessage?

    fun closeConnection()
    fun release()
}