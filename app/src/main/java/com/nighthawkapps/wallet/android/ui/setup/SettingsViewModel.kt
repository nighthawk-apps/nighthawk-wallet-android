package com.nighthawkapps.wallet.android.ui.setup

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.model.BlockHeight
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.lockbox.LockBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.Delegates
import kotlin.reflect.KProperty
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SettingsViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    lateinit var uiModels: MutableStateFlow<UiModel>

    private lateinit var initialServer: UiModel

    // TODO: track this in the app and then fetch. For now, just estimate the blocks per second.
    val bps = 40

    var pendingHost by Delegates.observable("", ::onUpdateModel)
    var pendingPortText by Delegates.observable("", ::onUpdateModel)

    fun scanDistance(): Long {
        synchronizer.latestHeight?.let { latestHeight ->
            synchronizer.latestBirthdayHeight?.let { latestBirthdayHeight ->
                return (latestHeight.value - latestBirthdayHeight.value).coerceAtLeast(0)
            }
        }
        return 0
    }

    fun wipe(context: Context?) {
        viewModelScope.launch {
            synchronizer.stop()
            Toast.makeText(NighthawkWalletApp.instance, context?.getString(R.string.rescan_wallet_wipe_success), Toast.LENGTH_LONG).show()
            Initializer.erase(NighthawkWalletApp.instance, NighthawkWalletApp.instance.defaultNetwork)
        }
    }

    suspend fun fullRescan() {
        synchronizer.latestBirthdayHeight?.let {
            rewindTo(it)
        }
    }

    suspend fun quickRescan() {
        synchronizer.latestHeight?.let {
            val newHeightValue =
                (it.value - 8064L).coerceAtLeast(synchronizer.network.saplingActivationHeight.value)
            rewindTo(BlockHeight.new(synchronizer.network, newHeightValue))
        }
    }

    private suspend fun rewindTo(targetHeight: BlockHeight) {
        twig("TMP: rewinding to targetHeight $targetHeight")
        synchronizer.rewindToNearestHeight(targetHeight, true)
    }

    fun fullScanDistance(): Long {
        synchronizer.latestHeight?.let { latestHeight ->
            synchronizer.latestBirthdayHeight?.let { latestBirthdayHeight ->
                return (latestHeight.value - latestBirthdayHeight.value).coerceAtLeast(0)
            }
        }
        return 0
    }

    fun quickScanDistance(): Int {
        val latest = synchronizer.latestHeight
        val oneWeek = 60 * 60 * 24 / 75 * 7 // a week's worth of blocks
        val height = BlockHeight.new(
            synchronizer.network,
            ((latest?.value ?: synchronizer.network.saplingActivationHeight.value) - oneWeek)
                .coerceAtLeast(synchronizer.network.saplingActivationHeight.value)
        )
        val foo = runBlocking {
            synchronizer.getNearestRewindHeight(height)
        }
        return ((latest?.value ?: 0) - foo.value).toInt().coerceAtLeast(0)
    }

    fun blocksToMinutesString(blocks: BlockHeight): String {
        val duration = (blocks.value / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", " minutes")
    }

    fun blocksToMinutesString(blocks: Int): String {
        val duration = (blocks / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", " minutes")
    }

    fun blocksToMinutesString(blocks: Long): String {
        val duration = (blocks / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", " minutes")
    }

    private fun getHost(): String {
        return prefs[Const.HOST_SERVER] ?: Const.Default.Server.HOST
    }

    private fun getPort(): Int {
        return prefs[Const.HOST_PORT] ?: Const.Default.Server.PORT
    }

    fun init() {
        initialServer = UiModel(getHost(), getPort().toString())
        uiModels = MutableStateFlow(initialServer)
    }

    suspend fun resetServer() {
        UiModel(
            Const.Default.Server.HOST,
            Const.Default.Server.PORT.toString()
        ).let { default ->
            uiModels.value = default
            submit()
        }
    }

    suspend fun submit() {
        var error: Throwable? = null
        val host = uiModels.value.host
        val port = uiModels.value.portInt
        synchronizer.changeServer(uiModels.value.host, uiModels.value.portInt) {
            error = it
        }
        if (error == null) {
            prefs[Const.HOST_SERVER] = host
            prefs[Const.HOST_PORT] = port
        }
        uiModels.value = uiModels.value.copy(changeError = error, complete = true)
    }

    suspend fun updateServer(context: Context, host: String, port: Int) {
        UiModel(
            host,
            port.toString()
        ).let { default ->
            uiModels.value = default
            submit()
        }
    }

    private fun onUpdateModel(kProperty: KProperty<*>, old: String, new: String) {
        val pendingPort = pendingPortText.toIntOrNull() ?: -1
        uiModels.value = UiModel(
            pendingHost,
            pendingPortText,
            pendingHost != initialServer.host || pendingPortText != initialServer.portText,
            if (!pendingHost.isValidHost()) "Please enter a valid host name or IP" else null,
            if (pendingPort >= 65535) "Please enter a valid port number below 65535" else null
        ).also {
            twig("updated model with $it")
        }
    }

    data class UiModel(
        val host: String = "",
        val portText: String = "",
        val submitEnabled: Boolean = false,
        val hostErrorMessage: String? = null,
        val portErrorMessage: String? = null,
        val changeError: Throwable? = null,
        val complete: Boolean = false
    ) {
        val portInt get() = portText.toIntOrNull() ?: -1
        val hasError get() = hostErrorMessage != null || portErrorMessage != null
    }

    // we can beef this up later if we want to but this is enough for now
    private fun String.isValidHost(): Boolean {
        return !contains("://")
    }
}
