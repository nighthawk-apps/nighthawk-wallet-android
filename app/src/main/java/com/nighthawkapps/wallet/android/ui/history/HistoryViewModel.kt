package com.nighthawkapps.wallet.android.ui.history

import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.db.entity.valueInZatoshi
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.isShielded
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Zatoshi
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ext.toAppStringFormatted
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ext.convertZatoshiToSelectedUnit
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.setup.FiatCurrencyViewModel
import com.nighthawkapps.wallet.android.ui.util.MemoUtil
import com.nighthawkapps.wallet.android.ui.util.Utils
import com.nighthawkapps.wallet.android.ui.util.toUtf8Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class HistoryViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    val selectedTransaction = MutableStateFlow<ConfirmedTransaction?>(null)
    val uiModels = selectedTransaction.map { it.toUiModel() }
    val transactionDetailsUIModel = selectedTransaction.map { it.toTransactionUIModel() }

    val transactions get() = synchronizer.clearedTransactions
    val balance get() = synchronizer.saplingBalances
    val latestHeight get() = synchronizer.latestHeight

    suspend fun getAddress() = synchronizer.getAddress()

    override fun onCleared() {
        super.onCleared()
        twig("HistoryViewModel cleared!")
    }

    //
    // History Item UiModel
    //

    data class UiModel(
        var topLabel: String = "",
        var topValue: String = "",
        var bottomLabel: String = "",
        var bottomValue: String = "",
        var minedHeight: String = "",
        var timestamp: String = "",
        var iconRotation: Float = -1f,

        var fee: String? = null,
        var source: String? = null,
        var memo: String? = null,
        var address: String? = null,
        var isInbound: Boolean? = null,
        var isMined: Boolean = false,
        var confirmation: String? = null,
        var txId: String? = null
    )

    private suspend fun ConfirmedTransaction?.toUiModel(latestHeight: BlockHeight? = this@HistoryViewModel.latestHeight): UiModel = UiModel().apply {
        this@toUiModel.let { tx ->
            txId = toTxId(tx?.rawTransactionId)
            isInbound = when {
                !(tx?.toAddress.isNullOrEmpty()) -> false
                tx != null && tx.toAddress.isNullOrEmpty() && tx.value > 0L && tx.minedHeight > 0 -> true
                else -> null
            }
            isMined = tx?.minedHeight != null && tx.minedHeight > synchronizer.network.saplingActivationHeight.value
            topValue = if (tx == null) "" else "\$${WalletZecFormmatter.toZecStringFull(tx.valueInZatoshi)}"
            minedHeight = String.format("%,d", tx?.minedHeight ?: 0)
            val flags =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            timestamp = if (tx == null || tx.blockTimeInSeconds <= 0) getString(R.string.transaction_timestamp_unavailable) else DateUtils.getRelativeDateTimeString(
                NighthawkWalletApp.instance,
                tx.blockTimeInSeconds * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                flags
            ).toString()

            // memo logic
            val txMemo = tx?.memo.toUtf8Memo()
            if (!txMemo.isEmpty()) {
                memo = txMemo
            }

            // confirmation logic
            // TODO: clean all of this up and remove/improve reliance on `isSufficientlyOld` function. Also, add a constant for the number of confirmations we expect.
            tx?.let {
                val isMined = it.blockTimeInSeconds != 0L
                if (isMined) {
                    val hasLatestHeight = latestHeight != null && latestHeight > synchronizer.network.saplingActivationHeight
                    if (it.minedHeight > 0 && hasLatestHeight) {
                        val confirmations = latestHeight?.value!! - it.minedHeight + 1
                        confirmation = if (confirmations >= 10) getString(R.string.transaction_status_confirmed) else "$confirmations ${getString(
                            R.string.transaction_status_confirming
                        )}"
                    } else {
                        if (!hasLatestHeight && isSufficientlyOld(tx)) {
                            twig("Warning: could not load latestheight from server to determine confirmations but this transaction is mined and old enough to be considered confirmed")
                            confirmation = getString(R.string.transaction_status_confirmed)
                        } else {
                            twig("Warning: could not determine confirmation text value so it will be left null!")
                            confirmation = getString(R.string.transaction_confirmation_count_unavailable)
                        }
                    }
                } else {
                    confirmation = getString(R.string.transaction_status_pending)
                }
            }

            when (isInbound) {
                true -> {
                    topLabel = getString(R.string.transaction_story_inbound)
                    bottomLabel = getString(R.string.transaction_story_inbound_total)
                    bottomValue = "\$${WalletZecFormmatter.toZecStringFull(tx?.valueInZatoshi)}"
                    iconRotation = 315f
                    source = getString(R.string.transaction_story_to_shielded)
                    address = MemoUtil.findAddressInMemo(tx, (synchronizer as SdkSynchronizer)::isValidAddress)
                }
                false -> {
                    topLabel = getString(R.string.transaction_story_outbound)
                    bottomLabel = getString(R.string.transaction_story_outbound_total)
                    bottomValue = "\$${WalletZecFormmatter.toZecStringFull(Zatoshi((tx?.valueInZatoshi?.value ?: 0) + ZcashSdk.MINERS_FEE.value))}"
                    iconRotation = 135f
                    fee = "+ 0.00001 network fee"
                    source = getString(R.string.transaction_story_from_shielded)
                    address = tx?.toAddress
                }
                null -> {
                    twig("Error: transaction appears to be invalid.")
                }
            }
        }
    }

    data class TransactionDetailsUIModel(
        var transactionAmount: String = "",
        var selectedUnit: String = "ZEC",
        var convertedAmount: String = "",
        var transactionStatus: String = "",
        @DrawableRes var transactionStatusStartDrawableId: Int = R.drawable.ic_icon_finalizing,
        var memo: String? = null,
        var timestamp: String = "",
        var network: String = "",
        var blockId: String = "",
        var confirmation: String = "0",
        var transactionId: String? = null,
        var recipientAddressType: String = "",
        var toAddress: String = "",
        var subTotal: String = "",
        var networkFees: String? = null,
        var totalAmount: String = "",
        var isInbound: Boolean? = null,
        var isMined: Boolean = false,
        var iconRotation: Float = 0f
    )

    private suspend fun ConfirmedTransaction?.toTransactionUIModel(latestHeight: BlockHeight? = this@HistoryViewModel.latestHeight): TransactionDetailsUIModel = TransactionDetailsUIModel().apply {
        this@toTransactionUIModel.let { tx ->
            network = synchronizer.network.networkName
            transactionId = toTxId(tx?.rawTransactionId)
            isInbound = when {
                !(tx?.toAddress.isNullOrEmpty()) -> false
                tx != null && tx.toAddress.isNullOrEmpty() && tx.value > 0L && tx.minedHeight > 0 -> true
                else -> null
            }
            isMined = tx?.minedHeight != null && tx.minedHeight > synchronizer.network.saplingActivationHeight.value
            val selectedFiatUnit = getSelectedFiatUnit()
            transactionAmount = tx?.valueInZatoshi?.value.convertZatoshiToSelectedUnit(selectedFiatUnit)
            selectedUnit = selectedFiatUnit.unit
            convertedAmount = calculateZecConvertedAmount(tx?.value ?: 0L) ?: ""
            blockId = String.format("%,d", tx?.minedHeight ?: 0)
            val flags =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            timestamp = if (tx == null || tx.blockTimeInSeconds <= 0) getString(R.string.transaction_timestamp_unavailable) else DateUtils.getRelativeDateTimeString(
                NighthawkWalletApp.instance,
                tx.blockTimeInSeconds * 1000,
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
                val isMined = it.blockTimeInSeconds != 0L
                if (isMined) {
                    val hasLatestHeight = latestHeight != null && latestHeight > synchronizer.network.saplingActivationHeight
                    confirmation = if (it.minedHeight > 0 && hasLatestHeight) {
                        val confirmations = latestHeight?.value!! - it.minedHeight + 1
                        transactionStatus = getString(R.string.ns_confirmed)
                        transactionStatusStartDrawableId = R.drawable.ic_icon_confirmed
                        "$confirmations"
                    } else {
                        if (!hasLatestHeight && isSufficientlyOld(tx)) {
                            twig("Warning: could not load latestheight from server to determine confirmations but this transaction is mined and old enough to be considered confirmed")
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
                    totalAmount = tx?.valueInZatoshi?.value.convertZatoshiToSelectedUnit(selectedFiatUnit)
                    subTotal = totalAmount
                    iconRotation = 0f
                    toAddress = MemoUtil.findAddressInMemo(tx, (synchronizer as SdkSynchronizer)::isValidAddress) ?: getString(R.string.unknown)
                    recipientAddressType = getString(if (toAddress.isShielded() || toAddress.equals(getString(R.string.unknown), true)) R.string.ns_shielded else R.string.ns_transparent)
                }
                false -> {
                    totalAmount = (tx?.valueInZatoshi?.plus(ZcashSdk.MINERS_FEE))?.value.convertZatoshiToSelectedUnit(getSelectedFiatUnit())
                    subTotal = tx?.valueInZatoshi?.value.convertZatoshiToSelectedUnit(selectedFiatUnit)
                    iconRotation = 180f
                    networkFees = ZcashSdk.MINERS_FEE.value.convertZatoshiToSelectedUnit(getSelectedFiatUnit())
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
            val selectedFiatCurrencyName = FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByName(prefs[Const.AppConstants.KEY_LOCAL_CURRENCY] ?: "").currencyName
            if (selectedFiatCurrencyName.isBlank()) null
            else Utils.getZecConvertedAmountText(WalletZecFormmatter.toZecStringShort(Zatoshi(zatoshi)), it, currencyName = selectedFiatCurrencyName)
        }
    }

    private fun getZecMarketPrice(): String? {
        return prefs[Const.AppConstants.KEY_ZEC_AMOUNT]
    }

    fun getSelectedFiatUnit(): FiatCurrencyViewModel.FiatUnit {
        return FiatCurrencyViewModel.FiatUnit.getFiatUnit(prefs[Const.AppConstants.KEY_LOCAL_UNIT] ?: "")
    }

    private fun getString(@StringRes id: Int) = id.toAppString()

    private fun getString(@StringRes id: Int, vararg args: Any) = id.toAppStringFormatted(args)

    private fun toTxId(tx: ByteArray?): String? {
        if (tx == null) return null
        val sb = StringBuilder(tx.size * 2)
        for (i in (tx.size - 1) downTo 0) {
            sb.append(String.format("%02x", tx[i]))
        }
        return sb.toString()
    }

    // TODO: determine this in a more generic and technically correct way. For now, this is good enough.
    //       the goal is just to improve the edge cases where the latest height isn't known but other
    //       information suggests that the TX is confirmed. We can improve this, later.
    private fun isSufficientlyOld(tx: ConfirmedTransaction): Boolean {
        val threshold = 75 * 1000 * 25 // approx 25 blocks
        val delta = System.currentTimeMillis() / 1000L - tx.blockTimeInSeconds
        return tx.minedHeight > synchronizer.network.saplingActivationHeight.value &&
                delta < threshold
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
}
