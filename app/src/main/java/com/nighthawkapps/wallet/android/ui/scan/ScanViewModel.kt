package com.nighthawkapps.wallet.android.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil
import javax.inject.Inject

class ScanViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    val networkName get() = synchronizer.network.networkName

    suspend fun parse(qrCode: String): DeepLinkUtil.SendDeepLinkData? {
        // Temporary parse code to allow both plain addresses and those that start with Zcash
        // TODO: Replace with more robust ZIP-321 handling of QR codes
        val address = if (qrCode.startsWith("zcash:")) {
            qrCode.substring(6, qrCode.indexOf("?").takeUnless { it == -1 } ?: qrCode.length)
        } else {
            qrCode
        }
        val data = DeepLinkUtil.getSendDeepLinkData(Uri.parse(qrCode))
        return if (synchronizer.validateAddress(data?.address ?: address).isNotValid) null else data ?: DeepLinkUtil.SendDeepLinkData(address, 0, null)
    }

    override fun onCleared() {
        super.onCleared()
        twig("${javaClass.simpleName} cleared!")
    }
}
