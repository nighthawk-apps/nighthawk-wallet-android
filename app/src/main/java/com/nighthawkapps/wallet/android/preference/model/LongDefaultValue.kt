package com.nighthawkapps.wallet.android.preference.model

import android.content.Context
import android.content.SharedPreferences
import com.nighthawkapps.wallet.android.preference.SharedPreferenceFactory

/**
 * A default value represents a preference key, along with its default value.  It does not, by itself,
 * know how to read or write values from the preference repository.
 */
data class LongDefaultValue(
    override val key: String,
    internal val defaultValue: Long
) : DefaultValue<Long> {
    init {
        require(key.isNotEmpty())
    }
}

fun LongDefaultValue.get(context: Context) = get(
    SharedPreferenceFactory.getSharedPreferences(context)
)

internal fun LongDefaultValue.get(sharedPreferences: SharedPreferences) =
    sharedPreferences.getLong(key, defaultValue)

fun LongDefaultValue.put(context: Context, newValue: Long) = put(
    SharedPreferenceFactory.getSharedPreferences(context),
    newValue
)

internal fun LongDefaultValue.put(sharedPreferences: SharedPreferences, newValue: Long) =
    sharedPreferences.edit().putLong(key, newValue).apply()
