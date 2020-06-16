package com.nighthawkapps.wallet.android.qrecycler

/**
 * An interface to allow for plugging in any scanner
 */
interface QScanner {
    fun scanBarcode(callback: (Result<String>) -> Unit)
}