package com.espressodev.bluetooth.nearby

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class NearbyConnectionEvent {
    data object Idle : NearbyConnectionEvent()
    data object NavigateToHome : NearbyConnectionEvent()
    data object NavigateToGame : NearbyConnectionEvent()
}

object NearbyConnectionEventBus {
    private val _nearbyLifecycleEvent =
        MutableStateFlow<NearbyConnectionEvent>(NearbyConnectionEvent.Idle)
    val nearbyLifecycleEvent = _nearbyLifecycleEvent.asStateFlow()
    fun onEvent(event: NearbyConnectionEvent) = when (event) {
        NearbyConnectionEvent.Idle -> {}
        NearbyConnectionEvent.NavigateToGame -> _nearbyLifecycleEvent.update { NearbyConnectionEvent.NavigateToGame }
        NearbyConnectionEvent.NavigateToHome -> _nearbyLifecycleEvent.update { NearbyConnectionEvent.NavigateToHome }
    }
}