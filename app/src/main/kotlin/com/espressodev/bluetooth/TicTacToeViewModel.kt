package com.espressodev.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.espressodev.bluetooth.BuildConfig.APPLICATION_ID
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.espressodev.bluetooth.nearby.ConnectionLifecycleEvent
import com.espressodev.bluetooth.nearby.ConnectionLifecycleEventImpl
import com.espressodev.bluetooth.event_bus.GameEvent
import com.espressodev.bluetooth.event_bus.GameEventBusController.gameUtility
import com.espressodev.bluetooth.event_bus.GameEventBusController.onEvent
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TicTacToeViewModel @Inject constructor(private val connectionsClient: ConnectionsClient) :
    ViewModel(), ConnectionLifecycleEvent by ConnectionLifecycleEventImpl(connectionsClient) {
    // It's not changing entire app we can directly use it
    private val localUsername = gameUtility.value.localUsername

    fun startHosting() {
        TicTacToeRouter.navigateTo(Screen.Hosting)
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            localUsername,
            APPLICATION_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            onEvent(GameEvent.OnLocalPlayerChanged(1))
            onEvent(GameEvent.OnOpponentPlayerChanged(2))
            Log.d(TAG, "Advertising...")
        }.addOnFailureListener {
            TicTacToeRouter.navigateTo(Screen.Home)
            Log.d(TAG, "Unable to start advertising: $it")
        }
    }

    fun startDiscovering() {
        TicTacToeRouter.navigateTo(Screen.Discovering)
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            APPLICATION_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovering...")
            onEvent(GameEvent.OnLocalPlayerChanged(2))
            onEvent(GameEvent.OnOpponentPlayerChanged(1))
        }.addOnFailureListener {
            Log.d(TAG, "Unable to start discovering: $it")
            TicTacToeRouter.navigateTo(Screen.Home)
        }
    }

    override fun onCleared() {
        stopClient()
        super.onCleared()
    }

    private companion object {
        const val TAG = "TicTacToeViewModel"
        val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}