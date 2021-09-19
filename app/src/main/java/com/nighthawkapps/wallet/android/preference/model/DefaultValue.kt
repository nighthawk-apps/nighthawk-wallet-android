package com.nighthawkapps.wallet.android.preference.model

/**
 * A key and a default value for a key-value store of preferences.
 *
 * Use of this interface avoids duplication or accidental variation in default value, because key
 * and default are defined together just once.
 *
 * Note that T is not fully generic and should be one of the supported types: Boolean, ... (other types to be added in the future)
 *
 * @see BooleanDefaultValue
 */
/*
 * Although primitives would be nice, Objects don't increase memory usage much
 * because of the autoboxing cache on the JVM.  For example, Boolean's true/false values
 * are cached.
 */
interface DefaultValue<T> {
    // Note: the default value is not available through the public interface in order to prevent
    // clients from accidentally using the default value instead of the stored value.

    val key: String
}
