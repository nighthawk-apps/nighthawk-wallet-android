package com.nighthawkapps.wallet.android.ui.util

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.core.content.ContextCompat
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R

/**
 * A span used for numbering the parts of an address. It combines a [android.text.style.RelativeSizeSpan],
 * [android.text.style.SuperscriptSpan], and a [android.text.style.ForegroundColorSpan] into one class for efficiency.
 */
class AddressPartNumberSpan(
    val proportion: Float = 0.5f,
    val color: Int = ContextCompat.getColor(NighthawkWalletApp.instance, R.color.colorPrimary)
) : MetricAffectingSpan() {

    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.baselineShift += (textPaint.ascent() / 2).toInt() // from SuperscriptSpan
        textPaint.textSize = textPaint.textSize * proportion // from RelativeSizeSpan
    }

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.baselineShift += (textPaint.ascent() / 2).toInt() // from SuperscriptSpan (baseline must shift before resizing or else it will not properly align to the top of the text)
        textPaint.textSize = textPaint.textSize * proportion // from RelativeSizeSpan
        textPaint.color = color // from ForegroundColorSpan
    }
}