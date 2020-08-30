package com.nighthawkapps.wallet.android.ext

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.nighthawkapps.wallet.android.NighthawkWalletApp

const val SERVER_HOST: String = "SERVER_HOST"
const val SERVER_PORT: String = "SERVER_PORT"

var masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

var sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
    "shared_prefs",
    masterKeyAlias,
    NighthawkWalletApp.instance,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

fun putString(key: String, value: String) {
    sharedPreferences.run { edit().putString(key, value).commit() }
}

fun putInt(key: String, value: Int) {
    sharedPreferences.run { edit().putInt(key, value).commit() }
}

fun getString(key: String, value: String): String {
    return sharedPreferences.getString(key, value) ?: value
}

fun getInt(key: String, value: Int): Int {
    return sharedPreferences.getInt(key, value)
}