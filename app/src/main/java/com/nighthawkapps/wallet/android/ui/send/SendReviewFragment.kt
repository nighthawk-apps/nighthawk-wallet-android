package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendReviewBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.toSplitColorSpan
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.util.Utils

class SendReviewFragment : BaseFragment<FragmentSendReviewBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendReviewBinding {
        return FragmentSendReviewBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack()
        fillUI()
    }

    private fun fillUI() {
        binding.tvBalance.text = sendViewModel.zatoshiAmount.convertZatoshiToZecString()
        binding.tvConvertedAmount.text = calculateZecConvertedAmount(sendViewModel.zatoshiAmount)
        binding.groupMemo.isVisible = sendViewModel.createMemoToSend().isNotBlank()
        binding.tvMemo.text = sendViewModel.createMemoToSend()
        binding.tvRecipient.text = if (sendViewModel.isShielded) getString(R.string.ns_shielded) else getString(R.string.ns_transparent)
        binding.tvAddress.text = getSpannedAddress()
        binding.tvSubTotal.text = getString(R.string.ns_zec_amount, sendViewModel.zatoshiAmount.convertZatoshiToZecString())
        binding.tvFee.text = getString(R.string.ns_zec_amount, ZcashSdk.MINERS_FEE_ZATOSHI.convertZatoshiToZecString())
        val total = (sendViewModel.zatoshiAmount + ZcashSdk.MINERS_FEE_ZATOSHI).convertZatoshiToZecString()
        binding.tvTotalAmount.text = total
    }

    private fun calculateZecConvertedAmount(zatoshi: Long): String? {
        return sendViewModel.getZecMarketPrice()?.let {
            getString(R.string.ns_around, Utils.getZecConvertedAmountText(WalletZecFormmatter.toZecStringShort(zatoshi), it))
        }
    }

    private fun getSpannedAddress(): CharSequence {
        return sendViewModel.toAddress.toSplitColorSpan(R.color.ns_white, R.color.ns_white, NO_OF_SPANNED_COUNT, NO_OF_SPANNED_COUNT)
    }

    companion object {
        const val NO_OF_SPANNED_COUNT = 10
    }
}
