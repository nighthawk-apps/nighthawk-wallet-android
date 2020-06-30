package com.nighthawkapps.wallet.android.ui.send

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendAddressBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.*
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Funnel.Send
import com.nighthawkapps.wallet.android.feedback.Report.Tap.*
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.ecc.android.sdk.ext.*
import cash.z.ecc.android.sdk.validate.AddressType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SendAddressFragment : BaseFragment<FragmentSendAddressBinding>(),
    ClipboardManager.OnPrimaryClipChangedListener {
    override val screen = Report.Screen.SEND_ADDRESS

    private var maxZatoshi: Long? = null

    val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendAddressBinding =
        FragmentSendAddressBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButtonHitArea.onClickNavTo(R.id.action_nav_send_address_to_nav_home) { tapped(SEND_ADDRESS_BACK) }
        binding.buttonNext.setOnClickListener {
            onSubmit().also { tapped(SEND_ADDRESS_NEXT) }
        }
        binding.textBannerAction.setOnClickListener {
            onPaste().also { tapped(SEND_ADDRESS_PASTE) }
        }
        binding.textBannerMessage.setOnClickListener {
            onPaste().also { tapped(SEND_ADDRESS_PASTE) }
        }
        binding.textMax.setOnClickListener {
            onMax().also { tapped(SEND_ADDRESS_MAX) }
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

        binding.inputZcashAddress.onEditorActionDone(::onSubmit).also { tapped(SEND_ADDRESS_DONE_ADDRESS) }
        binding.inputZcashAmount.onEditorActionDone(::onSubmit).also { tapped(SEND_ADDRESS_DONE_AMOUNT) }

        binding.inputZcashAddress.apply {
            doAfterTextChanged {
                val textStr = text.toString()
                val trim = textStr.trim()
                if (text.toString() != trim) {
                    val textView = binding.inputZcashAddress.findViewById<EditText>(R.id.input_zcash_address)
                    val cursorPosition = textView.selectionEnd;
                    textView.setText(trim)
                    textView.setSelection(cursorPosition-(textStr.length-trim.length))
                }
                onAddressChanged(trim)
            }
        }

        binding.textLayoutAddress.setEndIconOnClickListener {
            mainActivity?.maybeOpenScan().also { tapped(SEND_ADDRESS_SCAN) }
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
                sendViewModel.funnel(Send.AddressPageComplete)
                mainActivity?.safeNavigate(R.id.action_nav_send_address_to_send_memo)
            } else {
                resumedScope.launch {
                    binding.textAddressError.text = it
                    delay(1500L)
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
            "You have ${balance.availableZatoshi.coerceAtLeast(0L).convertZatoshiToZecString(8)} available"
        maxZatoshi = (balance.availableZatoshi - ZcashSdk.MINERS_FEE_ZATOSHI).coerceAtLeast(0L)
    }

    override fun onPrimaryClipChanged() {
        twig("clipboard changed!")
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

    private fun ClipboardManager.text(): CharSequence =
        primaryClip!!.getItemAt(0).coerceToText(mainActivity)
}