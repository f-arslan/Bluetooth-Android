package com.espressodev.bluetooth.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.espressodev.bluetooth.common.component.TicTacToeDialog
import com.espressodev.bluetooth.event_bus.DialogState
import com.espressodev.bluetooth.R.string as AppText

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState.homeViewState) {
        HomeViewState.Idle -> {
            HomeScreen(
                uiState = uiState,
                onDialogStateChanged = viewModel::onDialogStateChanged,
                onStartHostingClick = viewModel::startHosting,
                onStartDiscoveringClick = viewModel::startDiscovering
            )
        }

        HomeViewState.Loading -> HomeLoading(
            onCancelClicked = { viewModel.onHomeViewStateChanged(HomeViewState.Idle) })
        HomeViewState.ReadyForGame -> TODO()
    }
}



@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDialogStateChanged: (DialogState) -> Unit,
    onStartHostingClick: () -> Unit,
    onStartDiscoveringClick: () -> Unit
) {
    if (uiState.dialogState == DialogState.Open) {
        TicTacToeDialog(
            onDismiss = { onDialogStateChanged(DialogState.Dismiss) },
            onConfirm = { onDialogStateChanged(DialogState.Confirm) },
            title = AppText.auth_dialog_title,
            opponentEndpointId = uiState.opponentEndpointId
        )
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartHostingClick
        ) {
            Text(text = "Host")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartDiscoveringClick
        ) {
            Text(text = "Discover")
        }
    }
}

@Composable
fun HomeLoading(onCancelClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Button(onClick = onCancelClicked) {
            Text(text = stringResource(AppText.cancel))
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview(
) {
    HomeScreen(uiState = HomeUiState(
        dialogState = DialogState.Idle,
        homeViewState = HomeViewState.Idle,
        opponentEndpointId = "possim"
    ), onDialogStateChanged = {}, onStartHostingClick = {}, onStartDiscoveringClick = {})
}