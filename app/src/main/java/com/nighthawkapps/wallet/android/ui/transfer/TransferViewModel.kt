package com.nighthawkapps.wallet.android.ui.transfer

import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.ext.Const

class TransferViewModel : ViewModel() {

    /**
     * Configure the MoonPay url and return the final URL
     */
    fun getMoonPayUrl(): String {
        return "${Const.Default.Server.BUY_ZEC_BASE_URL}&currencyCode=zec"
    }
}
