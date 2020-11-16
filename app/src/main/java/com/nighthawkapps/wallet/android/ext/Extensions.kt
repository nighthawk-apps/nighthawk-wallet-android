package com.nighthawkapps.wallet.android.ext

import cash.z.ecc.android.sdk.ext.twig

fun Boolean.asString(ifTrue: String = "", ifFalse: String = "") = if (this) ifTrue else ifFalse

inline fun <R> tryWithWarning(message: String = "", block: () -> R): R? {
    return try {
        block()
    } catch (error: Throwable) {
        twig("WARNING: $message")
        null
    }
}
