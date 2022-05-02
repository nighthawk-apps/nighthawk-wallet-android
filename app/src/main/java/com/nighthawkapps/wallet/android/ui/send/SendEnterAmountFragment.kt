package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.type.WalletBalance
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendEnterAmountBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.convertZecToZatoshi
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil
import com.nighthawkapps.wallet.android.ui.util.Utils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SendEnterAmountFragment : BaseFragment<FragmentSendEnterAmountBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()
    private lateinit var numberPad: List<TextView>
    private var maxZatoshi: Long = 0L
    private var availableZatoshi: Long = 0L

    override fun inflate(inflater: LayoutInflater): FragmentSendEnterAmountBinding {
        return FragmentSendEnterAmountBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    sendViewModel.enteredValue.collect {
                        onAmountValueUpdated(it)
                    }
                }
                launch {
                    sendViewModel.synchronizer.saplingBalances.collect {
                        onBalanceUpdated(it)
                    }
                }
                launch {
                    sendViewModel.sendZecDeepLinkData.collect {
                        onDataReceivedFromDeepLink(it)
                    }
                }
            }
        }

        with(binding) {
            numberPad = arrayListOf(
                buttonNumberPad0.asKey(),
                buttonNumberPad1.asKey(),
                buttonNumberPad2.asKey(),
                buttonNumberPad3.asKey(),
                buttonNumberPad4.asKey(),
                buttonNumberPad5.asKey(),
                buttonNumberPad6.asKey(),
                buttonNumberPad7.asKey(),
                buttonNumberPad8.asKey(),
                buttonNumberPad9.asKey()
            )

            buttonNumberPadBack.setOnClickListener {
                onDeleteClicked()
            }
            buttonNumberPadDecimal.setOnClickListener {
                onDecimalClicked()
            }
            tvScanPaymentCode.setOnClickListener {
                mainActivity?.maybeOpenScan()
            }
            btnContinue.setOnClickListener {
                if (sendViewModel.isAmountValid(sendViewModel.zatoshiAmount, maxZatoshi)) {
                    twig("Amount entered to send ${sendViewModel.zatoshiAmount}")
                    mainActivity?.safeNavigate(R.id.action_nav_enter_amount_to_enter_address)
                }
            }
            binding.btnTopUpWallet.setOnClickListener {
                mainActivity?.safeNavigate(R.id.action_nav_enter_amount_to_transfer_top_up)
            }
            binding.btnNotEnoughZCash.setOnClickListener {
                mainActivity?.safeNavigate(R.id.action_nav_enter_amount_to_transfer)
            }
        }
    }

    private fun onDataReceivedFromDeepLink(data: DeepLinkUtil.SendDeepLinkData?) {
        data?.let {
            sendViewModel.toAddress = data.address
            sendViewModel.memo = data.memo ?: ""
            sendViewModel.zatoshiAmount = data.amount
            val newValue = sendViewModel.zatoshiAmount.convertZatoshiToZecString()
            binding.tvBalance.text = newValue
            onAmountValueUpdated(newValue)
            if (sendViewModel.isAmountValid(sendViewModel.zatoshiAmount, maxZatoshi)) {
                mainActivity?.safeNavigate(R.id.action_nav_send_enter_to_nav_send_review)
            }
        }
    }

    private fun onAmountValueUpdated(newValue: String) {
        binding.tvBalance.let {
            it.text = newValue
            it.convertZecToZatoshi()?.let { zatoshi ->
                sendViewModel.zatoshiAmount = zatoshi
                calculateZecConvertedAmount(zatoshi)
            }
        }
        updateButtonsUI(newValue)
    }

    private fun calculateZecConvertedAmount(zatoshi: Long) {
        sendViewModel.getZecMarketPrice()?.let {
            val selectedCurrencyName = sendViewModel.getSelectedFiatCurrency().currencyName
            binding.tvConvertedAmount.text = getString(R.string.ns_around, Utils.getZecConvertedAmountText(WalletZecFormmatter.toZecStringShort(zatoshi), it, selectedCurrencyName))
        }
    }

    private fun updateButtonsUI(value: String) {
        val amountString = value.toFloatOrNull()?.toString() ?: PREFILLED_VALUE
        if (amountString == PREFILLED_VALUE || value.toFloatOrNull() ?: -1f == 0f) {
            updateVisibilityOfButtons(scanPaymentCode = true, continueButton = false, topUpWallet = false, notEnoughZcash = false)
        } else if (binding.tvBalance.convertZecToZatoshi() ?: -1 > maxZatoshi) {
            updateVisibilityOfButtons(scanPaymentCode = false, continueButton = false, topUpWallet = true, notEnoughZcash = true)
        } else {
            updateVisibilityOfButtons(scanPaymentCode = false, continueButton = true, topUpWallet = false, notEnoughZcash = false)
        }
    }

    private fun updateVisibilityOfButtons(scanPaymentCode: Boolean, continueButton: Boolean, topUpWallet: Boolean, notEnoughZcash: Boolean) {
        with(binding) {
            tvScanPaymentCode.isVisible = scanPaymentCode
            btnContinue.isVisible = continueButton
            btnTopUpWallet.isVisible = topUpWallet
            btnNotEnoughZCash.isVisible = notEnoughZcash
        }
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        maxZatoshi = (balance.availableZatoshi - ZcashSdk.MINERS_FEE_ZATOSHI).coerceAtLeast(0L)
        availableZatoshi = balance.availableZatoshi
    }

    private fun TextView.asKey(): TextView {
        setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                onKey(text)
            }
        }
        return this
    }

    private fun onKey(newChar: CharSequence) {
        binding.tvBalance.let {
            val oldValue = it.text ?: ""
            var updatedValue = oldValue.toString()
            if (oldValue.isBlank().not() && oldValue == PREFILLED_VALUE) {
                updatedValue = newChar.toString()
            } else {
                if (updatedValue.isBlank()) updatedValue = "0"
                updatedValue += newChar.toString()
            }
            onNewValueEntered(updatedValue)
        }
    }

    private fun onDeleteClicked() {
        binding.tvBalance.text?.let {
            val oldValue = it
            if (oldValue.isBlank().not() && oldValue != PREFILLED_VALUE) { // if only PREFILLED_VALUE available as text we can ignore the delete key
                var newValue = oldValue.dropLast(1)
                if (newValue.isBlank()) {
                    newValue = "0"
                }
                onNewValueEntered(newValue.toString())
            }
        }
    }

    private fun onDecimalClicked() {
        binding.tvBalance.text?.let {
            val oldValue = it
            if (oldValue.contains(DOT)) return
            val newValue = if (oldValue.isBlank()) {
                PREFILLED_VALUE + DOT
            } else {
                oldValue.toString() + DOT
            }
            onNewValueEntered(newValue)
        }
    }

    private fun onNewValueEntered(newValue: String) {
        sendViewModel.onNewValueEntered(newValue)
    }

    companion object {
        const val DOT = "."
        const val PREFILLED_VALUE = "0"
    }
}
