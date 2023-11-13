package com.espressodev.bluetooth.event_bus

import com.espressodev.bluetooth.domain.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


object GameEventBus {
    private val _gameUtility = MutableStateFlow(GameUtility())
    val gameUtility = _gameUtility.asStateFlow()

    private val _gameState = MutableStateFlow(GameState.Uninitialized)
    val gameState = _gameState.asStateFlow()

    fun onEvent(event: GameEvent) = when (event) {
        is GameEvent.OnLocalPlayerChanged -> _gameUtility.update { it.copy(localPlayer = event.localPlayer) }
        is GameEvent.OnOpponentPlayerChanged -> _gameUtility.update { it.copy(opponentPlayer = event.opponentPlayer) }
        is GameEvent.OnLocalUsernameChanged -> _gameUtility.update { it.copy(localUsername = event.localUsername) }
        is GameEvent.OnOpponentEndPointChanged -> _gameUtility.update {
            it.copy(opponentEndpointId = event.opponentEndPoint)
        }

        is GameEvent.OnAuthDialogStateChanged -> _gameUtility.update { it.copy(authDialogState = event.state) }
        is GameEvent.OnGameStateChanged -> _gameState.update { event.gameState }
        GameEvent.Reset -> _gameUtility.update { GameUtility() }
    }
}
