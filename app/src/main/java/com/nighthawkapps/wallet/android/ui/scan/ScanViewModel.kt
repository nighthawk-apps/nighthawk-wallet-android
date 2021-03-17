package com.nighthawkapps.wallet.android.ui.scan

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import javax.inject.Inject

class ScanViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    suspend fun parse(qrCode: String): String? {
        // Temporary parse code to allow both plain addresses and those that start with Zcash
        // TODO: Replace with more robust ZIP-321 handling of QR codes
        val address = if (qrCode.startsWith("zcash:")) {
            qrCode.substring(6, qrCode.indexOf("?").takeUnless { it == -1 } ?: qrCode.length)
        } else {
            qrCode
        }
        return if (synchronizer.validateAddress(address).isNotValid) null else address
    }

    override fun onCleared() {
        super.onCleared()
        twig("${javaClass.simpleName} cleared!")
    }
}
