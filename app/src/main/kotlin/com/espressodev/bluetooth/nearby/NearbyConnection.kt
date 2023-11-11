package com.espressodev.bluetooth.nearby

import android.util.Log
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.espressodev.bluetooth.playground.GameEvent
import com.espressodev.bluetooth.playground.GameEventBusController.game
import com.espressodev.bluetooth.playground.GameEventBusController.gameUtility
import com.espressodev.bluetooth.playground.GameEventBusController.onEvent
import com.espressodev.bluetooth.playground.toPosition
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

interface ConnectionLifecycleEvent {
    fun navigateToGame()
    fun navigateToHome()
    fun stopClient()
    val connectionLifecycleCallback: ConnectionLifecycleCallback
    val endpointDiscoveryCallback: EndpointDiscoveryCallback
    val payloadCallback: PayloadCallback
}


class ConnectionLifecycleEventImpl(
    private val connectionsClient: ConnectionsClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) :
    ConnectionLifecycleEvent {
    private lateinit var opponentEndpointId: String
    private var localPlayer by Delegates.notNull<Int>()
    lateinit var localUsername: String

    init {
        observeGameUtility()
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

    override val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val position = payload.toPosition()
            game.play(gameUtility.value.opponentPlayer, position)
            onEvent(
                GameEvent.OnGameStateChanged(
                    GameState(
                        gameUtility.value.localPlayer,
                        game.playerTurn,
                        game.playerWon,
                        game.isOver,
                        game.board
                    )
                )
            )
            Log.d(TAG, "onPayloadReceived: $endpointId, $payload")
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate: $endpointId, $update")
        }
    }

    override val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    onEvent(GameEvent.OnOpponentEndPointChanged(endpointId))
                    navigateToGame()
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
            navigateToHome()
        }
    }

    override val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
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

    override fun navigateToGame() {
        game.reset()
        updateGameState()
        TicTacToeRouter.navigateTo(Screen.Game)
    }

    override fun navigateToHome() {
        stopClient()
        TicTacToeRouter.navigateTo(Screen.Home)
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
    }
}