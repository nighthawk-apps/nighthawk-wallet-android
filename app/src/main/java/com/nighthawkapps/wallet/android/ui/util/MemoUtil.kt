package com.nighthawkapps.wallet.android.ui.util

import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
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
    INCLUDE_MEMO_PREFIX_STANDARD, // standard
    "reply-to", // standard w/o colon
    "reply to:", // space instead of dash
    "reply to", // space instead of dash w/o colon
    "sent from:", // previous standard
    "sent from" // previous standard w/o colon
)

// TODO: move this to the SDK
inline fun ByteArray?.toUtf8Memo(): String {
// TODO: make this more official but for now, this will do
    return if (this == null || this.isEmpty() || this[0] >= 0xF5) "" else try {
        // trim empty and "replacement characters" for codes that can't be represented in unicode
        String(this, StandardCharsets.UTF_8).trim('\u0000', '\uFFFD')
    } catch (t: Throwable) {
        "Unable to parse memo."
    }
}

object MemoUtil {

    suspend fun findAddressInMemo(tx: ConfirmedTransaction?, addressValidator: suspend (String) -> Boolean): String? {
        // note: t-addr min length is 35, plus we're expecting prefixes
        return tx?.memo?.toUtf8Memo()?.takeUnless { it.length < 35 }?.let { memo ->
            // start with what we accept as prefixes
            INCLUDE_MEMO_PREFIXES_RECOGNIZED.mapNotNull {
                val maybeMemo = memo.substringAfterLast(it)
                if (addressValidator(maybeMemo)) maybeMemo else null
            }.firstOrNull { !it.isNullOrBlank() }
        }
    }

    // note: cannot use substringAfterLast, directly because we want to ignore case. perhaps submit a feature request to kotlin for adding `ignoreCase`
    private fun String.substringAfterLast(prefix: String): String {
        return lastIndexOf(prefix, ignoreCase = true).takeUnless { it == -1 }?.let { i ->
            substring(i + prefix.length).trimStart()
        } ?: ""
    }
}
