package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.di.module.InitializerModule
import com.nighthawkapps.wallet.android.ext.SERVER_HOST
import com.nighthawkapps.wallet.android.ext.SERVER_PORT
import javax.inject.Inject

class SettingsViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    fun updateServer(host: String, port: Int) {
        com.nighthawkapps.wallet.android.ext.putString(SERVER_HOST, host)
        com.nighthawkapps.wallet.android.ext.putInt(SERVER_PORT, port)
        synchronizer.stop()
    }

    fun getServerHost(): String {
        return com.nighthawkapps.wallet.android.ext.getString(
            SERVER_HOST,
            InitializerModule.defaultHost
        )
    }

    fun getServerPort(): Int {
        return com.nighthawkapps.wallet.android.ext.getInt(
            SERVER_PORT,
            InitializerModule.defaultPort
        )
    }

    override fun onCleared() {
        super.onCleared()
        twig("SettingsViewModel cleared!")
    }
}
