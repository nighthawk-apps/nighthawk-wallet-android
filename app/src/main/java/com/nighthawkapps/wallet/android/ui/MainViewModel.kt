package com.nighthawkapps.wallet.android.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class MainViewModel @Inject constructor() : ViewModel() {

    private val _loadingMessage = MutableStateFlow<String?>("\u23F3 Loading...")
    private val _syncReady = MutableStateFlow(false)
    private val _intentData = MutableStateFlow<Uri?>(null)
    private val _sendZecDeepLinkData = MutableStateFlow<DeepLinkUtil.SendDeepLinkData?>(null)
    private val _isAppStarting = MutableStateFlow(true)
    private val _startingDestination = MutableStateFlow<Int?>(null)

    val loadingMessage: StateFlow<String?> get() = _loadingMessage
    val isLoading get() = loadingMessage.value != null
    val intentData get() = _intentData
    val sendZecDeepLinkData get() = _sendZecDeepLinkData
    val isAppStarting: StateFlow<Boolean> get() = _isAppStarting
    val startDestination: StateFlow<Int?> get() = _startingDestination

    fun setStartingDestination(destination: Int) {
        _startingDestination.value = destination
        _isAppStarting.value = false
    }

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

    fun setIntentData(uri: Uri?) {
        _intentData.value = uri
    }

    fun setSendZecDeepLinkData(data: DeepLinkUtil.SendDeepLinkData?) {
        _sendZecDeepLinkData.value = data
    }

    fun isMainScreen(destinationID: Int): Boolean {
        return destinationID == R.id.nav_home || destinationID == R.id.nav_transfer || destinationID == R.id.nav_settings
    }
}
