package com.espressodev.bluetooth.data.impl

import android.bluetooth.BluetoothSocket
import com.espressodev.bluetooth.data.BTDataTransferService
import com.espressodev.bluetooth.data.model.BTMessage
import com.espressodev.bluetooth.data.model.toBTMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class BTDataTransferServiceImpl(private val socket: BluetoothSocket) : BTDataTransferService {
    override fun listenIncomingMessages(): Flow<BTMessage> = flow {
        if (!socket.isConnected) return@flow

        val buffer = ByteArray(1024)
        while (true) {
            val byteCount = try {
                socket.inputStream.read(buffer)
            } catch (e: Exception) {
                throw TransferFailedException()
            }

            emit(buffer.decodeToString(endIndex = byteCount).toBTMessage(isFromLocalUser = false))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun sendMessage(bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socket.outputStream.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext false
            }
            true
        }
    }
}

class TransferFailedException : IOException("Reading incoming data failed")