package com.nighthawkapps.wallet.android.ui.send

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.isShielded
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.history.HistoryViewModel
import com.nighthawkapps.wallet.android.ui.setup.FiatCurrencyViewModel
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil
import com.nighthawkapps.wallet.android.ui.util.INCLUDE_MEMO_PREFIX_STANDARD
import com.nighthawkapps.wallet.android.ui.util.Utils
import com.nighthawkapps.wallet.android.ui.util.toUtf8Memo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class SendViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    lateinit var synchronizer: Synchronizer

    val networkName get() = synchronizer.network.networkName

    private val _enteredValue = MutableStateFlow("0")
    val enteredValue: StateFlow<String> get() = _enteredValue
    private val _sendZecDeepLinkData = MutableStateFlow<DeepLinkUtil.SendDeepLinkData?>(null)
    val sendZecDeepLinkData: StateFlow<DeepLinkUtil.SendDeepLinkData?> get() = _sendZecDeepLinkData
    private val _pendingTransaction = MutableStateFlow<PendingTransaction?>(null)
    val pendingTransaction: Flow<PendingTransaction?> = _pendingTransaction
    val transactionDetailsUIModel = pendingTransaction.map { it.toTransactionUIModel() }
    private val latestHeight get() = synchronizer.latestHeight
    var isZecAmountState = true // To track user selected the zec balance mode or converted balance mode

    var fromAddress: String = ""
    var toAddress: String = ""
    var memo: String = ""
    var zatoshiAmount: Long = -1L
    var includeFromAddress: Boolean = false
        set(value) {
            require(!value || (value && fromAddress.isNotBlank())) {
                "Error: fromAddress was empty while attempting to include it in the memo. Verify" +
                        " that initFromAddress() has previously been called on this viewmodel."
            }
            field = value
        }
    val isShielded get() = toAddress.startsWith("z")

    fun onNewValueEntered(newValue: String) {
        _enteredValue.value = newValue
    }

    fun isAmountValid(enteredZatoshi: Long, maxAvailableZatoshi: Long): Boolean {
        return enteredZatoshi in 0..maxAvailableZatoshi
    }

    fun getZecMarketPrice(): String? {
        return lockBox[Const.AppConstants.KEY_ZEC_AMOUNT]
    }

    fun getSelectedFiatCurrency(): FiatCurrencyViewModel.FiatCurrency {
        return FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByName(lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] ?: "")
    }

    fun setSendZecDeepLinkData(data: DeepLinkUtil.SendDeepLinkData?) {
        _sendZecDeepLinkData.value = data
    }

    fun send() {
        viewModelScope.launch(Dispatchers.IO) {
            val memoToSend = createMemoToSend()
            val keys = DerivationTool.deriveSpendingKeys(
                lockBox.getBytes(Const.Backup.SEED)!!,
                synchronizer.network
            )
            synchronizer.sendToAddress(keys[0], zatoshiAmount, toAddress, memoToSend.chunked(ZcashSdk.MAX_MEMO_SIZE).firstOrNull() ?: "")
                    .onEach { tx ->
                        _pendingTransaction.value = tx
                        twig("Received pending txUpdate: $tx")
                    }
                    .catch { throwable ->
                        twig("Error in shielding $throwable")
                    }
                    .collect()
        }
    }

    fun cancel(pendingId: Long) {
        viewModelScope.launch {
            synchronizer.cancelSpend(pendingId)
        }
    }

    fun createMemoToSend() = if (includeFromAddress) "$memo\n$INCLUDE_MEMO_PREFIX_STANDARD\n$fromAddress" else memo

    suspend fun validateAddress(address: String): AddressType =
        synchronizer.validateAddress(address)

    fun validate(context: Context, availableZatoshi: Long?, maxZatoshi: Long?) = flow {
        when {
            synchronizer.validateAddress(toAddress).isNotValid -> {
                emit(context.getString(R.string.send_validation_error_address_invalid))
            }
            zatoshiAmount < 1 -> {
                emit(context.getString(R.string.send_validation_error_amount_minimum))
            }
            availableZatoshi == null -> {
                emit(context.getString(R.string.send_validation_error_unknown_funds))
            }
            availableZatoshi == 0L -> {
                emit(context.getString(R.string.send_validation_error_no_available_funds))
            }
            availableZatoshi > 0 && availableZatoshi < ZcashSdk.MINERS_FEE_ZATOSHI -> {
                emit(context.getString(R.string.send_validation_error_dust))
            }
            maxZatoshi != null && zatoshiAmount > maxZatoshi -> {
                emit(context.getString(R.string.send_validation_error_too_much, WalletZecFormmatter.toZecStringFull(maxZatoshi), NighthawkWalletApp.instance.getString(R.string.symbol)))
            }
            createMemoToSend().length > ZcashSdk.MAX_MEMO_SIZE -> {
                emit(context.getString(R.string.send_validation_error_memo_length, ZcashSdk.MAX_MEMO_SIZE))
            }
            else -> emit(null)
        }
    }

    fun afterInitFromAddress(block: () -> Unit) {
        viewModelScope.launch {
            fromAddress = synchronizer.getAddress()
            block()
        }
    }

    private fun PendingTransaction?.toTransactionUIModel(latestHeight: Int? = this@SendViewModel.latestHeight): HistoryViewModel.TransactionDetailsUIModel = HistoryViewModel.TransactionDetailsUIModel()
        .apply {
        this@toTransactionUIModel.let { tx ->
            network = synchronizer.network.networkName
            transactionId = toTxId(tx?.rawTransactionId)
            isInbound = when {
                !(tx?.toAddress.isNullOrEmpty()) -> false
                tx != null && tx.toAddress.isEmpty() && tx.value > 0L && tx.minedHeight > 0 -> true
                else -> null
            }
            isMined = tx?.minedHeight != null && tx.minedHeight > synchronizer.network.saplingActivationHeight
            transactionAmount = WalletZecFormmatter.toZecStringFull(tx?.value)
            convertedAmount = calculateZecConvertedAmount(tx?.value ?: 0L) ?: ""
            blockId = String.format("%,d", tx?.minedHeight ?: 0)
            val flags =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            timestamp = if (tx == null || tx.createTime <= 0) getString(R.string.transaction_timestamp_unavailable) else DateUtils.getRelativeDateTimeString(
                NighthawkWalletApp.instance,
                tx.createTime * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                flags
            ).toString()

            // memo logic
            val txMemo = tx?.memo.toUtf8Memo()
            if (txMemo.isNotEmpty()) {
                memo = txMemo
            }

            // confirmation logic
            tx?.let {
                val isMined = it.createTime != 0L
                if (isMined) {
                    val hasLatestHeight = latestHeight != null && latestHeight > synchronizer.network.saplingActivationHeight
                    confirmation = if (it.minedHeight > 0 && hasLatestHeight) {
                        val confirmations = latestHeight!! - it.minedHeight + 1
                        transactionStatus = getString(R.string.ns_confirmed)
                        transactionStatusStartDrawableId = R.drawable.ic_icon_confirmed
                        "$confirmations"
                    } else {
                        if (!hasLatestHeight && isSufficientlyOld(tx)) {
                            twig("Warning: could not load latest height from server to determine confirmations but this transaction is mined and old enough to be considered confirmed")
                            transactionStatusStartDrawableId = R.drawable.ic_icon_confirmed
                            transactionStatus = getString(R.string.ns_confirmed)
                            getString(R.string.transaction_status_confirmed)
                        } else {
                            twig("Warning: could not determine confirmation text value so it will be left null!")
                            transactionStatusStartDrawableId = R.drawable.ic_done_24dp
                            transactionStatus = getString(R.string.ns_sent)
                            getString(R.string.transaction_confirmation_count_unavailable)
                        }
                    }
                } else {
                    transactionStatusStartDrawableId = R.drawable.ic_icon_preparing
                    transactionStatus = getString(R.string.ns_processing)
                    confirmation = getString(R.string.transaction_status_pending)
                }
            }

            when (isInbound) {
                true -> {
                    totalAmount = WalletZecFormmatter.toZecStringFull(tx?.value)
                    subTotal = totalAmount
                    iconRotation = 0f
                    toAddress = getString(R.string.unknown)
                    recipientAddressType = getString(if (toAddress.isShielded() || toAddress.equals(getString(R.string.unknown), true)) R.string.ns_shielded else R.string.ns_transparent)
                }
                false -> {
                    totalAmount = WalletZecFormmatter.toZecStringFull(tx?.value?.plus(ZcashSdk.MINERS_FEE_ZATOSHI))
                    subTotal = WalletZecFormmatter.toZecStringFull(tx?.value)
                    iconRotation = 180f
                    networkFees = "0.00001"
                    toAddress = tx?.toAddress ?: ""
                    recipientAddressType = getString(if (toAddress.isShielded() || toAddress.equals(getString(R.string.unknown), true)) R.string.ns_shielded else R.string.ns_transparent)
                }
                null -> {
                    twig("Error: transaction appears to be invalid.")
                }
            }
        }
    }

    private fun calculateZecConvertedAmount(zatoshi: Long): String? {
        return getZecMarketPrice()?.let {
            val selectedFiatCurrencyName = FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByName(lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] ?: "").currencyName
            if (selectedFiatCurrencyName.isBlank()) null
            else Utils.getZecConvertedAmountText(WalletZecFormmatter.toZecStringShort(zatoshi), it, currencyName = selectedFiatCurrencyName)
        }
    }

    private fun getString(@StringRes id: Int) = id.toAppString()

    private fun toTxId(tx: ByteArray?): String? {
        if (tx == null) return null
        val sb = StringBuilder(tx.size * 2)
        for (i in (tx.size - 1) downTo 0) {
            sb.append(String.format("%02x", tx[i]))
        }
        return sb.toString()
    }

    private fun isSufficientlyOld(tx: PendingTransaction): Boolean {
        val threshold = 75 * 1000 * 25 // approx 25 blocks
        val delta = System.currentTimeMillis() / 1000L - tx.createTime
        return tx.minedHeight > synchronizer.network.saplingActivationHeight &&
                delta < threshold
    }

    fun reset() {
        fromAddress = ""
        toAddress = ""
        memo = ""
        zatoshiAmount = -1L
        includeFromAddress = false
        _enteredValue.value = "0"
        _sendZecDeepLinkData.value = null
    }
}
