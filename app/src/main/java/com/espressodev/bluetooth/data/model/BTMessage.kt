package com.espressodev.bluetooth.data.model

data class BTMessage(
    val message: String,
    val senderName: String,
    val isFromLocalUser: Boolean
)

fun String.toBTMessage(isFromLocalUser: Boolean): BTMessage {
    val name = substringBeforeLast("#")
    val message = substringAfter("#")
    return BTMessage(
        message = message,
        senderName = name,
        isFromLocalUser = isFromLocalUser
    )
}

fun BTMessage.toByteArray(): ByteArray {
    return "$senderName#$message".encodeToByteArray()
}