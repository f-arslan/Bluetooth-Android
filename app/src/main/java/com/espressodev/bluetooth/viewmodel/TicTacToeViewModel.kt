package com.espressodev.bluetooth.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.domain.model.TicTacToe
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Strategy
import java.util.*
import com.espressodev.bluetooth.BuildConfig.APPLICATION_ID
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlin.text.Charsets.UTF_8

class TicTacToeViewModel(private val connectionsClient: ConnectionsClient) : ViewModel() {
    private val localUsername = UUID.randomUUID().toString()
    private var localPlayer: Int = 0
    private var opponentPlayer: Int = 0
    private var opponentEndpointId: String = ""

    private var game = TicTacToe()

    private val _state = MutableLiveData(GameState.Uninitialized)
    val state: LiveData<GameState> = _state

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(TAG, "onPayloadReceived")

            // 1
            if (payload.type == Payload.Type.BYTES) {
                // 2
                val position = payload.toPosition()
                Log.d(TAG, "Received [${position.first},${position.second}] from $endpointId")
                // 3
                play(opponentPlayer, position)
            }
        }


        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate")
        }
    }


    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "onConnectionInitiated")

            Log.d(TAG, "Accepting connection...")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            Log.d(TAG, "onConnectionResult")

            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "ConnectionsStatusCodes.STATUS_OK")

                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    opponentEndpointId = endpointId
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
            localPlayer = 1
            opponentPlayer = 2
        }.addOnFailureListener {
            Log.d(TAG, "Unable to start advertising")
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
            localPlayer = 2
            opponentPlayer = 1
        }.addOnFailureListener {
            Log.d(TAG, "Unable to start discovering")
            TicTacToeRouter.navigateTo(Screen.Home)
        }
    }

    fun newGame() {
        Log.d(TAG, "Starting new game")
        game = TicTacToe()
        _state.value =
            GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
    }

    fun play(position: Pair<Int, Int>) {
        if (game.playerTurn != localPlayer) return
        if (game.isPlayedBucket(position)) return

        play(localPlayer, position)
        sendPosition(position)
    }

    private fun play(player: Int, position: Pair<Int, Int>) {
        Log.d(TAG, "Player $player played [${position.first},${position.second}]")

        game.play(player, position)
        _state.value =
            GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
    }

    private fun sendPosition(position: Pair<Int, Int>) {
        Log.d(TAG, "Sending [${position.first},${position.second}] to $opponentEndpointId")
        connectionsClient.sendPayload(
            opponentEndpointId,
            position.toPayLoad()
        )
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
        localPlayer = 0
        opponentPlayer = 0
        opponentEndpointId = ""
    }

    private companion object {
        const val TAG = "TicTacToeVM"
        val STRATEGY = Strategy.P2P_POINT_TO_POINT
        fun Pair<Int, Int>.toPayLoad() = Payload.fromBytes("$first,$second".toByteArray(UTF_8))
        fun Payload.toPosition(): Pair<Int, Int> {
            val positionStr = String(asBytes()!!, UTF_8)
            val positionArray = positionStr.split(",")
            return positionArray[0].toInt() to positionArray[1].toInt()
        }
    }
}