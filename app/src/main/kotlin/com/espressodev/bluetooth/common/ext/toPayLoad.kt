package com.espressodev.bluetooth.common.ext

import com.google.android.gms.nearby.connection.Payload

fun Pair<Int, Int>.toPayLoad() =
            Payload.fromBytes("$first,$second".toByteArray(Charsets.UTF_8))