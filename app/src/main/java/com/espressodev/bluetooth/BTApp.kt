package com.espressodev.bluetooth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.espressodev.bluetooth.ui.theme.BluetoothTheme

@Composable
fun BTApp() {
    BluetoothTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        }
    }
}
