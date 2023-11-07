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

class TicTacToeViewModel(private val connectionsClient: ConnectionsClient) : ViewModel() {
    private val localUsername = UUID.randomUUID().toString()
    private var localPlayer: Int = 0
    private var opponentPlayer: Int = 0
    private var opponentEndpointId: String = ""

    private var game = TicTacToe()

    private val _state = MutableLiveData(GameState.Uninitialized)
    val state: LiveData<GameState> = _state

    fun startHosting() {
        Log.d(TAG, "Start advertising...")
        TicTacToeRouter.navigateTo(Screen.Hosting)
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
    }

    fun startDiscovering() {
        Log.d(TAG, "Start discovering...")
        TicTacToeRouter.navigateTo(Screen.Discovering)
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
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
    }

    fun goToHome() {
        TicTacToeRouter.navigateTo(Screen.Home)
    }

    private companion object {
        const val TAG = "TicTacToeVM"
        val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}