package com.espressodev.bluetooth.event_bus

import com.espressodev.bluetooth.domain.model.GameState
import java.util.UUID

enum class DialogState {
    Idle, Dismiss, Confirm, Open
}

data class GameUtility(
    val localPlayer: Int = 0,
    val opponentPlayer: Int = 0,
    val localUsername: String = UUID.randomUUID().toString(),
    val opponentEndpointId: String = "",
    val authDialogState: DialogState = DialogState.Idle
)

sealed class GameEvent {
    data class OnLocalPlayerChanged(val localPlayer: Int) : GameEvent()
    data class OnOpponentPlayerChanged(val opponentPlayer: Int) : GameEvent()
    data class OnLocalUsernameChanged(val localUsername: String) : GameEvent()
    data class OnOpponentEndPointChanged(val opponentEndPoint: String) : GameEvent()
    data class OnGameStateChanged(val gameState: GameState) : GameEvent()

    data class OnAuthDialogStateChanged(val state: DialogState) : GameEvent()
    data object Reset : GameEvent()
}