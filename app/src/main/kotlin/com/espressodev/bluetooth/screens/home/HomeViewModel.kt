package com.espressodev.bluetooth.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressodev.bluetooth.domain.model.TicTacToe
import com.espressodev.bluetooth.event_bus.DialogState
import com.espressodev.bluetooth.event_bus.GameEventBus.gameUtility
import com.espressodev.bluetooth.nearby.NearbyConnectionEvent
import com.espressodev.bluetooth.nearby.NearbyConnectionEventBus
import com.espressodev.bluetooth.nearby.NearbyLifecycle
import com.espressodev.bluetooth.nearby.NearbyLifecycleImpl
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    connectionsClient: ConnectionsClient,
    game: TicTacToe
) : ViewModel(), NearbyLifecycle by NearbyLifecycleImpl(connectionsClient, game) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeNearbyConnectionEvent()
        observeGameUtility()
    }

    fun onDialogStateChanged(state: DialogState) {
        _uiState.update { it.copy(dialogState = state) }
    }

    fun onHomeViewStateChanged(homeViewState: HomeViewState) {
        _uiState.update { it.copy(homeViewState = homeViewState) }
    }

    fun startHosting() {
        _uiState.update { it.copy(homeViewState = HomeViewState.Loading) }
        startHost()
    }

    fun navigateToGame() {
    }

    fun startDiscovering() {
        startDiscovery()
    }

    override fun onCleared() {
        stopClient()
        super.onCleared()
    }

    private fun observeGameUtility() = viewModelScope.launch {
        gameUtility.collectLatest { util ->
            _uiState.update {
                it.copy(
                    dialogState = util.authDialogState,
                    opponentEndpointId = util.opponentEndpointId
                )
            }
        }
    }

    private fun observeNearbyConnectionEvent() = viewModelScope.launch {
        NearbyConnectionEventBus.nearbyLifecycleEvent.collectLatest {
            if (it == NearbyConnectionEvent.NavigateToGame) {
                navigateToGame()
            }
        }
    }

    companion object {
        const val TAG = "HomeViewModel"
    }
}
