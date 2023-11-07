package com.espressodev.bluetooth.screen

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.MacAddress
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.espressodev.bluetooth.screen.ui.theme.BluetoothTheme
import java.util.UUID
import java.util.concurrent.Executor
import java.util.regex.Pattern

class CompanionDevicePairing : ComponentActivity() {
    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }
    val executor: Executor = Executor { it.run() }
    val pairedDevices = mutableStateListOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // To skip filters based on names and supported feature flags (UUIDs),
        // omit calls to setNamePattern() and addServiceUuid()
        // respectively, as shown in the following  Bluetooth example.
        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
            .setNamePattern(Pattern.compile("My device"))
            .addServiceUuid(ParcelUuid(UUID(0x123abcL, -1L)), null)
            .build()

        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of them appears.
        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(true)
            .build()

        setContent {
            BluetoothTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        val context = LocalContext.current
                        Button(onClick = {
                            // When the app tries to pair with a Bluetooth device, show the
                            // corresponding dialog box to the user.
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                deviceManager.associate(pairingRequest,
                                    executor,
                                    object : CompanionDeviceManager.Callback() {
                                        // Called when a device is found. Launch the IntentSender so the user
                                        // can select the device they want to pair with.
                                        override fun onAssociationPending(intentSender: IntentSender) {
                                            intentSender.let {
                                               startIntentSender(
                                                    it,
                                                    null,
                                                    0,
                                                    0,
                                                    0
                                               )
                                            }
                                        }

                                        override fun onAssociationCreated(associationInfo: AssociationInfo) {
                                            // AssociationInfo object is created and get association id and the
                                            // macAddress.
                                            val associationId: Int = associationInfo.id
                                            val macAddress: MacAddress? =
                                                associationInfo.deviceMacAddress

                                            Log.d(
                                                "CompanionDevicePairingHello",
                                                "associationId: $associationId"
                                            )
                                            Log.d(
                                                "CompanionDevicePairingHello",
                                                "macAddress: $macAddress"
                                            )
                                        }

                                        override fun onFailure(errorMessage: CharSequence?) {
                                            // Handle the failure.
                                            Log.d(
                                                "CompanionDevicePairingHello 3",
                                                "onFailure: $errorMessage"
                                            )
                                        }
                                    })
                            } else {
                                // When the app tries to pair with a Bluetooth device, show the
                                // corresponding dialog box to the user.
                                deviceManager.associate(
                                    pairingRequest,
                                    object : CompanionDeviceManager.Callback() {

                                        override fun onDeviceFound(chooserLauncher: IntentSender) {
                                            startIntentSenderForResult(
                                                chooserLauncher,
                                                SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                                            )
                                        }

                                        override fun onFailure(error: CharSequence?) {
                                            // Handle the failure.
                                            Log.d(
                                                "CompanionDevicePairingHello 4",
                                                "onFailure: $error"
                                            )
                                        }
                                    }, null
                                )
                            }
                        }) {
                            Text("Connect to a device")
                        }
                        LazyColumn {
                            items(pairedDevices) { device ->
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    Log.d("CompanionDevicePairing", "No permission")
                                }
                                Text(text = device.name)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SELECT_DEVICE_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.let { device ->
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                requestPermissions(
                                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                    1
                                )
                            }
                            return
                        }
                        device.createBond()
                        pairedDevices.add(device)
                    }
                }
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val SELECT_DEVICE_REQUEST_CODE = 0
    }
}
