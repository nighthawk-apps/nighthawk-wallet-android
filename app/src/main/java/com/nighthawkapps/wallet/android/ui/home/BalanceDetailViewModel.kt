package com.nighthawkapps.wallet.android.ui.home

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import com.nighthawkapps.wallet.android.lockbox.LockBox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import javax.inject.Inject

class BalanceDetailViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var lockBox: LockBox

    var showAvailable: Boolean = true
        set(value) {
            field = value
            latestBalance?.showAvailable = value
        }

    var latestBalance: BalanceModel? = null

    val balances: Flow<BalanceModel>
        get() = combineTransform(
            synchronizer.saplingBalances,
            synchronizer.transparentBalances
        ) { saplingBalance, transparentBalance ->
            BalanceModel(saplingBalance, transparentBalance, showAvailable).let {
                latestBalance = it
                emit(it)
            }
        }

    val statuses: Flow<StatusModel>
        get() = combineTransform(
            balances,
            synchronizer.pendingTransactions,
            synchronizer.processorInfo
        ) { balances, pending, info ->
            emit(StatusModel(balances, pending, info))
        }

    data class BalanceModel(
        val shieldedBalance: WalletBalance?,
        val transparentBalance: WalletBalance?,
        var showAvailable: Boolean = false
    ) {
        /** Whether to make calculations based on total or available zatoshi */

        val canAutoShield: Boolean = (transparentBalance?.available?.value ?: 0L) > ZcashSdk.MINERS_FEE.value

        val balanceShielded: String
            get() {
                return if (showAvailable) shieldedBalance?.available.toDisplay()
                else shieldedBalance?.total.toDisplay()
            }

        val balanceTransparent: String
            get() {
                return if (showAvailable) transparentBalance?.available.toDisplay()
                else transparentBalance?.total.toDisplay()
            }

        val balanceTotal: String
            get() {
                return if (showAvailable) ((shieldedBalance?.available ?: Zatoshi(0)) + (transparentBalance?.available ?: Zatoshi(0))).toDisplay()
                else ((shieldedBalance?.total ?: Zatoshi(0)) + (transparentBalance?.total ?: Zatoshi(0))).toDisplay()
            }

        val paddedShielded get() = pad(balanceShielded)
        val paddedTransparent get() = pad(balanceTransparent)
        val paddedTotal get() = pad(balanceTotal)
        val maxLength get() = maxOf(balanceShielded.length, balanceTransparent.length, balanceTotal.length)
        val hasPending = (null != shieldedBalance && shieldedBalance.available != shieldedBalance.total) ||
                (null != transparentBalance && transparentBalance.available != transparentBalance.total)
        private fun Zatoshi?.toDisplay(): String {
            return this?.convertZatoshiToZecString(8, 8) ?: "0"
        }

        private fun pad(balance: String): String {
            var diffLength = maxLength - balance.length
            return buildString {
                repeat(diffLength) {
                    append(' ')
                }
                append(balance)
            }
        }

        fun hasData(): Boolean {
            return shieldedBalance != null || transparentBalance != null
        }
    }

    data class StatusModel(
        val balances: BalanceModel,
        val pending: List<PendingTransaction>,
        val info: CompactBlockProcessor.ProcessorInfo
    ) {
        val pendingUnconfirmed = pending.filter { it.isSubmitSuccess() && it.isMined() && !it.isConfirmed(info.lastScannedHeight) }
        val pendingUnmined = pending.filter { it.isSubmitSuccess() && !it.isMined() }
        val pendingShieldedBalance = balances.shieldedBalance?.pending
        val pendingTransparentBalance = balances.transparentBalance?.pending
        val hasUnconfirmed = pendingUnconfirmed.isNotEmpty()
        val hasUnmined = pendingUnmined.isNotEmpty()
        val hasPendingShieldedBalance = (pendingShieldedBalance?.value ?: 0L) > 0L
        val hasPendingTransparentBalance = (pendingTransparentBalance?.value ?: 0L) > 0L
        val missingBlocks = ((info.networkBlockHeight?.value ?: 0) - (info.lastScannedHeight?.value ?: 0)).coerceAtLeast(0)

        private fun PendingTransaction.isConfirmed(networkBlockHeight: BlockHeight?): Boolean {
            return networkBlockHeight?.let {
                isMined() && (it.value - minedHeight + 1) > 10 // fix: plus 1 because the mined block counts as the FIRST confirmation
            } ?: false
        }

        fun remainingConfirmations(confirmationsRequired: Int = 10) =
            pendingUnconfirmed
                .map { confirmationsRequired - (info.lastScannedHeight!!.value - it.minedHeight + 1) } // fix: plus 1 because the mined block counts as the FIRST confirmation
                .filter { it > 0 }
                .sortedDescending()
    }
}
