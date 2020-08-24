package com.nighthawkapps.wallet.android.ui.util

import java.nio.charset.StandardCharsets

/**
 * The prefix that this wallet uses whenever the user chooses to include their address in the memo.
 * This is the one we standardize around.
 */
const val INCLUDE_MEMO_PREFIX_STANDARD = "Reply-To:"

/**
 * The non-standard prefixes that we will parse if other wallets send them our way.
 */
val INCLUDE_MEMO_PREFIXES_RECOGNIZED = arrayOf(
    INCLUDE_MEMO_PREFIX_STANDARD,
    "reply-to",
    "reply to:",
    "reply to",
    "sent from:",
    "sent from"
)

inline fun ByteArray?.toUtf8Memo(): String {
    return if (this == null || this[0] >= 0xF5) "" else try {
        String(this, StandardCharsets.UTF_8).trim('\u0000')
    } catch (t: Throwable) {
        "unable to parse memo"
    }
}