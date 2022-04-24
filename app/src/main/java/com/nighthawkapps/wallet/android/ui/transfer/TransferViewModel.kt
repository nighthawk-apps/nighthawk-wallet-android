package com.nighthawkapps.wallet.android.ui.transfer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TransferViewModel : ViewModel() {

    private val _currentUIScreen = MutableStateFlow(UIScreen.LANDING)
    val currentUIScreen: StateFlow<UIScreen> get() = _currentUIScreen

    fun updateUIScreen(uiScreen: UIScreen) {
        _currentUIScreen.value = uiScreen
    }

    enum class UIScreen {
        LANDING,
        RECEIVE,
        TOP_UP
    }
}
