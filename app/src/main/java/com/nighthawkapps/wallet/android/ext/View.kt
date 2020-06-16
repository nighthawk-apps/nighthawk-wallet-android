package com.nighthawkapps.wallet.android.ext

import android.view.View
import android.view.View.*
import com.nighthawkapps.wallet.android.ui.MainActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow

fun View.gone() = goneIf(true)

fun View.invisible() = invisibleIf(true)

fun View.goneIf(isGone: Boolean) {
    visibility = if (isGone) GONE else VISIBLE
}

fun View.invisibleIf(isInvisible: Boolean) {
    visibility = if (isInvisible) INVISIBLE else VISIBLE
}

fun View.disabledIf(isDisabled: Boolean) {
    isEnabled = !isDisabled
}

fun View.transparentIf(isTransparent: Boolean) {
    alpha = if (isTransparent) 0.0f else 1.0f
}

fun View.onClickNavTo(navResId: Int, block: (() -> Any) = {}) {
    setOnClickListener {
        block()
        (context as? MainActivity)?.safeNavigate(navResId)
            ?: throw IllegalStateException("Cannot navigate from this activity. " +
                    "Expected MainActivity but found ${context.javaClass.simpleName}")
    }
}

fun View.onClickNavUp(block: (() -> Any) = {}) {
    setOnClickListener {
        block()
        (context as? MainActivity)?.navController?.navigateUp()
            ?: throw IllegalStateException(
                "Cannot navigate from this activity. " +
                        "Expected MainActivity but found ${context.javaClass.simpleName}"
            )
    }
}

fun View.onClickNavBack(block: (() -> Any) = {}) {
    setOnClickListener {
        block()
        (context as? MainActivity)?.navController?.popBackStack()
            ?: throw IllegalStateException(
                "Cannot navigate from this activity. " +
                        "Expected MainActivity but found ${context.javaClass.simpleName}"
            )
    }
}

fun View.clicks() = channelFlow<View> {
    setOnClickListener {
        offer(this@clicks)
    }
    awaitClose {
        setOnClickListener(null)
    }
}