package com.nighthawkapps.wallet.android.preference

import android.content.Context

object SharedPreferenceFactory {

    private const val DEFAULT_SHARED_PREFERENCES = "cash.z.ecc.default"

    fun getSharedPreferences(context: Context) = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES, Context.MODE_PRIVATE)
}
