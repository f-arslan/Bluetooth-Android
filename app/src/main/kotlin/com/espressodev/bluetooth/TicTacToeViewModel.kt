package com.espressodev.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressodev.bluetooth.domain.model.TicTacToe
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.espressodev.bluetooth.nearby.NearbyConnectionEvent
import com.espressodev.bluetooth.nearby.NearbyConnectionEventBus.nearbyLifecycleEvent
import com.espressodev.bluetooth.nearby.NearbyLifecycle
import com.espressodev.bluetooth.nearby.NearbyLifecycleImpl
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TicTacToeViewModel @Inject constructor(
    private val connectionsClient: ConnectionsClient,
    private val game: TicTacToe
) :
    ViewModel(), NearbyLifecycle by NearbyLifecycleImpl(connectionsClient, game) {
    init {
        observeNearbyConnectionEvent()
    }

    private fun observeNearbyConnectionEvent() = viewModelScope.launch {
        nearbyLifecycleEvent.collectLatest {
            if (it == NearbyConnectionEvent.NavigateToGame) {
                navigateToGame()
            }
        }
    }

    fun navigateToGame() {
        TicTacToeRouter.navigateTo(Screen.Game)
    }

    fun startHosting() {
        TicTacToeRouter.navigateTo(Screen.Hosting)
        startHost()
    }

    fun startDiscovering() {
        TicTacToeRouter.navigateTo(Screen.Discovering)
        startDiscovery()
    }

    override fun onCleared() {
        stopClient()
        super.onCleared()
    }

    private companion object {
        const val TAG = "TicTacToeViewModel"
    }
}