package com.nighthawkapps.wallet.android.ext

import androidx.fragment.app.Fragment

/**
 * A safer alternative to [Fragment.requireContext], as it avoids leaking Fragment or Activity context
 * when Application context is often sufficient.
 */
fun Fragment.requireApplicationContext() = requireContext().applicationContext
