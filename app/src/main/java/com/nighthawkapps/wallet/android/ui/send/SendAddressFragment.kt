package com.nighthawkapps.wallet.android.ui.send

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.onFirstWith
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.validate.AddressType
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendAddressBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.onEditorActionDone
import com.nighthawkapps.wallet.android.ext.onClickNavUp
import com.nighthawkapps.wallet.android.ext.convertZecToZatoshi
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SendAddressFragment : BaseFragment<FragmentSendAddressBinding>(),
    ClipboardManager.OnPrimaryClipChangedListener {

    private var maxZatoshi: Long? = null
    private var minZatoshi: Long = 1.toLong()

    val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendAddressBinding =
        FragmentSendAddressBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButtonHitArea.onClickNavUp()
        binding.buttonNext.setOnClickListener {
            onSubmit()
        }
        binding.textBannerAction.setOnClickListener {
            onPaste()
        }
        binding.textBannerMessage.setOnClickListener {
            onPaste()
        }
        binding.textMemo.setOnClickListener {
            onMemo()
        }
        binding.textMax.setOnClickListener {
            onMax()
        }

        // Apply View Model
        if (sendViewModel.zatoshiAmount > 0L) {
            sendViewModel.zatoshiAmount.convertZatoshiToZecString(8).let { amount ->
                binding.inputZcashAmount.setText(amount)
            }
        } else {
            binding.inputZcashAmount.setText(null)
        }
        if (!sendViewModel.toAddress.isNullOrEmpty()) {
            binding.inputZcashAddress.setText(sendViewModel.toAddress)
        } else {
            binding.inputZcashAddress.setText(null)
        }

        binding.inputZcashAddress.onEditorActionDone(::onSubmit)
        binding.inputZcashAmount.onEditorActionDone(::onSubmit)

        binding.inputZcashAddress.apply {
            doAfterTextChanged {
                val textStr = text.toString()
                val trim = textStr.trim()
                if (text.toString() != trim) {
                    val textView =
                        binding.inputZcashAddress.findViewById<EditText>(R.id.input_zcash_address)
                    val cursorPosition = textView.selectionEnd
                    textView.setText(trim)
                    textView.setSelection(cursorPosition - (textStr.length - trim.length))
                }
                onAddressChanged(trim)
            }
        }

        binding.textLayoutAddress.setEndIconOnClickListener {
            mainActivity?.maybeOpenScan()
        }
    }

    private fun onAddressChanged(address: String) {
        resumedScope.launch {
            var type = when (sendViewModel.validateAddress(address)) {
                is AddressType.Transparent -> "This is a valid transparent address" to R.color.zcashGreen
                is AddressType.Shielded -> "This is a valid shielded address" to R.color.zcashGreen
                is AddressType.Invalid -> "This address appears to be invalid" to R.color.zcashRed
            }
            if (address == sendViewModel.synchronizer.getAddress()) type =
                "Warning, this appears to be your address!" to R.color.zcashRed
            binding.textLayoutAddress.helperText = type.first
            binding.textLayoutAddress.setHelperTextColor(ColorStateList.valueOf(type.second.toAppColor()))
        }
    }

    private fun onSubmit(unused: EditText? = null) {
        sendViewModel.toAddress = binding.inputZcashAddress.text.toString()
        binding.inputZcashAmount.convertZecToZatoshi()?.let { sendViewModel.zatoshiAmount = it }
        sendViewModel.validate(maxZatoshi).onFirstWith(resumedScope) {
            if (it == null) {
                mainActivity?.authenticate("Please confirm that you want to send ${sendViewModel.zatoshiAmount.convertZatoshiToZecString(8)} ZEC to\n${sendViewModel.toAddress.toAbbreviatedAddress()}") {
                    mainActivity?.safeNavigate(R.id.action_nav_send_address_to_send_memo)
                }
            } else {
                resumedScope.launch {
                    binding.textAddressError.text = it
                    delay(2000L)
                    binding.textAddressError.text = ""
                }
            }
        }
    }

    private fun onMax() {
        if (maxZatoshi != null) {
            binding.inputZcashAmount.apply {
                setText(maxZatoshi.convertZatoshiToZecString(8))
                postDelayed({
                    requestFocus()
                    setSelection(text?.length ?: 0)
                }, 10L)
            }
        }
    }

    private fun onMemo() {
        if (maxZatoshi != null) {
            binding.inputZcashAmount.apply {
                setText(minZatoshi.convertZatoshiToZecString(8))
                postDelayed({
                    requestFocus()
                    setSelection(text?.length ?: 0)
                }, 10L)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity?.clipboard?.addPrimaryClipChangedListener(this)
    }

    override fun onDetach() {
        super.onDetach()
        mainActivity?.clipboard?.removePrimaryClipChangedListener(this)
    }

    override fun onResume() {
        super.onResume()
        updateClipboardBanner()
        sendViewModel.synchronizer.balances.collectWith(resumedScope) {
            onBalanceUpdated(it)
        }
        binding.inputZcashAddress.text.toString().let {
            if (!it.isNullOrEmpty()) onAddressChanged(it)
        }
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        binding.textLayoutAmount.helperText =
            "You have ${balance.availableZatoshi.coerceAtLeast(0L)
                .convertZatoshiToZecString(8)} ZEC available"
        maxZatoshi = (balance.availableZatoshi - ZcashSdk.MINERS_FEE_ZATOSHI).coerceAtLeast(0L)
    }

    override fun onPrimaryClipChanged() {
        updateClipboardBanner()
    }

    private fun updateClipboardBanner() {
        binding.groupBanner.goneIf(loadAddressFromClipboard() == null)
    }

    private fun onPaste() {
        mainActivity?.clipboard?.let { clipboard ->
            if (clipboard.hasPrimaryClip()) {
                binding.inputZcashAddress.setText(clipboard.text())
            }
        }
    }

    private fun loadAddressFromClipboard(): String? {
        mainActivity?.clipboard?.apply {
            if (hasPrimaryClip()) {
                text()?.let { text ->
                    if (text.startsWith("zs") && text.length > 70) {
                        return@loadAddressFromClipboard text.toString()
                    }
                    // treat t-addrs differently in the future
                    if (text.startsWith("t1") && text.length > 32) {
                        return@loadAddressFromClipboard text.toString()
                    }
                }
            }
        }
        return null
    }

    private fun ClipboardManager.text(): CharSequence? =
        primaryClip?.getItemAt(0)?.coerceToText(mainActivity)
}