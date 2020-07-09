package com.nighthawkapps.wallet.android.ui.scan

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import javax.inject.Inject

class ScanViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    suspend fun isNotValid(address: String) = synchronizer.validateAddress(address).isNotValid

    override fun onCleared() {
        super.onCleared()
        twig("${javaClass.simpleName} cleared!")
    }
}
