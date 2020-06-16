package com.nighthawkapps.wallet.android.ext

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun <T: View> LifecycleOwner.onClick(view: T, throttle: Long = 250L, block: (T) -> Unit) {
    view.clicks().debounce(throttle).onEach {
        block(view)
    }.launchIn(this.lifecycleScope)
}