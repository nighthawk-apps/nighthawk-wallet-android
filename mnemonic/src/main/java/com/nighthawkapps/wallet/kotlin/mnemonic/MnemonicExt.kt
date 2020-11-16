package com.nighthawkapps.wallet.kotlin.mnemonic

import java.util.*

/**
 * Clears out the given char array in memory, for security purposes.
 */
fun CharArray.clear() {
    Arrays.fill(this, '0')
}

/**
 * Clears out the given byte array in memory, for security purposes.
 */
fun ByteArray.clear() {
    Arrays.fill(this, 0.toByte())
}