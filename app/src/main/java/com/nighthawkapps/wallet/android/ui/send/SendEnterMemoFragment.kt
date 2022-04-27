package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import cash.z.ecc.android.sdk.ext.ZcashSdk
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendEnterMemoBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class SendEnterMemoFragment : BaseFragment<FragmentSendEnterMemoBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendEnterMemoBinding {
        return FragmentSendEnterMemoBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack()
        binding.btnContinue.setOnClickListener { onContinue() }
        binding.etMemo.doAfterTextChanged {
            sendViewModel.memo = it?.toString() ?: ""
            onMemoUpdated()
        }
        binding.checkIncludeAddress.setOnCheckedChangeListener { _, _ ->
            onIncludeMemo(binding.checkIncludeAddress.isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.etMemo.setText(sendViewModel.memo)
    }

    private fun onContinue() {
        sendViewModel.memo = binding.etMemo.text?.toString() ?: ""
        mainActivity?.safeNavigate(R.id.action_nav_enter_memo_to_send_review)
    }

    private fun onIncludeMemo(checked: Boolean) {
        sendViewModel.afterInitFromAddress {
            sendViewModel.includeFromAddress = checked
            onMemoUpdated()
        }
    }

    private fun onMemoUpdated() {
        val totalLength = sendViewModel.createMemoToSend().length
        binding.btnContinue.isEnabled = totalLength <= ZcashSdk.MAX_MEMO_SIZE
        binding.btnContinue.text = getString(if (totalLength < 1) R.string.ns_skip else R.string.ns_continue)
    }
}
