package com.nighthawkapps.wallet.android.ui.home

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.ZcashSdk.MINERS_FEE_ZATOSHI
import cash.z.ecc.android.sdk.ext.ZcashSdk.ZATOSHI_PER_ZEC
import cash.z.ecc.android.sdk.type.WalletBalance
import com.google.gson.Gson
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.util.price.PriceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

class HomeViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    lateinit var uiModels: Flow<UiModel>

    lateinit var _typedChars: ConflatedBroadcastChannel<Char>

    var initialized = false

    val balance get() = synchronizer.saplingBalances
    var priceModel: PriceModel? = null

    private val fetchPriceScope = CoroutineScope(Dispatchers.IO)

    fun initPrice() {
        fetchPriceScope.launch {
            supervisorScope {
                if (priceModel == null) {
                    val client = OkHttpClient()
                    val urlBuilder = "https://api.lightwalletd.com/price.json".toHttpUrlOrNull()?.newBuilder()
                    val url = urlBuilder?.build().toString()
                    val request: Request = Request.Builder().url(url).build()
                    val gson = Gson()
                    var responseBody: ResponseBody? = null

                    try {
                        responseBody = client.newCall(request).execute().body
                    } catch (e: IOException) {
                        twig("initPrice + ${e.message}" + "$responseBody")
                    } catch (e: IllegalStateException) {
                        twig("initPrice + ${e.message}" + "$responseBody")
                    }
                    if (responseBody != null) {
                        try {
                            priceModel = gson.fromJson(responseBody.string(), PriceModel::class.java)
                        } catch (e: IOException) {
                            twig("initPrice + ${e.message}" + "$priceModel")
                        }
                    }
                }
            }
        }
    }

    fun initializeMaybe() {
        twig("init called")
        if (initialized) {
            twig("Warning already initialized HomeViewModel. Ignoring call to initialize.")
            return
        }

        if (::_typedChars.isInitialized) {
            _typedChars.close()
        }
        _typedChars = ConflatedBroadcastChannel()
        val typedChars = _typedChars.asFlow()

        val zec = typedChars.scan("0") { acc, c ->
            when {
                // no-op cases
                acc == "0" && c == '0' || (c == '<' && acc == "0") || (c == '.' && acc.contains('.')) -> {
                    twig("triggered: 1  acc: $acc  c: $c")
                    acc
                }
                c == '<' && acc.length <= 1 -> {
                    twig("triggered: 2 $typedChars")
                    "0"
                }
                c == '<' -> {
                    twig("triggered: 3")
                    acc.substring(0, acc.length - 1)
                }
                acc == "0" && c != '.' -> {
                    twig("triggered: 4 $typedChars")
                    c.toString()
                }
                else -> {
                    twig("triggered: 5  $typedChars")
                    "$acc$c"
                }
            }
        }
        twig("initializing view models stream")
        uiModels = synchronizer.run {
            combine(
                status,
                processorInfo,
                orchardBalances,
                saplingBalances,
                transparentBalances,
                zec,
                pendingTransactions.distinctUntilChanged()
                // unfortunately we have to use an untyped array here rather than typed parameters because combine only supports up to 5 typed params
            ) { flows ->
                val unminedCount = (flows[6] as List<PendingTransaction>).count {
                    it.isSubmitSuccess() && !it.isMined()
                }
                UiModel(
                    status = flows[0] as Synchronizer.Status,
                    processorInfo = flows[1] as CompactBlockProcessor.ProcessorInfo,
                    orchardBalance = flows[2] as WalletBalance,
                    saplingBalance = flows[3] as WalletBalance,
                    transparentBalance = flows[4] as WalletBalance,
                    pendingSend = flows[5] as String,
                    unminedCount = unminedCount
                )
            }.onStart { emit(UiModel()) }
        }.conflate()
    }

    override fun onCleared() {
        super.onCleared()
        twig("HomeViewModel cleared!")
    }

    suspend fun onChar(c: Char) {
        _typedChars.send(c)
    }

    data class UiModel(
        val status: Synchronizer.Status = Synchronizer.Status.DISCONNECTED,
        val processorInfo: CompactBlockProcessor.ProcessorInfo = CompactBlockProcessor.ProcessorInfo(),
        val orchardBalance: WalletBalance = WalletBalance(),
        val saplingBalance: WalletBalance = WalletBalance(),
        val transparentBalance: WalletBalance = WalletBalance(),
        val pendingSend: String = "0",
        val unminedCount: Int = 0
    ) {
        // Note: the wallet is effectively empty if it cannot cover the miner's fee
        val hasFunds: Boolean get() = saplingBalance.availableZatoshi > (MINERS_FEE_ZATOSHI.toDouble() / ZATOSHI_PER_ZEC) // 0.00001
        val hasSaplingBalance: Boolean get() = saplingBalance.totalZatoshi > 0
        val hasAutoshieldFunds: Boolean get() = transparentBalance.availableZatoshi >= NighthawkWalletApp.instance.autoshieldThreshold
        val isSynced: Boolean get() = status == Synchronizer.Status.SYNCED
        val isSendEnabled: Boolean get() = isSynced && hasFunds

        // Processor Info
        val isDownloading = status == Synchronizer.Status.DOWNLOADING
        val isScanning = status == Synchronizer.Status.SCANNING
        val isValidating = status == Synchronizer.Status.VALIDATING
        val isDisconnected = status == Synchronizer.Status.DISCONNECTED
        val downloadProgress: Int get() {
            return processorInfo.run {
                if (lastDownloadRange.isEmpty()) {
                    100
                } else {
                    val progress =
                        (((lastDownloadedHeight - lastDownloadRange.first + 1).coerceAtLeast(0).toFloat() / (lastDownloadRange.last - lastDownloadRange.first + 1)) * 100.0f).coerceAtMost(
                            100.0f
                        ).roundToInt()
                    progress
                }
            }
        }
        val scanProgress: Int get() {
            return processorInfo.run {
                if (lastScanRange.isEmpty()) {
                    100
                } else {
                    val progress = (((lastScannedHeight - lastScanRange.first + 1).coerceAtLeast(0).toFloat() / (lastScanRange.last - lastScanRange.first + 1)) * 100.0f).coerceAtMost(100.0f).roundToInt()
                    progress
                }
            }
        }
        val totalProgress: Float get() {
            val downloadWeighted = 0.40f * (downloadProgress.toFloat() / 100.0f).coerceAtMost(1.0f)
            val scanWeighted = 0.60f * (scanProgress.toFloat() / 100.0f).coerceAtMost(1.0f)
            return downloadWeighted.coerceAtLeast(0.0f) + scanWeighted.coerceAtLeast(0.0f)
        }
    }

    /**
     * Configure the MoonPay url and return the final URL
     */
    fun getMoonPayUrl(): String {
        return "${Const.Default.Server.BUY_ZEC_BASE_URL}&currencyCode=zec"
    }
}
