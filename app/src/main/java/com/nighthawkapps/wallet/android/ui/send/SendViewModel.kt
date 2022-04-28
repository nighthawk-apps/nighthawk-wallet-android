package com.nighthawkapps.wallet.android.ui.send

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.util.INCLUDE_MEMO_PREFIX_STANDARD
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

    fun onNewValueEntered(newValue: String) {
        _enteredValue.value = newValue
    }

    fun isAmountValid(enteredZatoshi: Long, maxAvailableZatoshi: Long): Boolean {
        return maxAvailableZatoshi > ZcashSdk.MINERS_FEE_ZATOSHI && enteredZatoshi < maxAvailableZatoshi - ZcashSdk.MINERS_FEE_ZATOSHI
    }

    fun getZecMarketPrice(): String? {
        return lockBox[Const.AppConstants.KEY_ZEC_AMOUNT]
    }

    fun send(): Flow<PendingTransaction> {
        val memoToSend = createMemoToSend()
        val keys = DerivationTool.deriveSpendingKeys(
            lockBox.getBytes(Const.Backup.SEED)!!,
            synchronizer.network
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

    fun validate(context: Context, availableZatoshi: Long?, maxZatoshi: Long?) = flow<String?> {
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

    fun reset() {
        fromAddress = ""
        toAddress = ""
        memo = ""
        zatoshiAmount = -1L
        includeFromAddress = false
        _enteredValue.value = "0"
    }
}
