package com.nighthawkapps.wallet.android.ui

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class MainViewModel @Inject constructor() : ViewModel() {

    private val _loadingMessage = MutableStateFlow<String?>("\u23F3 Loading...")

    val loadingMessage: StateFlow<String?> get() = _loadingMessage
    val isLoading get() = loadingMessage.value != null

    fun setLoading(isLoading: Boolean = false, message: String? = null) {
        twig("MainViewModel.setLoading: $isLoading")
        _loadingMessage.value = if (!isLoading) {
            null
        } else {
            message ?: "\u23F3 Loading..."
        }
    }
}
