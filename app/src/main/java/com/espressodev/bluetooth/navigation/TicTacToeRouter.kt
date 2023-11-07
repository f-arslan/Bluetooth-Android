package com.espressodev.bluetooth.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class Screen {
    object Home : Screen()
    object Hosting : Screen()
    object Discovering : Screen()
    object Game : Screen()
}

object TicTacToeRouter {
    var currentScreen: Screen by mutableStateOf(Screen.Home)

    fun navigateTo(destination: Screen) {
        currentScreen = destination
    }
}