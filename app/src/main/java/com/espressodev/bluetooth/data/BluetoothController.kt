package com.espressodev.bluetooth.data

import com.espressodev.bluetooth.data.model.BLEDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BLEDevice>>
    val pairedDevices: StateFlow<List<BLEDevice>>
    val errors: SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun startBLEServer(): Flow<>

    fun release()
}