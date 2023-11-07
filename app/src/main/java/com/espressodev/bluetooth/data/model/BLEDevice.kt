package com.espressodev.bluetooth.data.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

data class BLEDevice(
    val name: String?,
    val address: String  // Mac address
)

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBLEDevice(): BLEDevice {
    return BLEDevice(
        name = name,
        address = address
    )
}