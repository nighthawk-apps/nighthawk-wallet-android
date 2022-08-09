package com.nighthawkapps.wallet.android.ext

import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Distribute a string into evenly-sized chunks and then execute a function with each chunk.
 *
 * @param chunks the number of chunks to create
 * @param block a function to be applied to each zero-indexed chunk.
 */
fun <T> String.distribute(chunks: Int, block: (Int, String) -> T) {
    val charsPerChunk = length / chunks.toFloat()
    val wholeCharsPerChunk = charsPerChunk.toInt()
    val chunksWithExtra = ((charsPerChunk - wholeCharsPerChunk) * chunks).roundToInt()
    repeat(chunks) { i ->
        val part = if (i < chunksWithExtra) {
            substring(i * (wholeCharsPerChunk + 1), (i + 1) * (wholeCharsPerChunk + 1))
        } else {
            substring(i * wholeCharsPerChunk + chunksWithExtra, (i + 1) * wholeCharsPerChunk + chunksWithExtra)
        }
        block(i, part)
    }
}

fun Boolean.asString(ifTrue: String = "", ifFalse: String = "") = if (this) ifTrue else ifFalse

inline val WalletBalance.pending: Zatoshi
    get() = (this.total - this.available).coerceAtLeast(Zatoshi(0))

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
