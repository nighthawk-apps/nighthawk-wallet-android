package com.nighthawkapps.wallet.android.ui.home

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.type.WalletBalance
import com.nighthawkapps.wallet.android.ext.pending
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
        val shieldedBalance: WalletBalance = WalletBalance(),
        val transparentBalance: WalletBalance = WalletBalance(),
        var showAvailable: Boolean = false
    ) {
        /** Whether to make calculations based on total or available zatoshi */

        val canAutoShield: Boolean = transparentBalance.availableZatoshi > 0L

        val balanceShielded: String
            get() {
                return if (showAvailable) shieldedBalance.availableZatoshi.toDisplay()
                else shieldedBalance.totalZatoshi.toDisplay()
            }

        val balanceTransparent: String
            get() {
                return if (showAvailable) transparentBalance.availableZatoshi.toDisplay()
                else transparentBalance.totalZatoshi.toDisplay()
            }

        val balanceTotal: String
            get() {
                return if (showAvailable) (shieldedBalance.availableZatoshi + transparentBalance.availableZatoshi).toDisplay()
                else (shieldedBalance.totalZatoshi + transparentBalance.totalZatoshi).toDisplay()
            }

        val paddedShielded get() = pad(balanceShielded)
        val paddedTransparent get() = pad(balanceTransparent)
        val paddedTotal get() = pad(balanceTotal)
        val maxLength get() = maxOf(balanceShielded.length, balanceTransparent.length, balanceTotal.length)
        val hasPending = shieldedBalance.availableZatoshi != shieldedBalance.totalZatoshi ||
                transparentBalance.availableZatoshi != transparentBalance.totalZatoshi
        private fun Long.toDisplay(): String {
            return convertZatoshiToZecString(8, 8)
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
            val default = WalletBalance()
            return shieldedBalance.availableZatoshi != default.availableZatoshi ||
                    shieldedBalance.totalZatoshi != default.totalZatoshi ||
                    shieldedBalance.availableZatoshi != default.availableZatoshi ||
                    shieldedBalance.totalZatoshi != default.totalZatoshi
        }
    }

    data class StatusModel(
        val balances: BalanceModel,
        val pending: List<PendingTransaction>,
        val info: CompactBlockProcessor.ProcessorInfo
    ) {
        val pendingUnconfirmed = pending.filter { it.isSubmitSuccess() && it.isMined() && !it.isConfirmed(info.lastScannedHeight) }
        val pendingUnmined = pending.filter { it.isSubmitSuccess() && !it.isMined() }
        val pendingShieldedBalance = balances.shieldedBalance.pending
        val pendingTransparentBalance = balances.transparentBalance.pending
        val hasUnconfirmed = pendingUnconfirmed.isNotEmpty()
        val hasUnmined = pendingUnmined.isNotEmpty()
        val hasPendingShieldedBalance = pendingShieldedBalance > 0L
        val hasPendingTransparentBalance = pendingTransparentBalance > 0L
        val missingBlocks = (info.networkBlockHeight - info.lastScannedHeight).coerceAtLeast(0)

        private fun PendingTransaction.isConfirmed(networkBlockHeight: Int): Boolean {
            return isMined() && (networkBlockHeight - minedHeight + 1) > 10 // fix: plus 1 because the mined block counts as the FIRST confirmation
        }

        fun remainingConfirmations(confirmationsRequired: Int = 10) =
            pendingUnconfirmed
                .map { confirmationsRequired - (info.lastScannedHeight - it.minedHeight + 1) } // fix: plus 1 because the mined block counts as the FIRST confirmation
                .filter { it > 0 }
                .sortedDescending()
    }
}
