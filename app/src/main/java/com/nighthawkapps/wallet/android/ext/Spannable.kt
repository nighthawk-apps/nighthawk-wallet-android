package com.nighthawkapps.wallet.android.ext

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.core.text.toSpannable

fun CharSequence.toColoredSpan(@ColorRes colorResId: Int, coloredPortion: String): CharSequence {
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

fun CharSequence.toSplitColorSpan(@ColorRes startColorResId: Int, @ColorRes endColorResId: Int, startColorLength: Int): CharSequence {
    return toSpannable().apply {
        setSpan(ForegroundColorSpan(startColorResId.toAppColor()), 0, startColorLength - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(ForegroundColorSpan(endColorResId.toAppColor()), startColorLength, this@toSplitColorSpan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
