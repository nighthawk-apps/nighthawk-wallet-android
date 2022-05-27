package com.nighthawkapps.wallet.android.ui.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.WalletBalance
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.pending
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.lockbox.LockBox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class AutoShieldViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var lockBox: LockBox

    var latestBalance: BalanceModel? = null

    val balances get() = combineTransform(
        synchronizer.orchardBalances,
        synchronizer.saplingBalances,
        synchronizer.transparentBalances
    ) { o, s, t ->
        BalanceModel(o, s, t).let {
            latestBalance = it
            emit(it)
        }
    }

    val statuses get() = combineTransform(synchronizer.saplingBalances, synchronizer.pendingTransactions, synchronizer.processorInfo) { balance, pending, info ->
        val unconfirmed = pending.filter { !it.isConfirmed(info.networkBlockHeight) }
        val unmined = pending.filter { it.isSubmitSuccess() && !it.isMined() }
        val pending = balance.pending
        emit(StatusModel(unmined, unconfirmed, pending, info.networkBlockHeight))
    }

    private fun PendingTransaction.isConfirmed(networkBlockHeight: Int): Boolean {
        return isMined() && (networkBlockHeight - minedHeight + 1) > 10
    }

    fun cancel(id: Long) {
        viewModelScope.launch {
            synchronizer.cancelSpend(id)
        }
    }

    /**
     * Update the autoshielding achievement and return true if this is the first time.
     */
    fun updateAutoshieldAchievement(): Boolean {
        val existingValue = lockBox.getBoolean(Const.App.TRIGGERED_SHIELDING)
        return if (!existingValue) {
            lockBox.setBoolean(Const.App.TRIGGERED_SHIELDING, true)
            true
        } else {
            false
        }
    }

    fun shieldFunds(): Flow<PendingTransaction> {
        return lockBox.getBytes(Const.Backup.SEED)?.let {
            val sk = runBlocking { DerivationTool.deriveSpendingKeys(it, synchronizer.network)[0] }
            val tsk = runBlocking { DerivationTool.deriveTransparentSecretKey(it, synchronizer.network) }
            val addr = runBlocking { DerivationTool.deriveTransparentAddressFromPrivateKey(tsk, synchronizer.network) }
            synchronizer.shieldFunds(sk, tsk, "${ZcashSdk.DEFAULT_SHIELD_FUNDS_MEMO_PREFIX}\nAll UTXOs from $addr").onEach { tx ->
                twig("Received shielding txUpdate: ${tx?.toString()}")
            }
        } ?: throw IllegalStateException("Seed was expected but it was not found!")
    }

    data class BalanceModel(
        val orchardBalance: WalletBalance = WalletBalance(),
        val saplingBalance: WalletBalance = WalletBalance(),
        val transparentBalance: WalletBalance = WalletBalance()
    ) {
        val balanceShielded: String = saplingBalance.availableZatoshi.toDisplay()
        val balanceTransparent: String = transparentBalance.availableZatoshi.toDisplay()
        val balanceTotal: String = (saplingBalance.availableZatoshi + transparentBalance.availableZatoshi).toDisplay()
        val canAutoShield: Boolean = transparentBalance.availableZatoshi > 0L

        val maxLength = maxOf(balanceShielded.length, balanceTransparent.length, balanceTotal.length)
        val paddedShielded = pad(balanceShielded)
        val paddedTransparent = pad(balanceTransparent)
        val paddedTotal = pad(balanceTotal)

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
    }

    data class StatusModel(
        val pendingUnconfirmed: List<PendingTransaction> = listOf(),
        val pendingUnmined: List<PendingTransaction> = listOf(),
        val pendingBalance: Long = 0L,
        val latestHeight: Int = 0
    ) {
        val hasUnconfirmed = pendingUnconfirmed.isNotEmpty()
        val hasUnmined = pendingUnmined.isNotEmpty()
        val hasPendingBalance = pendingBalance > 0L

        fun remainingConfirmations(latestHeight: Int, confirmationsRequired: Int = 10) =
            pendingUnconfirmed
                .map { confirmationsRequired - (latestHeight - it.minedHeight + 1) }
                .filter { it > 0 }
                .sortedDescending()
    }
}
