package com.espressodev.bluetooth.screens.game

import androidx.lifecycle.ViewModel
import com.espressodev.bluetooth.TicTacToeViewModel
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.domain.model.TicTacToe
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class GameViewModel @Inject constructor() : ViewModel() {
    private val game = TicTacToe()
    private val _state = MutableStateFlow(GameState.Uninitialized)
    val state = _state.asStateFlow()

    fun newGame(localPlayer: Int) {

    }

}