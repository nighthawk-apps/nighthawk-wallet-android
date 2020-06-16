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


/*
        if self.0[0] < 0xF5 {
            // Check if it is valid UTF8
            Some(str::from_utf8(&self.0).map(|memo| {
                // Drop trailing zeroes
                memo.trim_end_matches(char::from(0)).to_owned()
            }))
        } else {
            None
        }
 */