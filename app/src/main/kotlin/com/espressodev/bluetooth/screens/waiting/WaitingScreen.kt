package com.espressodev.bluetooth.screens.waiting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HostingScreen(navigateToHome: () -> Unit) {
    BackHandler(onBack = navigateToHome)
    WaitingScreen(title = "Hosting...", onStopClick = navigateToHome)
}

@Composable
fun DiscoveringScreen(navigateToHome: () -> Unit) {
    BackHandler(onBack = navigateToHome)
    WaitingScreen(title = "Discovering...", onStopClick = navigateToHome)
}

@Composable
fun WaitingScreen(title: String, onStopClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title)
        CircularProgressIndicator(
            modifier = Modifier
                .padding(16.dp)
                .size(80.dp)
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStopClick
        ) {
            Text(text = "Stop")
        }
    }
}

@Preview
@Composable
fun WaitingScreenPreview() {
    WaitingScreen(title = "Hosting...", onStopClick = {})
}