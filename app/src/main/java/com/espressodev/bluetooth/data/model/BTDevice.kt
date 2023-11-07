package com.espressodev.bluetooth.data.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

data class BTDevice(
    val name: String?,
    val address: String  // Mac address
)

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBTDevice(): BTDevice {
    return BTDevice(
        name = name,
        address = address
    )
}