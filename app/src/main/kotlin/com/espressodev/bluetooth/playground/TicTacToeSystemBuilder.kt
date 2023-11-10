package com.espressodev.bluetooth.playground

import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.domain.model.TicTacToe
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

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

    data class OnGameStateChanged(val gameState: GameState) : GameEvent()

    data class OnGameChanged(val game: TicTacToe) : GameEvent()
    data object Reset : GameEvent()
}

object GameEventBusController {
    private val _gameUtility = MutableStateFlow(GameUtility.Uninitialized)
    val gameUtility = _gameUtility.asStateFlow()
    val game = TicTacToe()

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
        is GameEvent.OnGameChanged -> {
            game.reset()
        }

        GameEvent.Reset -> _gameUtility.update { GameUtility.Uninitialized }
    }

}

fun Pair<Int, Int>.toPayLoad() = Payload.fromBytes("$first,$second".toByteArray(Charsets.UTF_8))
fun Payload.toPosition(): Pair<Int, Int> {
    val positionStr = String(asBytes()!!, Charsets.UTF_8)
    val positionArray = positionStr.split(",")
    return positionArray[0].toInt() to positionArray[1].toInt()
}
val GameEventBusController.payloadCallback: PayloadCallback
    get() = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val position = payload.toPosition()
                game.play(gameUtility.value.opponentPlayer, position)
                onEvent(
                    GameEvent.OnGameStateChanged(
                        GameState(
                            gameUtility.value.localPlayer,
                            game.playerTurn,
                            game.playerWon,
                            game.isOver,
                            game.board
                        )
                    )
                )
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        }
    }

class GamePublisher {

}