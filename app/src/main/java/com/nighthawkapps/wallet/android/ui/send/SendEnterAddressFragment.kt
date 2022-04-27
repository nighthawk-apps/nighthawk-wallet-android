package com.nighthawkapps.wallet.android.ui.send

import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendEnterAddressBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SendEnterAddressFragment : BaseFragment<FragmentSendEnterAddressBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()
    private lateinit var addressJob: Job

    override fun inflate(inflater: LayoutInflater): FragmentSendEnterAddressBinding {
        return FragmentSendEnterAddressBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack()
        binding.etAddress.setText(sendViewModel.toAddress)
        binding.ivScanIcon.setOnClickListener { mainActivity?.maybeOpenScan() }
        binding.ivClearAllIcon.setOnClickListener { binding.etAddress.text?.clear() }
        binding.tvPasteFromClipBoard.setOnClickListener { onPasteClicked() }
        binding.btnContinue.setOnClickListener { onContinue() }
        binding.etAddress.apply {
            doAfterTextChanged {
                val enteredText = it?.toString() ?: ""
                val trimmedText = enteredText.trim()
                if (trimmedText != enteredText) {
                    setText(trimmedText)
                    setSelection(selectionEnd - (enteredText.length - trimmedText.length))
                }
                onAddressChanged(trimmedText)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sendViewModel.toAddress.let {
            if (it.isNotBlank()) {
                binding.etAddress.setText(it)
                onAddressChanged(it)
            }
        }
    }

    private fun onContinue() {
        sendViewModel.toAddress = binding.etAddress.text.toString()
        mainActivity?.safeNavigate(R.id.action_nav_enter_address_to_enter_memo)
    }

    private fun onAddressChanged(address: String) {
        if (this::addressJob.isInitialized && !addressJob.isCompleted && !addressJob.isCancelled) {
            addressJob.cancel()
        }
        addressJob = viewLifecycleOwner.lifecycleScope.launch {
            val validation = sendViewModel.validateAddress(address)
            binding.btnContinue.isEnabled = validation.isNotValid.not()
        }
        updateContinueAndAddressIcon((address.isBlank()))
    }

    private fun updateContinueAndAddressIcon(isEmptyText: Boolean) {
        binding.tvPasteFromClipBoard.isVisible = isEmptyText
        binding.ivScanIcon.isVisible = isEmptyText
        binding.ivClearAllIcon.isVisible = isEmptyText.not()
    }

    private fun onPasteClicked() {
        loadAddressFromClipboard()?.let {
            binding.etAddress.setText(it)
        } ?: Toast.makeText(requireContext(), getString(R.string.ns_clipboard_wrong_text_paste_error), Toast.LENGTH_SHORT).show()
    }

    private fun loadAddressFromClipboard(): String? {
        mainActivity?.clipboard?.apply {
            if (hasPrimaryClip()) {
                text()?.let { text ->
                    if (text.startsWith("zs") && text.length > 70) {
                        return@loadAddressFromClipboard text.toString()
                    }
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
