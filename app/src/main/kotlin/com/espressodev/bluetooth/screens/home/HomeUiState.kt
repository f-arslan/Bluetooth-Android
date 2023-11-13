package com.espressodev.bluetooth.screens.home

import com.espressodev.bluetooth.event_bus.DialogState


data class HomeUiState(
    val dialogState: DialogState = DialogState.Idle,
    val homeViewState: HomeViewState = HomeViewState.Idle,
    val opponentEndpointId: String = ""
)

sealed interface HomeViewState {
    data object Idle: HomeViewState
    data object Loading: HomeViewState
    data object ReadyForGame: HomeViewState
}