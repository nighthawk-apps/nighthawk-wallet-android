package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.SERVER_HOST
import com.nighthawkapps.wallet.android.ext.SERVER_PORT
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    fun updateServer(host: String, port: Int) {
        viewModelScope.launch {
            synchronizer.changeServer(host, port) { error ->
                if (error == null) {
                    com.nighthawkapps.wallet.android.ext.putString(SERVER_HOST, host)
                    com.nighthawkapps.wallet.android.ext.putInt(SERVER_PORT, port)
                }
            }
        }
    }

    fun getServerHost(): String {
        return com.nighthawkapps.wallet.android.ext.getString(
            SERVER_HOST,
            Const.Default.Server.HOST
        )
    }

    fun getServerPort(): Int {
        return com.nighthawkapps.wallet.android.ext.getInt(
            SERVER_PORT,
            Const.Default.Server.PORT
        )
    }

    override fun onCleared() {
        super.onCleared()
        twig("SettingsViewModel cleared!")
    }
}
