package com.nighthawkapps.wallet.android.ui.send

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.onFirstWith
import cash.z.ecc.android.sdk.ext.safelyConvertToBigDecimal
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.sdk.type.WalletBalance
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.convertZecToZatoshi
import com.nighthawkapps.wallet.android.ext.gone
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.onClickNavUp
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.ext.visible
import com.nighthawkapps.wallet.android.ui.MainViewModel
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SendFragment : BaseFragment<FragmentSendBinding>(),
    ClipboardManager.OnPrimaryClipChangedListener {

    private var maxZatoshi: Long? = null
    private var minZatoshi: Long = 1.toLong()
    private var availableZatoshi: Long? = null

    private val sendViewModel: SendViewModel by activityViewModel()
    private val mainViewModel: MainViewModel by activityViewModel()
    lateinit var addressJob: Job

    override fun inflate(inflater: LayoutInflater): FragmentSendBinding =
        FragmentSendBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply View Model
        applyViewModel(sendViewModel)

        // Apply behaviors

        binding.buttonSend.setOnClickListener {
            onSubmit()
        }

        binding.checkIncludeAddress.setOnCheckedChangeListener { _, _ ->
            onIncludeMemo(binding.checkIncludeAddress.isChecked)
        }

        binding.textAddressError.setOnClickListener {
            onPaste()
        }

        binding.textMemo.setOnClickListener {
            onMemo()
        }

        binding.textMax.setOnClickListener {
            onMax()
        }

        binding.inputZcashAddress.apply {
            doAfterTextChanged {
                val textStr = text.toString()
                val trim = textStr.trim()
                // bugfix: prevent cursor from moving while backspacing and deleting whitespace
                if (text.toString() != trim) {
                    setText(trim)
                    setSelection(selectionEnd - (textStr.length - trim.length))
                }
                onAddressChanged(trim)
            }
        }

        binding.backButtonHitArea.onClickNavUp()

        binding.inputZcashMemo.doAfterTextChanged {
            onMemoUpdated()
        }

        binding.textLayoutAddress.setEndIconOnClickListener {
            clearPreFilledData()
            mainActivity?.maybeOpenScan()
        }

        // banners

        binding.backgroundClipboard.setOnClickListener {
            onPaste()
        }
        binding.containerClipboard.setOnClickListener {
            onPaste()
        }
        binding.backgroundLastUsed.setOnClickListener {
            onReuse()
        }
        binding.containerLastUsed.setOnClickListener {
            onReuse()
        }

        lifecycleScope.launchWhenResumed {
            mainViewModel.sendZecDeepLinkData.collect {
                onDataReceivedFromDeepLink(it)
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

    private fun onDataReceivedFromDeepLink(data: DeepLinkUtil.SendDeepLinkData?) {
        data?.let {
            sendViewModel.toAddress = data.address
            sendViewModel.memo = data.memo ?: ""
            sendViewModel.zatoshiAmount = data.amount
            applyViewModel(sendViewModel)
        }
    }

    private fun clearPreFilledData() {
        mainViewModel.setSendZecDeepLinkData(null)
        sendViewModel.reset()
    }

    private fun applyViewModel(model: SendViewModel) {
        // Apply View Model
        if (sendViewModel.zatoshiAmount > 0L) {
            sendViewModel.zatoshiAmount.convertZatoshiToZecString(8).let { amount ->
                binding.inputZcashAmount.setText(amount)
            }
        } else {
            binding.inputZcashAmount.setText(null)
        }

        binding.inputZcashAddress.setText(model.toAddress)
        binding.inputZcashMemo.setText(model.memo)
        binding.checkIncludeAddress.isChecked = model.includeFromAddress
        onMemoUpdated()
    }

    private fun onMemoUpdated() {
        val totalLength = sendViewModel.createMemoToSend().length
        binding.textLayoutMemo.helperText = "$totalLength/${ZcashSdk.MAX_MEMO_SIZE} chars"
        val color = if (totalLength > ZcashSdk.MAX_MEMO_SIZE) R.color.zcashRed else R.color.text_light_dimmed
        binding.textLayoutMemo.setHelperTextColor(ColorStateList.valueOf(color.toAppColor()))
    }

    private fun onClearMemo() {
        binding.inputZcashMemo.setText("")
    }

    private fun onIncludeMemo(checked: Boolean) {
        sendViewModel.afterInitFromAddress {
            sendViewModel.includeFromAddress = checked
            onMemoUpdated()
        }
    }

    private fun onAddressChanged(address: String) {
        if (this::addressJob.isInitialized && !addressJob.isCompleted && !addressJob.isCancelled) {
            addressJob.cancel()
        }
        addressJob = resumedScope.launch {
            val validation = sendViewModel.validateAddress(address)
            binding.buttonSend.isActivated = !validation.isNotValid
            var type = when (validation) {
                is AddressType.Transparent -> "This is a valid transparent address" to R.color.zcashGreen
                is AddressType.Shielded -> "This is a valid shielded address" to R.color.zcashGreen
                else -> "This address appears to be invalid" to R.color.zcashRed
            }
            if (address == sendViewModel.synchronizer.getAddress()) type =
                "Warning, this appears to be your address!" to R.color.zcashRed
            binding.textLayoutAddress.helperText = type.first
            binding.textLayoutAddress.setHelperTextColor(ColorStateList.valueOf(type.second.toAppColor()))

            // if we have the clipboard address but we're changing it, then clear the selection
            if (binding.imageClipboardAddressSelected.isVisible) {
                loadAddressFromClipboard().let { clipboardAddress ->
                    if (address != clipboardAddress) {
                        updateClipboardBanner(false, clipboardAddress)
                    }
                }
            }
            // if we have the last used address but we're changing it, then clear the selection
            if (binding.imageLastUsedAddressSelected.isVisible) {
                loadLastUsedAddress().let { lastAddress ->
                    if (address != lastAddress) {
                        updateLastUsedBanner(false, lastAddress)
                    }
                }
            }
        }
    }

    private fun onSubmit(unused: EditText? = null) {
        sendViewModel.toAddress = binding.inputZcashAddress.text.toString()
        sendViewModel.memo = binding.inputZcashMemo.text?.toString() ?: ""
        sendViewModel.zatoshiAmount = binding.inputZcashAmount.text.toString().safelyConvertToBigDecimal().convertZecToZatoshi()
        binding.inputZcashAmount.convertZecToZatoshi()?.let { sendViewModel.zatoshiAmount = it }
        applyViewModel(sendViewModel)
        sendViewModel.validate(requireContext(), availableZatoshi, maxZatoshi).onFirstWith(resumedScope) { errorMessage ->
            if (errorMessage == null) {
                mainActivity?.authenticate("Please confirm that you want to send ${sendViewModel.zatoshiAmount.convertZatoshiToZecString(8)} ZEC to\n${sendViewModel.toAddress.toAbbreviatedAddress()}") {
                    mainActivity?.safeNavigate(R.id.action_nav_send_to_nav_send_final)
                }
            } else {
                resumedScope.launch {
                    binding.textAddressError.text = errorMessage
                    delay(25L)
                    binding.textAddressError.text = ""
                }
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
        mainViewModel.setSendZecDeepLinkData(null)
        mainViewModel.setIntentData(null)
        sendViewModel.reset()
    }

    override fun onResume() {
        super.onResume()
        updateClipboardBanner()
        updateLastUsedBanner()
        sendViewModel.synchronizer.saplingBalances.collectWith(resumedScope) {
            onBalanceUpdated(it)
        }
        binding.inputZcashAddress.text.toString().let {
            if (!it.isNullOrEmpty()) onAddressChanged(it)
        }
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        maxZatoshi = (balance.availableZatoshi - ZcashSdk.MINERS_FEE_ZATOSHI).coerceAtLeast(0L)
        availableZatoshi = balance.availableZatoshi
    }

    override fun onPrimaryClipChanged() {
        updateClipboardBanner()
        updateLastUsedBanner()
    }

    private fun updateClipboardBanner(selected: Boolean = false, address: String? = loadAddressFromClipboard()) {
        binding.apply {
            updateAddressBanner(
                groupClipboard,
                clipboardAddress,
                imageClipboardAddressSelected,
                imageShield,
                clipboardAddressLabel,
                selected,
                address
            )
        }
    }

    private fun updateLastUsedBanner(
        selected: Boolean = false,
        address: String? = loadLastUsedAddress()
    ) {
        val isBoth = address == loadAddressFromClipboard()
        binding.apply {
            updateAddressBanner(
                groupLastUsed,
                lastUsedAddress,
                imageLastUsedAddressSelected,
                imageLastUsedShield,
                lastUsedAddressLabel,
                selected,
                address.takeUnless { isBoth })
        }
        binding.dividerClipboard.text = if (isBoth) "Last Used and On Clipboard" else "On Clipboard"
    }

    private fun updateAddressBanner(
        group: Group,
        addressTextView: TextView,
        checkIcon: ImageView,
        shieldIcon: ImageView,
        addressLabel: TextView,
        selected: Boolean = false,
        address: String? = loadLastUsedAddress()
    ) {
        resumedScope.launch {
            if (address == null) {
                group.gone()
            } else {
                val userAddress = sendViewModel.synchronizer.getAddress()
                group.visible()
                addressTextView.text = address.toAbbreviatedAddress(16, 16)
                checkIcon.goneIf(!selected)
                ImageViewCompat.setImageTintList(shieldIcon, ColorStateList.valueOf(if (selected) R.color.colorPrimary.toAppColor() else R.color.zcashWhite_12.toAppColor()))
                addressLabel.setText(if (address == userAddress) R.string.send_banner_address_user else R.string.send_banner_address_unknown)
                addressLabel.setTextColor(if (selected) R.color.colorPrimary.toAppColor() else R.color.text_light.toAppColor())
                addressTextView.setTextColor(if (selected) R.color.text_light.toAppColor() else R.color.text_light_dimmed.toAppColor())
            }
        }
    }

    private fun onPaste() {
        mainActivity?.clipboard?.let { clipboard ->
            if (clipboard.hasPrimaryClip()) {
                val address = clipboard.text().toString()
                val applyValue = binding.imageClipboardAddressSelected.isGone
                updateClipboardBanner(applyValue, address)
                binding.inputZcashAddress.setText(address.takeUnless { !applyValue })
            }
        }
    }

    private fun onReuse() {
        val address = loadLastUsedAddress()
        val applyValue = binding.imageLastUsedAddressSelected.isGone
        updateLastUsedBanner(applyValue, address)
        binding.inputZcashAddress.setText(address.takeUnless { !applyValue })
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

    private var lastUsedAddress: String? = null
    private fun loadLastUsedAddress(): String? {
        if (lastUsedAddress == null) sendViewModel.viewModelScope.launch {
            lastUsedAddress = sendViewModel.synchronizer.sentTransactions.first().firstOrNull { !it.toAddress.isNullOrEmpty() }?.toAddress
            updateLastUsedBanner(binding.imageLastUsedAddressSelected.isVisible, lastUsedAddress)
        }
        return lastUsedAddress
    }

    private fun ClipboardManager.text(): CharSequence =
        primaryClip!!.getItemAt(0).coerceToText(mainActivity)
}
