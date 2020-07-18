package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import javax.inject.Inject

class SettingsViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    fun restartSynchronizer() = synchronizer.start()

    override fun onCleared() {
        super.onCleared()
        twig("SettingsViewModel cleared!")
    }
}
