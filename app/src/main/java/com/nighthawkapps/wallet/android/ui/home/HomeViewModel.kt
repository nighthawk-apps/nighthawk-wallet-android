package com.nighthawkapps.wallet.android.ui.home

import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.ZcashSdk.MINERS_FEE
import cash.z.ecc.android.sdk.ext.isShielded
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.network.models.ZcashPriceApiResponse
import com.nighthawkapps.wallet.android.network.repository.CoinMetricsRepository
import com.nighthawkapps.wallet.android.ui.setup.FiatCurrencyViewModel
import com.nighthawkapps.wallet.android.ui.setup.SyncNotificationViewModel
import com.nighthawkapps.wallet.android.ui.util.MemoUtil
import com.nighthawkapps.wallet.android.ui.util.Resource
import com.nighthawkapps.wallet.android.ui.util.Utils
import com.nighthawkapps.wallet.android.ui.util.toUtf8Memo
import com.nighthawkapps.wallet.android.ui.util.WorkManagerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

class HomeViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var coinMetricsRepository: CoinMetricsRepository

    @Inject
    lateinit var lockBox: LockBox

    lateinit var uiModels: Flow<UiModel>

    lateinit var _typedChars: ConflatedBroadcastChannel<Char>

    var initialized = false
    var expectingAmount = 0L

    val balance get() = synchronizer.saplingBalances
    private var _coinMetricsMarketData = MutableStateFlow<ZcashPriceApiResponse?>(null)
    val zcashPriceApiData: StateFlow<ZcashPriceApiResponse?> get() = _coinMetricsMarketData
    val transactions get() = synchronizer.clearedTransactions
    private val formatter by lazy {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                R.string.ns_format_date_time.toAppString()
            ), Locale.getDefault()
        )
    }

    private val fetchPriceScope = CoroutineScope(Dispatchers.IO)

    fun getZecMarketPrice(market: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (market.isBlank()) {
                _coinMetricsMarketData.value = null
                twig("response: no option selected for fiat ${zcashPriceApiData.value}")
                resetSavedCurrencyData()
                return@launch
            }
            if (zcashPriceApiData.value != null && market == zcashPriceApiData.value?.data?.keys?.firstOrNull()) {
                twig("response: coin metric data already available $zcashPriceApiData")
                return@launch
            }
            coinMetricsRepository.getZecMarketData(currency = market)
                .catch {
                    twig("error in getZecMarket data: $it")
                }
                .collect {
                    twig("response: $it")
                    val response = extractCoinMarketData(it)
                    if (response != null && response.data.isNotEmpty()) {
                        _coinMetricsMarketData.value = response
                    } else {
                        _coinMetricsMarketData.value = null
                    }
                    lockBox[Const.AppConstants.KEY_ZEC_AMOUNT] =
                        _coinMetricsMarketData.value?.data?.values?.firstOrNull()?.toString() ?: "0"
                }
        }
    }

    fun getSelectedCurrencyName(): String {
        return FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByName(
            lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] ?: ""
        ).currencyName
    }

    fun getFiatCurrencyMarket(): String {
        return FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByName(
            lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] ?: ""
        ).serverUrl
    }

    private fun resetSavedCurrencyData() {
        lockBox[Const.AppConstants.KEY_ZEC_AMOUNT] = "0"
        lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] = ""
    }

    private fun extractCoinMarketData(resource: Resource<ZcashPriceApiResponse>): ZcashPriceApiResponse? {
        return when (resource) {
            is Resource.Success -> resource.data
            else -> null
        }
    }

    fun initializeMaybe(preTypedChars: String = "0") {
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
        val decimal = '.' // R.string.key_decimal.toAppString()[0]
        val backspace = R.string.key_backspace.toAppString()[0]
        val zec = typedChars.scan(preTypedChars) { acc, c ->
            when {
                // no-op cases
                acc == "0" && c == '0' ||
                        (c == backspace && acc == "0")
                        || (c == decimal && acc.contains(decimal)) -> {
                    acc
                }
                c == backspace && acc.length <= 1 -> {
                    "0"
                }
                c == backspace -> {
                    acc.substring(0, acc.length - 1)
                }
                acc == "0" && c != decimal -> {
                    c.toString()
                }
                acc.contains(decimal) && acc.length - acc.indexOf(decimal) > 8 -> {
                    acc
                }
                else -> {
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
                    orchardBalance = flows[2] as WalletBalance?,
                    saplingBalance = flows[3] as WalletBalance?,
                    transparentBalance = flows[4] as WalletBalance?,
                    pendingSend = flows[5] as String,
                    unminedCount = unminedCount
                )
            }.onStart {
                emit(
                    UiModel(
                        orchardBalance = null,
                        saplingBalance = null,
                        transparentBalance = null
                    )
                )
            }
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
        val processorInfo: CompactBlockProcessor.ProcessorInfo = CompactBlockProcessor.ProcessorInfo(
            null,
            null,
            null,
            null,
            null
        ),
        val orchardBalance: WalletBalance?,
        val saplingBalance: WalletBalance?,
        val transparentBalance: WalletBalance?,
        val pendingSend: String = "0",
        val unminedCount: Int = 0
    ) {
        // Note: the wallet is effectively empty if it cannot cover the miner's fee
        val autoShieldingThreshold = 1_000_000L // 0.01 ZEC
        val hasFunds: Boolean
            get() = (saplingBalance?.available?.value
                ?: 0) > (MINERS_FEE.value.toDouble() / Zatoshi.ZATOSHI_PER_ZEC) // 0.00001
        val hasSaplingBalance: Boolean get() = (saplingBalance?.total?.value ?: 0) > 0L
        val hasAutoshieldFunds: Boolean
            get() = (transparentBalance?.available?.value ?: 0) >= autoShieldingThreshold
        val isSynced: Boolean get() = status == Synchronizer.Status.SYNCED
        val isSendEnabled: Boolean get() = isSynced && hasFunds

        // Processor Info
        val isDownloading = status == Synchronizer.Status.DOWNLOADING
        val isScanning = status == Synchronizer.Status.SCANNING
        val isValidating = status == Synchronizer.Status.VALIDATING
        val isDisconnected = status == Synchronizer.Status.DISCONNECTED
        val downloadProgress: Int get() {
            return processorInfo.run {
                if (lastDownloadRange?.isEmpty() == true) {
                    100
                } else {
                    val progress =
                        ((((lastDownloadedHeight?.value ?: 0) - (lastDownloadRange?.start?.value ?: 0) + 1).coerceAtLeast(0).toFloat() / ((lastDownloadRange?.endInclusive?.value ?: 0) - (lastDownloadRange?.start?.value ?: 0) + 1)) * 100.0f).coerceAtMost(
                            100.0f
                        ).roundToInt()
                    progress
                }
            }
        }
        val scanProgress: Int get() {
            return processorInfo.run {
                if (lastScanRange?.isEmpty() == true) {
                    100
                } else {
                    val progress = ((((lastScannedHeight?.value ?: 0) - (lastScanRange?.start?.value ?: 0) + 1).coerceAtLeast(0).toFloat() / ((lastScanRange?.endInclusive?.value ?: 0) - (lastScanRange?.start?.value ?: 0) + 1)) * 100.0f).coerceAtMost(100.0f).roundToInt()
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

    suspend fun getRecentUIModel(transactionList: List<ConfirmedTransaction>): List<RecentActivityUiModel> {
        val transactions =
            if (transactionList.size > 2) transactionList.subList(0, 2) else transactionList
        return transactions.map { confirmedTransaction ->
            val transactionType =
                if (confirmedTransaction.toAddress.isNullOrEmpty()) RecentActivityUiModel.TransactionType.RECEIVED else RecentActivityUiModel.TransactionType.SENT
            val address =
                if (transactionType == RecentActivityUiModel.TransactionType.RECEIVED) getSender(
                    confirmedTransaction
                ) else confirmedTransaction.toAddress
            val toZecStringShort =
                WalletZecFormmatter.toZecStringShort(Zatoshi(confirmedTransaction.value))
            RecentActivityUiModel(
                transactionType = transactionType,
                transactionTime = formatter.format(confirmedTransaction.blockTimeInSeconds * 1000L),
                isTransactionShielded = address?.equals(
                    R.string.unknown.toAppString(),
                    true
                ) == true || address.isShielded(),
                amount = toZecStringShort,
                isMemoAvailable = confirmedTransaction.memo?.toUtf8Memo()?.isNotBlank() == true,
                zecConvertedValueText = Utils.getZecConvertedAmountText(
                    toZecStringShort,
                    zcashPriceApiData.value
                ),
                confirmedTransaction = confirmedTransaction
            )
        }
    }

    data class RecentActivityUiModel(
        var transactionType: TransactionType? = null,
        var transactionTime: String? = null,
        var isTransactionShielded: Boolean = false,
        var amount: String = "---",
        var isMemoAvailable: Boolean = false,
        var zecConvertedValueText: String? = null,
        val confirmedTransaction: ConfirmedTransaction
    ) {
        enum class TransactionType {
            SENT,
            RECEIVED
        }
    }

    private suspend fun getSender(transaction: ConfirmedTransaction?): String {
        if (transaction == null) return R.string.unknown.toAppString()
        return MemoUtil.findAddressInMemo(transaction, ::isValidAddress)?.toAbbreviatedAddress()
            ?: R.string.unknown.toAppString()
    }

    private suspend fun isValidAddress(address: String): Boolean {
        try {
            return !synchronizer.validateAddress(address).isNotValid
        } catch (t: Throwable) {
        }
        return false
    }

    /**
     * Configure the MoonPay url and return the final URL
     */
    fun getMoonPayUrl(): String {
        return "${Const.Default.Server.BUY_ZEC_BASE_URL}&currencyCode=zec"
    }

    private fun getStartTimeForCoinMetricsApi(): String {
        return DateFormat.format("yyyy-MM-dd", Calendar.getInstance().timeInMillis).toString()
    }

    fun cancelSyncAppNotificationAndReRegister() {
        val syncNotificationPref =
            SyncNotificationViewModel.NotificationSyncPref.getNotificationSyncPrefByText(
                lockBox[Const.AppConstants.KEY_SYNC_NOTIFICATION] ?: ""
            )
        WorkManagerUtils.cancelSyncAppNotificationAndReRegister(syncNotificationPref)
    }
}
