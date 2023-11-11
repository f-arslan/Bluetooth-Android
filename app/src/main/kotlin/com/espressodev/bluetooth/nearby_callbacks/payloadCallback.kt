package com.espressodev.bluetooth.nearby_callbacks

import android.util.Log
import com.espressodev.bluetooth.domain.model.GameState
import com.espressodev.bluetooth.playground.GameEvent
import com.espressodev.bluetooth.playground.GameEventBusController
import com.espressodev.bluetooth.playground.toPosition
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate

val GameEventBusController.payloadCallback: PayloadCallback
    get() = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
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
            Log.d(
                "GameEventBusController",
                "onPayloadReceived: $endpointId, $payload"
            )
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(
                "GameEventBusController",
                "onPayloadTransferUpdate: $endpointId, $update"
            )
        }
    }

