package com.espressodev.bluetooth.common.ext

import com.google.android.gms.nearby.connection.Payload

fun Payload.toPosition(): Pair<Int, Int> {
    val positionStr = String(asBytes()!!, Charsets.UTF_8)
    val positionArray = positionStr.split(",")
    return positionArray[0].toInt() to positionArray[1].toInt()
}

