package com.espressodev.bluetooth.nearby

import android.util.Log
import com.espressodev.bluetooth.BuildConfig
import com.espressodev.bluetooth.TicTacToeViewModel
import com.espressodev.bluetooth.common.ext.toPosition
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.event_bus.GameEvent
import com.espressodev.bluetooth.event_bus.GameEventBusController.game
import com.espressodev.bluetooth.event_bus.GameEventBusController.gameUtility
import com.espressodev.bluetooth.event_bus.GameEventBusController.onEvent
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Singleton
import kotlin.properties.Delegates

interface NearbyLifecycle {
    fun stopClient()
    fun startHost()
    fun startDiscovery()
    val nearbyLifecycleEvent: StateFlow<NearbyConnectionEvent>
}

sealed interface NearbyConnectionEvent {
    data object Idle : NearbyConnectionEvent
    data object NavigateToHome : NearbyConnectionEvent
    data object NavigateToGame : NearbyConnectionEvent

}

class NearbyLifecycleImpl(
    private val connectionsClient: ConnectionsClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) : NearbyLifecycle {
    private lateinit var opponentEndpointId: String
    private var localPlayer by Delegates.notNull<Int>()
    lateinit var localUsername: String

    private val _nearbyLifecycleEvent =
        MutableStateFlow<NearbyConnectionEvent>(NearbyConnectionEvent.Idle)
    override val nearbyLifecycleEvent = _nearbyLifecycleEvent.asStateFlow()

    init {
        observeGameUtility()
        Log.d (TAG, "Nearby code: ${this.nearbyLifecycleEvent.hashCode()}")
    }

    private fun observeGameUtility() {
        scope.launch {
            gameUtility.collectLatest {
                opponentEndpointId = it.opponentEndpointId
                localPlayer = it.localPlayer
                localUsername = it.localUsername
            }
        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val position = payload.toPosition()
            game.play(gameUtility.value.opponentPlayer, position)
            updateGameState()
            Log.d(TAG, "onPayloadReceived: $endpointId, $payload")
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate: $endpointId, $update")
        }
    }

    override fun startHost() {
        val advertisingOptions =
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            localUsername,
            BuildConfig.APPLICATION_ID,
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

    override fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            BuildConfig.APPLICATION_ID,
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

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    onConnectionResultOK(endpointId)
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
            _nearbyLifecycleEvent.update { NearbyConnectionEvent.NavigateToHome }
            Log.d(TAG, _nearbyLifecycleEvent.value.toString())
        }
    }

    private fun onConnectionResultOK(endpointId: String) {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        game.reset()
        onEvent(GameEvent.OnOpponentEndPointChanged(endpointId))
        updateGameState()
        _nearbyLifecycleEvent.update { NearbyConnectionEvent.NavigateToGame }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
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

    override fun stopClient() {
        Log.d(TAG, "Stop advertising, discovering, all endpoints")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        onEvent(GameEvent.Reset)
    }

    private fun updateGameState() {
        onEvent(
            GameEvent.OnGameStateChanged(
                GameState(
                    localPlayer,
                    game.playerTurn,
                    game.playerWon,
                    game.isOver,
                    game.board
                )
            )
        )
    }

    private companion object {
        const val TAG = "ConnectionLifecycleCallbackImpl"
        val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}