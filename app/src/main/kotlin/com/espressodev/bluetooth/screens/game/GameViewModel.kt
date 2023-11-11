package com.espressodev.bluetooth.screens.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.espressodev.bluetooth.playground.GameEvent
import com.espressodev.bluetooth.playground.GameEventBusController
import com.espressodev.bluetooth.playground.GameEventBusController.game
import com.espressodev.bluetooth.playground.GameEventBusController.gameUtility
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates


@HiltViewModel
class GameViewModel @Inject constructor(private val connectionsClient: ConnectionsClient) :
    ViewModel() {
    private var localPlayer by Delegates.notNull<Int>()
    private lateinit var opponentEndpointId: String

    init {
        viewModelScope.launch {
            gameUtility.collectLatest {
                localPlayer = it.localPlayer
                opponentEndpointId = it.opponentEndpointId
            }
        }
    }

    fun newGame() {
        game.reset()
        GameEventBusController.onEvent(
            GameEvent.OnGameStateChanged(
                GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
            )
        )
        Log.d(TAG, "newGame")
    }

    fun playMoveAndSend(position: Pair<Int, Int>) {
        if (game.playerTurn != localPlayer) return
        if (game.isPlayedBucket(position)) return

        playMove(localPlayer, position)
        sendPosition(position)
    }

    private fun playMove(player: Int, position: Pair<Int, Int>) {
        game.play(player, position)
        GameEventBusController.onEvent(GameEvent.OnGameStateChanged(
            GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
        ))
        Log.d(TAG, "Player $player played [${position.first},${position.second}]")
    }

    fun goToHome() {
        stopClient()
        TicTacToeRouter.navigateTo(Screen.Home)
    }

    private fun stopClient() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        GameEventBusController.onEvent(GameEvent.Reset)
        Log.d(TAG, "Stop advertising, discovering, all endpoints")
    }

    private fun sendPosition(position: Pair<Int, Int>) {
        connectionsClient.sendPayload(
            opponentEndpointId,
            position.toPayLoad()
        )
        Log.d(TAG, "Sending [${position.first},${position.second}] to $opponentEndpointId")
    }

    companion object {
        const val TAG = "TicTacToeViewModel"
        fun Pair<Int, Int>.toPayLoad() =
            Payload.fromBytes("$first,$second".toByteArray(Charsets.UTF_8))
    }
}