package com.nighthawkapps.wallet.android.ext

import android.text.Spannable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.toSpannable

fun CharSequence.toColoredSpan(colorResId: Int, coloredPortion: String): Spannable {
    return toSpannable().apply {
        val start = this@toColoredSpan.indexOf(coloredPortion)
        setSpan(
            ForegroundColorSpan(colorResId.toAppColor()),
            start,
            start + coloredPortion.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}