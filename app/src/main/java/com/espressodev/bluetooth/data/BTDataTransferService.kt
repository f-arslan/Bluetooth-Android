package com.espressodev.bluetooth.data

import com.espressodev.bluetooth.data.model.BTMessage
import kotlinx.coroutines.flow.Flow

interface BTDataTransferService {
    fun listenIncomingMessages(): Flow<BTMessage>
    suspend fun sendMessage(bytes: ByteArray): Boolean
}