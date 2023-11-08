package com.espressodev.bluetooth.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class Screen {
    data object Home : Screen()
    data object Hosting : Screen()
    data object Discovering : Screen()
    data object Game : Screen()
}

object TicTacToeRouter {
    var currentScreen: Screen by mutableStateOf(Screen.Home)

    fun navigateTo(destination: Screen) {
        currentScreen = destination
    }
}