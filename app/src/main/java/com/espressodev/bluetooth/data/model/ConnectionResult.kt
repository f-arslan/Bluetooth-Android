package com.espressodev.bluetooth.data.model

sealed interface ConnectionResult {
    object ConnectionEstablished: ConnectionResult
    data class TransferSucceeded(val message: BTMessage): ConnectionResult
    data class Error(val message: String): ConnectionResult
}