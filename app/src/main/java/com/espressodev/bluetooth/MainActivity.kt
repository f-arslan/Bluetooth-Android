package com.espressodev.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.espressodev.bluetooth.navigation.Screen
import com.espressodev.bluetooth.navigation.TicTacToeRouter
import com.espressodev.bluetooth.screens.DiscoveringScreen
import com.espressodev.bluetooth.screens.GameScreen
import com.espressodev.bluetooth.screens.HomeScreen
import com.espressodev.bluetooth.screens.HostingScreen
import com.espressodev.bluetooth.ui.theme.BluetoothTheme
import com.espressodev.bluetooth.viewmodel.TicTacToeViewModel
import com.espressodev.bluetooth.viewmodel.TicTacToeViewModelFactory
import com.google.android.gms.nearby.Nearby

class MainActivity : ComponentActivity() {


    private val viewModel: TicTacToeViewModel by viewModels {
        TicTacToeViewModelFactory(Nearby.getConnectionsClient(applicationContext))
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.any { !it.value }) {
            Toast.makeText(this, "Required permissions needed", Toast.LENGTH_LONG).show()
            finish()
        } else {
            recreate()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestMultiplePermissions.launch(
                REQUIRED_PERMISSIONS
            )
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.isEmpty() || permissions.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothTheme {
                MainActivityScreen()
            }
        }
    }

    @Composable
    private fun MainActivityScreen() {
        Surface {
            when (TicTacToeRouter.currentScreen) {
                is Screen.Home -> HomeScreen(viewModel)
                is Screen.Hosting -> HostingScreen(viewModel)
                is Screen.Discovering -> DiscoveringScreen(viewModel)
                is Screen.Game -> GameScreen(viewModel)
            }
        }
    }

    private companion object {
        val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
    }
}
