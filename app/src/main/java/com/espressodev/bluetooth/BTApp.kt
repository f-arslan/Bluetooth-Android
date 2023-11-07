package com.espressodev.bluetooth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.espressodev.bluetooth.data.model.BTDevice
import com.espressodev.bluetooth.ui.theme.BluetoothTheme

@Composable
fun BTApp(viewModel: BTViewModel = hiltViewModel()) {
    BluetoothTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val context = LocalContext.current
            Scaffold {
                Column(modifier = Modifier.padding(it)) {
                    LaunchedEffect(key1 = state.errorMessage) {
                        state.errorMessage?.let {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    }

                    LaunchedEffect(key1 = state.isConnected) {
                        if (state.isConnected) {
                            Toast.makeText(context, "You're connected!", Toast.LENGTH_LONG).show()
                        }
                    }

                    when {
                        state.isConnecting -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Text(text = "Connecting...")
                            }
                        }

                        state.isConnected -> {

                        }

                        else -> {
                            DeviceSection(
                                state = state,
                                onStartScanClick = viewModel::startScan,
                                onStopScanClick = viewModel::stopScan,
                                onStartServerClick = viewModel::waitForIncomingConnections,
                                onDeviceClick = viewModel::connectToDevice
                            )
                        }
                    }

                }
            }
        }
    }
}


@Composable
fun DeviceSection(
    state: BTUiState,
    onStartScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onStartServerClick: () -> Unit,
    onDeviceClick: (BTDevice) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        BTDeviceList(
            pairedDevices = state.pairedDevices,
            scannedDevices = state.scannedDevices,
            onClick = onDeviceClick,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(onClick = onStartScanClick) {
                    Text(text = "Start the scan")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onStopScanClick) {
                    Text(text = "Stop the scan")
                }
            }
            Button(onClick = onStartServerClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Start the server for connection")
            }
        }
    }
}

@Composable
fun BTDeviceList(
    pairedDevices: List<BTDevice>,
    scannedDevices: List<BTDevice>,
    onClick: (BTDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = "Paired Devices",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(pairedDevices) { device ->
            Text(
                text = device.name ?: "Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onClick(device) }
            )
        }

        item {
            Text(
                text = "Scanned Devices",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(scannedDevices) { device ->
            Text(
                text = device.name ?: "Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onClick(device) }
            )
        }
    }
}


