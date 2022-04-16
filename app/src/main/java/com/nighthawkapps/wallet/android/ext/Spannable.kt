package com.nighthawkapps.wallet.android.ext

import android.graphics.Color
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.text.toSpannable

fun CharSequence.toColoredSpan(@ColorRes colorResId: Int, coloredPortion: String): CharSequence {
    return toSpannable().apply {
        val start = this@toColoredSpan.indexOf(coloredPortion)
        if (start == -1) return@apply
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

fun TextView.toClickableSpan(coloredPortion: String, onClick: (view: View) -> Unit = {}) {
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(p0: View) {
            onClick(p0)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }
    val spannable = this.text.toSpannable().apply {
        val startIndex = this@toClickableSpan.text.indexOf(coloredPortion)
        if (startIndex == -1) return
        setSpan(clickableSpan, startIndex, startIndex + coloredPortion.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    this.apply {
        text = spannable
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT
    }
}
