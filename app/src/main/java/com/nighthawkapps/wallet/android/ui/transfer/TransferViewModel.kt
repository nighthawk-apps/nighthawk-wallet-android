package com.nighthawkapps.wallet.android.ui.transfer

import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.ext.Const
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TransferViewModel : ViewModel() {

    private val _currentUIScreen = MutableStateFlow(UIScreen.LANDING)
    val currentUIScreen: StateFlow<UIScreen> get() = _currentUIScreen
    var lastShownScreen: UIScreen = _currentUIScreen.value

    fun updateUIScreen(uiScreen: UIScreen) {
        lastShownScreen = _currentUIScreen.value
        _currentUIScreen.value = uiScreen
    }

    /**
     * Configure the MoonPay url and return the final URL
     */
    fun getMoonPayUrl(): String {
        return "${Const.Default.Server.BUY_ZEC_BASE_URL}&currencyCode=zec"
    }

    enum class UIScreen {
        LANDING,
        RECEIVE,
        TOP_UP
    }
}
