package com.espressodev.bluetooth.event_bus

import com.espressodev.bluetooth.domain.model.GameState
import java.util.UUID

data class GameUtilityState(
    val localPlayer: Int,
    val opponentPlayer: Int,
    val localUsername: String = UUID.randomUUID().toString(),
    val opponentEndpointId: String
) {
    companion object {
        val Uninitialized =
            GameUtilityState(localPlayer = 0, opponentPlayer = 0, opponentEndpointId = "")
    }
}

sealed class GameEvent {
    data class OnLocalPlayerChanged(val localPlayer: Int) : GameEvent()
    data class OnOpponentPlayerChanged(val opponentPlayer: Int) : GameEvent()
    data class OnLocalUsernameChanged(val localUsername: String) : GameEvent()
    data class OnOpponentEndPointChanged(val opponentEndPoint: String) : GameEvent()
    data class OnGameStateChanged(val gameState: GameState) : GameEvent()
    data object Reset : GameEvent()
}