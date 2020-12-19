package com.nighthawkapps.wallet.android.ui.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.validate.AddressType
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.util.INCLUDE_MEMO_PREFIX_STANDARD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class SendViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var initializer: Initializer

    var fromAddress: String = ""
    var toAddress: String = ""
    var memo: String = ""
    var zatoshiAmount: Long = -1L
    var includeFromAddress: Boolean = false
        set(value) {
            require(!value || (value && !fromAddress.isNullOrEmpty())) {
                "Error: fromAddress was empty while attempting to include it in the memo. Verify" +
                        " that initFromAddress() has previously been called on this viewmodel."
            }
            field = value
        }
    val isShielded get() = toAddress.startsWith("z")

    fun send(): Flow<PendingTransaction> {
        val memoToSend = createMemoToSend()
        val keys = DerivationTool.deriveSpendingKeys(
            lockBox.getBytes(Const.Backup.SEED)!!
        )
        return synchronizer.sendToAddress(
            keys[0],
            zatoshiAmount,
            toAddress,
            memoToSend.chunked(ZcashSdk.MAX_MEMO_SIZE).firstOrNull() ?: ""
        ).onEach {
            twig("Received pending txUpdate: ${it?.toString()}")
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

    fun validate(availableZatoshi: Long?, maxZatoshi: Long?) = flow<String?> {
        when {
            synchronizer.validateAddress(toAddress).isNotValid -> {
                emit("Please enter a valid address.")
            }
            zatoshiAmount < 1 -> {
                emit("Please go back and enter at least 1 Zatoshi.")
            }
            availableZatoshi == null -> {
                emit("Available funds not found. Please try again in a moment.")
            }
            availableZatoshi == 0L -> {
                emit("No funds available to send.")
            }
            availableZatoshi > 0 && availableZatoshi < ZcashSdk.MINERS_FEE_ZATOSHI -> {
                emit("Insufficient funds to cover miner's fee.")
            }
            maxZatoshi != null && zatoshiAmount > maxZatoshi -> {
                emit("Please go back and enter no more than ${maxZatoshi.convertZatoshiToZecString(8)} ZEC.")
            }
            createMemoToSend().length > ZcashSdk.MAX_MEMO_SIZE -> {
                emit("Memo must be less than ${ZcashSdk.MAX_MEMO_SIZE} in length.")
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

    fun reset() {
        fromAddress = ""
        toAddress = ""
        memo = ""
        zatoshiAmount = -1L
        includeFromAddress = false
    }
}
