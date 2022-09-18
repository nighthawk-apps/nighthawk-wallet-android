package com.nighthawkapps.wallet.android.ui.send

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.safelyConvertToBigDecimal
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendEnterAmountBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.convertZatoshiToSelectedUnit
import com.nighthawkapps.wallet.android.ext.convertedUnitToZatoshi
import com.nighthawkapps.wallet.android.ext.sanitizeInputValue
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil
import com.nighthawkapps.wallet.android.ui.util.Utils
import kotlinx.coroutines.launch

class SendEnterAmountFragment : BaseFragment<FragmentSendEnterAmountBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()
    private var maxZatoshi: Zatoshi = Zatoshi(0)
    private var availableZatoshi: Zatoshi = Zatoshi(0)
    private lateinit var numberPad: List<TextView>

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
                if (sendViewModel.isAmountValid(sendViewModel.zatoshiAmount, Zatoshi(maxZatoshi.value))) {
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
            binding.tvZec.setOnClickListener {
                if (sendViewModel.getSelectedFiatCurrency().currencyName.isNotBlank()) {
                    onFlipCurrencyClicked()
                }
            }
        }
    }

    private fun onDataReceivedFromDeepLink(data: DeepLinkUtil.SendDeepLinkData?) {
        data?.let {
            sendViewModel.toAddress = data.address
            sendViewModel.memo = data.memo ?: ""
            data.amount?.let {
                sendViewModel.zatoshiAmount = Zatoshi(it)
            }
            val newValue = if (sendViewModel.isZecAmountState) {
                sendViewModel.zatoshiAmount?.value.convertZatoshiToSelectedUnit(sendViewModel.getSelectedFiatUnit())
            } else {
                Utils.calculateZecToOtherCurrencyValue(sendViewModel.zatoshiAmount.convertZatoshiToZecString(), sendViewModel.getZecMarketPrice() ?: "0")
            }
            binding.tvBalance.text = newValue
            onAmountValueUpdated(newValue)
            if (sendViewModel.zatoshiAmount != null && sendViewModel.isAmountValid(sendViewModel.zatoshiAmount, maxZatoshi)) {
                mainActivity?.safeNavigate(R.id.action_nav_send_enter_to_nav_send_review)
            }
        }
    }

    private fun onAmountValueUpdated(inputValue: String) {
        val newValue = inputValue.sanitizeInputValue()
        binding.tvBalance.let {
            it.text = newValue
            val selectedFiatUnit = sendViewModel.getSelectedFiatUnit()
            if (sendViewModel.isZecAmountState) {
                newValue.safelyConvertToBigDecimal().convertedUnitToZatoshi(selectedFiatUnit).let { zatoshi ->
                    sendViewModel.zatoshiAmount = zatoshi
                }
            } else {
                sendViewModel.getZecMarketPrice()?.let { marketPrice ->
                    sendViewModel.zatoshiAmount = Zatoshi(Utils.calculateLocalCurrencyToZatoshi(marketPrice, newValue) ?: 0)
                }
            }
            calculateZecConvertedAmount(sendViewModel.zatoshiAmount)
        }
        updateButtonsUI(newValue)
    }

    private fun calculateZecConvertedAmount(zatoshi: Zatoshi?) {
        sendViewModel.getZecMarketPrice()?.let {
            val selectedCurrencyName = sendViewModel.getSelectedFiatCurrency().currencyName
            val selectedFiatUnit = sendViewModel.getSelectedFiatUnit()
            var drawableEnd: Drawable? = null
            if (selectedCurrencyName.isNotBlank()) {
                drawableEnd = ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_up_down)
                if (sendViewModel.isZecAmountState) {
                    binding.tvConvertedAmount.text = getString(
                        R.string.ns_around,
                        Utils.getZecConvertedAmountText(
                            WalletZecFormmatter.toZecStringShort(zatoshi),
                            it,
                            selectedCurrencyName
                        )
                    )
                    binding.tvZec.text = selectedFiatUnit.unit
                } else {
                    binding.tvConvertedAmount.text = getString(
                        R.string.ns_around,
                        "${
                            WalletZecFormmatter.toZatoshi(
                                Utils.calculateOtherCurrencyToZec(
                                    binding.tvBalance.text.toString(),
                                    it
                                )
                            ).value.convertZatoshiToSelectedUnit(selectedFiatUnit)
                        } ${selectedFiatUnit.unit}"
                    )
                    binding.tvZec.text = selectedCurrencyName
                }
            } else {
                binding.tvZec.text = selectedFiatUnit.unit
            }
            binding.tvZec.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                drawableEnd,
                null
            )
            binding.tvConvertedAmount.isVisible = selectedCurrencyName.isNotBlank()
        }
    }

    private fun onFlipCurrencyClicked() {
        sendViewModel.getZecMarketPrice()?.let {
            val enteredZec = WalletZecFormmatter.toZecStringShort(sendViewModel.zatoshiAmount)
            val convertedValue = if (sendViewModel.isZecAmountState) {
                Utils.calculateZecToOtherCurrencyValue(enteredZec, it)
            } else {
                WalletZecFormmatter.toZatoshi(Utils.calculateOtherCurrencyToZec(binding.tvBalance.text.toString(), it)).value
                    .convertZatoshiToSelectedUnit(sendViewModel.getSelectedFiatUnit())
            }
            sendViewModel.isZecAmountState = sendViewModel.isZecAmountState.not()
            if (convertedValue == "0" && binding.tvBalance.text == "0.") { // This handle a corner case. If user type 0.[dot] and then try to switch the currency and add again .[dot]
                onNewValueEntered("0") // change the pre-entered value of viewModel.enteredValue from 0.[dot] to 0
            } else {
                onAmountValueUpdated(convertedValue)
            }
        }
    }

    private fun getEnteredAmountInZatoshi(): Long {
        val enteredAmount = binding.tvBalance
        return if (sendViewModel.isZecAmountState) {
            enteredAmount.text.toString().safelyConvertToBigDecimal().convertedUnitToZatoshi(sendViewModel.getSelectedFiatUnit()).value
        } else {
            sendViewModel.getZecMarketPrice()?.let {
                Utils.calculateLocalCurrencyToZatoshi(it, enteredAmount.text.toString())
            } ?: -1
        }
    }

    private fun updateButtonsUI(value: String) {
        val amountString = value.toFloatOrNull()?.toString() ?: PREFILLED_VALUE
        if (amountString == PREFILLED_VALUE || (value.toFloatOrNull() ?: -1f) == 0f) {
            updateVisibilityOfButtons(
                scanPaymentCode = true,
                continueButton = false,
                topUpWallet = false,
                notEnoughZcash = false
            )
        } else if (Zatoshi(getEnteredAmountInZatoshi()) > maxZatoshi) {
            updateVisibilityOfButtons(
                scanPaymentCode = false,
                continueButton = false,
                topUpWallet = true,
                notEnoughZcash = true
            )
        } else {
            updateVisibilityOfButtons(
                scanPaymentCode = false,
                continueButton = true,
                topUpWallet = false,
                notEnoughZcash = false
            )
        }
    }

    private fun updateVisibilityOfButtons(
        scanPaymentCode: Boolean,
        continueButton: Boolean,
        topUpWallet: Boolean,
        notEnoughZcash: Boolean
    ) {
        with(binding) {
            tvScanPaymentCode.isVisible = scanPaymentCode
            btnContinue.isVisible = continueButton
            btnTopUpWallet.isVisible = topUpWallet
            btnNotEnoughZCash.isVisible = notEnoughZcash
        }
    }

    private fun onBalanceUpdated(balance: WalletBalance?) {
        maxZatoshi = ((balance?.available ?: Zatoshi(0)) - ZcashSdk.MINERS_FEE).coerceAtLeast(Zatoshi(0))
        availableZatoshi = balance?.available ?: Zatoshi(0)
        val selectedFiatUnit = sendViewModel.getSelectedFiatUnit()
        val spendableBalance = maxZatoshi.value.convertZatoshiToSelectedUnit(selectedFiatUnit)
        binding.tvSpendableBalance.text =
            getString(R.string.ns_spendable_balance, spendableBalance, selectedFiatUnit.unit)
        binding.tvSpendableBalance.setOnClickListener {
            val sanitizedText = spendableBalance.replace(",", "")
            binding.tvBalance.text = sanitizedText
            onAmountValueUpdated(sanitizedText)
        }
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
