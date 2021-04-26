package com.nighthawkapps.wallet.android.ext

import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import cash.z.ecc.android.sdk.ext.twig
import java.util.Locale

fun Boolean.asString(ifTrue: String = "", ifFalse: String = "") = if (this) ifTrue else ifFalse

inline fun <R> tryWithWarning(message: String = "", block: () -> R): R? {
    return try {
        block()
    } catch (error: Throwable) {
        twig("WARNING: $message")
        null
    }
}

inline fun <E : Throwable, R> failWith(specificErrorType: E, block: () -> R): R {
    return try {
        block()
    } catch (error: Throwable) {
        throw specificErrorType
    }
}

inline fun Fragment.locale(): Locale = context?.locale() ?: Locale.getDefault()

inline fun Context.locale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        //noinspection deprecation
        resources.configuration.locale
    }
}

fun ByteArray.toTxId(): String {
    val sb = StringBuilder(size * 2)
    for (i in (size - 1) downTo 0) {
        sb.append(String.format("%02x", this[i]))
    }
    return sb.toString()
}
