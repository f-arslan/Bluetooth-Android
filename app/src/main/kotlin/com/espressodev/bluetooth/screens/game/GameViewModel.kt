package com.espressodev.bluetooth.screens.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.domain.model.TicTacToe
import com.espressodev.bluetooth.playground.GameEventBusController
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates


@HiltViewModel
class GameViewModel @Inject constructor(private val connectionsClient: ConnectionsClient) :
    ViewModel() {
    private var game = TicTacToe()
    lateinit var gameState: StateFlow<GameState>
    var localPlayer by Delegates.notNull<Int>()
    lateinit var opponentEndpointId: String

    init {
        viewModelScope.launch {
            with(GameEventBusController) {
                this@GameViewModel.gameState = gameState
                combine(gameUtility, game, gameState) { gameUtility, game, gameState ->
                    localPlayer = gameUtility.localPlayer
                    opponentEndpointId = gameUtility.opponentEndpointId
                    this@GameViewModel.game = game
                }
            }
            GameEventBusController.gameUtility.collectLatest {
                localPlayer = it.localPlayer
                opponentEndpointId = it.opponentEndpointId
                Log.d(TAG, "localPlayer: $localPlayer")
                Log.d(TAG, "opponentEndpointId: $opponentEndpointId")
            }
        }
    }

    fun newGame(localPlayer: Int) {
        Log.d(TAG, "newGame")
        game.reset()
        _gameState.update {
            GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
        }
    }

    fun playMoveAndSend(position: Pair<Int, Int>) {
        if (game.playerTurn != localPlayer) return
        if (game.isPlayedBucket(position)) return
    }

    private fun playMove(player: Int, position: Pair<Int, Int>) {
        Log.d(TAG, "Player $player played [${position.first},${position.second}]")

        game.play(player, position)
        _gameState.value =
            GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
    }

    private fun sendPosition(position: Pair<Int, Int>) {
        Log.d(TAG, "Sending [${position.first},${position.second}] to $opponentEndpointId")
        connectionsClient.sendPayload(
            opponentEndpointId,
            position.toPayLoad()
        )
    }


    companion object {
        const val TAG = "TicTacToeViewModel"
        fun Pair<Int, Int>.toPayLoad() =
            Payload.fromBytes("$first,$second".toByteArray(Charsets.UTF_8))
    }
}