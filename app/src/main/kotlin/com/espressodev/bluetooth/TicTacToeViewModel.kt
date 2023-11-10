package com.espressodev.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressodev.bluetooth.BuildConfig.APPLICATION_ID
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.espressodev.bluetooth.playground.GameEvent
import com.espressodev.bluetooth.playground.GameEventBusController
import com.espressodev.bluetooth.playground.GameEventBusController.game
import com.espressodev.bluetooth.playground.payloadCallback
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
open class TicTacToeViewModel @Inject constructor(private val connectionsClient: ConnectionsClient) :
    ViewModel() {
    lateinit var localUsername: String
    private var localPlayer by Delegates.notNull<Int>()
    private var opponentPlayer by Delegates.notNull<Int>()
    lateinit var opponentEndpointId: String

    init {
        viewModelScope.launch {
            GameEventBusController.gameUtility.collectLatest { gameUtility ->
                localPlayer = gameUtility.localPlayer
                opponentPlayer = gameUtility.opponentPlayer
                opponentEndpointId = gameUtility.opponentEndpointId
                localUsername = gameUtility.localUsername
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "onConnectionInitiated")

            Log.d(TAG, "Accepting connection...")
            connectionsClient.acceptConnection(endpointId, GameEventBusController.payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            Log.d(TAG, "onConnectionResult")

            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "ConnectionsStatusCodes.STATUS_OK")

                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    GameEventBusController.onEvent(GameEvent.OnOpponentEndPointChanged(endpointId))
                    Log.d(TAG, "opponentEndpointId: $opponentEndpointId")
                    newGame()
                    TicTacToeRouter.navigateTo(Screen.Game)
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED")
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "ConnectionsStatusCodes.STATUS_ERROR")
                }

                else -> {
                    Log.d(TAG, "Unknown status code ${resolution.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "onDisconnected")
            goToHome()
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "onEndpointFound")
            Log.d(TAG, "Requesting connection...")
            connectionsClient.requestConnection(
                localUsername,
                endpointId,
                connectionLifecycleCallback
            ).addOnSuccessListener {
                Log.d(TAG, "Successfully requested a connection")
            }.addOnFailureListener {
                Log.d(TAG, "Failed to request the connection")
            }
        }


        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "onEndpointLost")
        }
    }

    fun startHosting() {
        Log.d(TAG, "Start advertising...")
        TicTacToeRouter.navigateTo(Screen.Hosting)
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            localUsername,
            APPLICATION_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising...")
            GameEventBusController.onEvent(GameEvent.OnLocalPlayerChanged(1))
            GameEventBusController.onEvent(GameEvent.OnOpponentPlayerChanged(2))
        }.addOnFailureListener {
            Log.d(TAG, "Unable to start advertising: $it")
            TicTacToeRouter.navigateTo(Screen.Home)
        }
    }

    fun startDiscovering() {
        Log.d(TAG, "Start discovering...")
        TicTacToeRouter.navigateTo(Screen.Discovering)
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            APPLICATION_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovering...")
            GameEventBusController.onEvent(GameEvent.OnLocalPlayerChanged(2))
            GameEventBusController.onEvent(GameEvent.OnOpponentPlayerChanged(1))
        }.addOnFailureListener {
            Log.d(TAG, "Unable to start discovering: $it")
            TicTacToeRouter.navigateTo(Screen.Home)
        }
    }

    fun newGame() {
        Log.d(TAG, "Starting new game")
        game.reset()
        GameEventBusController.onEvent(GameEvent.OnGameStateChanged(
            GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
        ))
    }

    override fun onCleared() {
        stopClient()
        super.onCleared()
    }

    fun goToHome() {
        stopClient()
        TicTacToeRouter.navigateTo(Screen.Home)
    }

    private fun stopClient() {
        Log.d(TAG, "Stop advertising, discovering, all endpoints")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        GameEventBusController.onEvent(GameEvent.Reset)
    }

    private companion object {
        const val TAG = "TicTacToeVM"
        val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}