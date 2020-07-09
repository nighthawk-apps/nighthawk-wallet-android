package com.nighthawkapps.wallet.android.ui.util

import java.nio.charset.StandardCharsets

const val INCLUDE_MEMO_PREFIX = "sent from"

inline fun ByteArray?.toUtf8Memo(): String {
// TODO: make this more official but for now, this will do
    return if (this == null || this[0] >= 0xF5) "" else try {
        String(this, StandardCharsets.UTF_8).trim('\u0000')
    } catch (t: Throwable) {
        "unable to parse memo"
    }
}
