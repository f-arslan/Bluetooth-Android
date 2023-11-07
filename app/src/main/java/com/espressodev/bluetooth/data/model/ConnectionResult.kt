package com.espressodev.bluetooth.data.model

sealed interface ConnectionResult {
    object ConnectionEstablished: ConnectionResult
    data class TransferSucceeded(val message: BTDevice): ConnectionResult
    data class Error(val message: String): ConnectionResult
}