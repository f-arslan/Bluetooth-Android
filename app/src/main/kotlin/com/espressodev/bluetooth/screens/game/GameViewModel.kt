package com.espressodev.bluetooth.screens.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressodev.bluetooth.common.ext.toPayLoad
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.event_bus.GameEvent
import com.espressodev.bluetooth.event_bus.GameEventBusController.game
import com.espressodev.bluetooth.event_bus.GameEventBusController.gameUtility
import com.espressodev.bluetooth.event_bus.GameEventBusController.onEvent
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.espressodev.bluetooth.nearby.NearbyConnectionEvent
import com.espressodev.bluetooth.nearby.NearbyLifecycle
import com.espressodev.bluetooth.nearby.NearbyLifecycleImpl
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates


@HiltViewModel
class GameViewModel @Inject constructor(private val connectionsClient: ConnectionsClient) :
    ViewModel(), NearbyLifecycle by NearbyLifecycleImpl(connectionsClient) {
    private var localPlayer by Delegates.notNull<Int>()
    private lateinit var opponentEndpointId: String

    init {
        observeGameUtility()
        observeNearbyLifecycleEvent()
        Log.d(TAG, "Nearby code: ${this.nearbyLifecycleEvent.hashCode()}")
    }

    private fun observeNearbyLifecycleEvent() = viewModelScope.launch {
        nearbyLifecycleEvent.collectLatest {
            if (it is NearbyConnectionEvent.NavigateToHome) {
                Log.d(TAG, it.toString())
                goToHome()
            }
        }
    }

    private fun observeGameUtility() = viewModelScope.launch {
        gameUtility.collectLatest {
            localPlayer = it.localPlayer
            opponentEndpointId = it.opponentEndpointId
        }
    }

    fun newGame() {
        game.reset()
        onEvent(
            GameEvent.OnGameStateChanged(
                GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
            )
        )
    }

    fun playMoveAndSend(position: Pair<Int, Int>) {
        if (game.playerTurn != localPlayer) return
        if (game.isPlayedBucket(position)) return

        playMove(localPlayer, position)
        sendPosition(position)
    }

    private fun playMove(player: Int, position: Pair<Int, Int>) {
        game.play(player, position)
        onEvent(
            GameEvent.OnGameStateChanged(
                GameState(localPlayer, game.playerTurn, game.playerWon, game.isOver, game.board)
            )
        )
    }

    fun goToHome() {
        stopClient()
        TicTacToeRouter.navigateTo(Screen.Home)
    }

    private fun sendPosition(position: Pair<Int, Int>) {
        connectionsClient.sendPayload(
            opponentEndpointId,
            position.toPayLoad()
        )
        Log.d(TAG, "Sending [${position.first},${position.second}] to $opponentEndpointId")
    }

    companion object {
        const val TAG = "GameViewModel"
    }
}