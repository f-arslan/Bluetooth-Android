package com.espressodev.bluetooth

import android.content.res.Resources
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.espressodev.bluetooth.common.Constants.MEDIUM_PADDING
import com.espressodev.bluetooth.common.snackbar.SnackbarManager
import com.espressodev.bluetooth.navigation.TicTacToeNavHost
import kotlinx.coroutines.CoroutineScope


@Composable
fun TicTacToeApp() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val appState = rememberAppState()
        Scaffold(snackbarHost = {
            SnackbarHost(
                hostState = appState.snackbarHostState,
                modifier = Modifier.padding(MEDIUM_PADDING),
                snackbar = { snackbarData -> Snackbar(snackbarData = snackbarData) }
            )
        }) {
            TicTacToeNavHost(appState = appState, modifier = Modifier.padding(it))
        }
    }
}


@Composable
@ReadOnlyComposable
fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}

@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarManager: SnackbarManager = SnackbarManager,
    resources: Resources = resources(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(navController, snackbarHostState, coroutineScope) {
    TicTacToeAppState(
        navController,
        snackbarHostState,
        snackbarManager,
        resources,
        coroutineScope
    )
}