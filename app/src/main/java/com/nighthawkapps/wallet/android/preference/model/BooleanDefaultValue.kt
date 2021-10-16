package com.nighthawkapps.wallet.android.preference.model

import android.content.Context
import android.content.SharedPreferences
import com.nighthawkapps.wallet.android.preference.SharedPreferenceFactory

/**
 * A default value represents a preference key, along with its default value.  It does not, by itself,
 * know how to read or write values from the preference repository.
 */
data class BooleanDefaultValue(override val key: String, internal val defaultValue: Boolean) :
    DefaultValue<Boolean> {
    init {
        require(key.isNotEmpty())
    }
}

fun BooleanDefaultValue.get(context: Context) = get(
    SharedPreferenceFactory.getSharedPreferences(
        context
    )
)

internal fun BooleanDefaultValue.get(sharedPreferences: SharedPreferences) =
    sharedPreferences.getBoolean(key, defaultValue)

fun BooleanDefaultValue.put(context: Context, newValue: Boolean) = put(
    SharedPreferenceFactory.getSharedPreferences(
        context
    ),
    newValue
)

internal fun BooleanDefaultValue.put(sharedPreferences: SharedPreferences, newValue: Boolean) =
    sharedPreferences.edit().putBoolean(key, newValue).apply()
