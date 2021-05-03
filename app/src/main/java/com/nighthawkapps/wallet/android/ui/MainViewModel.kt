package com.nighthawkapps.wallet.android.ui

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class MainViewModel @Inject constructor() : ViewModel() {

    private val _loadingMessage = MutableStateFlow<String?>("\u23F3 Loading...")
    private val _syncReady = MutableStateFlow(false)

    val loadingMessage: StateFlow<String?> get() = _loadingMessage
    val isLoading get() = loadingMessage.value != null

    /**
     * A flow of booleans representing whether or not the synchronizer has been started. This is
     * useful for views that want to monitor the status of the wallet but don't want to access the
     * synchronizer before it is ready to be used. This is also helpful for race conditions where
     * the status of the synchronizer is needed before it is created.
     */
    val syncReady = _syncReady.asStateFlow()

    fun setLoading(isLoading: Boolean = false, message: String? = null) {
        twig("MainViewModel.setLoading: $isLoading")
        _loadingMessage.value = if (!isLoading) {
            null
        } else {
            message ?: "\u23F3 Loading..."
        }
    }

    fun setSyncReady(isReady: Boolean) {
        twig("MainViewModel.setSyncReady: $isReady")
        _syncReady.value = isReady
    }
}
