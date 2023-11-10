package com.espressodev.bluetooth.playground

import android.util.Log
import androidx.lifecycle.ViewModel
import com.espressodev.bluetooth.BuildConfig
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.domain.model.TicTacToe
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

class TicTacToeSystemBuilder {
}


interface GameObserver {
    fun connectionsClient(connectionsClient: ConnectionsClient)
    fun opponentEndpointId(opponentEndPoint: String)
    fun localPlayer(localPlayer: Int)

}

@HiltViewModel
class ConnectionPublisher @Inject constructor(private val connectionsClient: ConnectionsClient) :
    ViewModel() {
    private val observers: MutableList<GameObserver> = mutableListOf()
    private val localUsername = UUID.randomUUID().toString()
    private var opponentEndpointId: String = ""
    protected var localPlayer: Int = 0
    protected var opponentPlayer: Int = 0


    fun startHosting() {
        Log.d(TAG, "Start advertising...")
        TicTacToeRouter.navigateTo(Screen.Hosting)
        val advertisingOptions =
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            localUsername,
            BuildConfig.APPLICATION_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising...")
            localPlayer = 1
            opponentPlayer = 2
        }.addOnFailureListener {
            Log.d(TAG, "Unable to start advertising: $it")
            TicTacToeRouter.navigateTo(Screen.Home)
        }
    }


    fun startDiscovering() {
        Log.d(TAG, "Start discovering...")
        TicTacToeRouter.navigateTo(Screen.Discovering)
        val discoveryOptions =
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            BuildConfig.APPLICATION_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovering...")
            localPlayer = 2
            opponentPlayer = 1
        }.addOnFailureListener {
            Log.d(TAG, "Unable to start discovering: $it")
            TicTacToeRouter.navigateTo(Screen.Home)
        }
    }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(TAG, "onPayloadReceived")
            if (payload.type == Payload.Type.BYTES) {
                val position = payload.toPosition()
                Log.d(TAG, "Received [${position.first},${position.second}] from $endpointId")
                // play(opponentPlayer, position)
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
                    // newGame()
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

    fun goToHome() {
        stopClient()
        TicTacToeRouter.navigateTo(Screen.Home)
    }

    override fun onCleared() {
        stopClient()
        super.onCleared()
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

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "onEndpointFound")
            Log.d(TAG, "Requesting connection...")
            connectionsClient.requestConnection(
                localUsername,
                endpointId,
                connectionLifecycleCallback
            )
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully requested a connection")
                }.addOnFailureListener {
                    Log.d(TAG, "Failed to request the connection")
                }
        }


        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "onEndpointLost")
        }
    }


    companion object {
        const val TAG = "ConnectionPublisher"
        val STRATEGY = Strategy.P2P_POINT_TO_POINT
        fun Pair<Int, Int>.toPayLoad() =
            Payload.fromBytes("$first,$second".toByteArray(Charsets.UTF_8))

        fun Payload.toPosition(): Pair<Int, Int> {
            val positionStr = String(asBytes()!!, Charsets.UTF_8)
            val positionArray = positionStr.split(",")
            return positionArray[0].toInt() to positionArray[1].toInt()
        }
    }
}

data class GameUtility(
    val localPlayer: Int,
    val opponentPlayer: Int,
    val localUsername: String = UUID.randomUUID().toString(),
    val opponentEndpointId: String
) {
    companion object {
        val Uninitialized =
            GameUtility(localPlayer = 0, opponentPlayer = 0, opponentEndpointId = "")
    }
}

sealed class GameEvent {
    data class OnLocalPlayerChanged(val localPlayer: Int) : GameEvent()
    data class OnOpponentPlayerChanged(val opponentPlayer: Int) : GameEvent()
    data class OnLocalUsernameChanged(val localUsername: String) : GameEvent()
    data class OnOpponentEndPointChanged(val opponentEndPoint: String) : GameEvent()

    data class OnGameStateChanged(val gameState: GameState): GameEvent()

    data class OnGameChanged(val game: TicTacToe): GameEvent()
    data object Reset : GameEvent()
}

object GameEventBusController {
    private val _gameUtility = MutableStateFlow(GameUtility.Uninitialized)
    val gameUtility = _gameUtility.asStateFlow()

    private val _game = MutableStateFlow(TicTacToe())
    val game = _game.asStateFlow()

    private val _gameState = MutableStateFlow(GameState.Uninitialized)
    val gameState = _gameState.asStateFlow()

    fun onEvent(event: GameEvent) = when (event) {
        is GameEvent.OnLocalPlayerChanged -> _gameUtility.update { it.copy(localPlayer = event.localPlayer) }
        is GameEvent.OnOpponentPlayerChanged -> _gameUtility.update { it.copy(opponentPlayer = event.opponentPlayer) }
        is GameEvent.OnLocalUsernameChanged -> _gameUtility.update { it.copy(localUsername = event.localUsername) }
        is GameEvent.OnOpponentEndPointChanged -> _gameUtility.update {
            it.copy(opponentEndpointId = event.opponentEndPoint)
        }
        is GameEvent.OnGameStateChanged -> _gameState.update { event.gameState }
        is GameEvent.OnGameChanged -> _game.update { event.game }
        GameEvent.Reset -> _gameUtility.update { GameUtility.Uninitialized }
    }

}

class GamePublisher {

}