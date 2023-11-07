package com.espressodev.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressodev.bluetooth.data.BTController
import com.espressodev.bluetooth.data.model.BTDevice
import com.espressodev.bluetooth.data.model.BTMessage
import com.espressodev.bluetooth.data.model.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BTUiState(
    val scannedDevices: List<BTDevice> = emptyList(),
    val pairedDevices: List<BTDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<BTMessage> = emptyList()
)

@HiltViewModel
class BTViewModel @Inject constructor(
    private val btController: BTController
) : ViewModel() {
    private val _state = MutableStateFlow(BTUiState())
    val state = combine(
        btController.scannedDevices,
        btController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            messages = if (state.isConnected) state.messages else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private var deviceConnectionJob: Job? = null

    init {
        btController.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        btController.errors.onEach { error ->
            _state.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: BTDevice) {
        _state.update { it.copy(isConnected = true) }
        deviceConnectionJob = btController.connectToDevice(device).listen()
    }

    fun sendMessage(message: String) = viewModelScope.launch {
        val btMessage = btController.trySendMessage(message)
        btMessage?.let {
            _state.update { it.copy(messages = it.messages + btMessage) }
        }
    }

    fun startScan(): Unit {
        Log.d("BTViewModel", "startScan")
        btController.startDiscovery()
    }

    fun stopScan(): Unit = btController.stopDiscovery()

    fun waitForIncomingConnections() {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = btController
            .startBTServer()
            .listen()
    }

    private fun Flow<ConnectionResult>.listen(): Job = onEach { result ->
        when (result) {
            ConnectionResult.ConnectionEstablished -> {
                _state.update {
                    it.copy(
                        isConnected = true,
                        isConnecting = false,
                        errorMessage = null
                    )
                }
            }

            is ConnectionResult.TransferSucceeded -> {
                _state.update { it.copy(messages = it.messages + result.message) }
            }

            is ConnectionResult.Error -> {
                _state.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }.catch {
        btController.closeConnection()
        _state.update { it.copy(isConnected = false, isConnecting = false) }
    }.launchIn(viewModelScope)

    override fun onCleared() {
        super.onCleared()
        btController.release()
    }
}
