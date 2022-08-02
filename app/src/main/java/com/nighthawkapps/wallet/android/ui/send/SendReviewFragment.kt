package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.onFirstWith
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendReviewBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.toSplitColorSpan
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.util.Utils
import kotlinx.coroutines.launch
import java.util.Locale

class SendReviewFragment : BaseFragment<FragmentSendReviewBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()
    private var maxZatoshi: Long = 0L
    private var availableZatoshi: Long = 0L

    override fun inflate(inflater: LayoutInflater): FragmentSendReviewBinding {
        return FragmentSendReviewBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                sendViewModel.synchronizer.saplingBalances.collect {
                    onBalanceUpdated(it!!)
                }
            }
        }

        binding.hitAreaExit.onClickNavBack()
        binding.btnSendZcash.setOnClickListener { onSendClicked() }
        fillUI()
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        maxZatoshi = (balance.available - ZcashSdk.MINERS_FEE).value.coerceAtLeast(0L)
        availableZatoshi = balance.available.value
    }

    private fun onSendClicked() {
        sendViewModel.validate(requireContext(), availableZatoshi, maxZatoshi).onFirstWith(resumedScope) { errorMessage ->
            if (errorMessage == null) {
                mainActivity?.authenticate("Please confirm that you want to send ${sendViewModel.zatoshiAmount.convertZatoshiToZecString(8)} ZEC") {
                    mainActivity?.safeNavigate(R.id.action_nav_send_review_to_send_status)
                }
            } else {
                resumedScope.launch {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fillUI() {
        binding.tvBalance.text = sendViewModel.zatoshiAmount.convertZatoshiToZecString()
        binding.tvNetwork.text = sendViewModel.networkName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        binding.tvConvertedAmount.text = calculateZecConvertedAmount(sendViewModel.zatoshiAmount?.value!!)
        binding.groupMemo.isVisible = sendViewModel.createMemoToSend().isNotBlank()
        binding.tvMemo.text = sendViewModel.createMemoToSend()
        binding.tvRecipient.text = if (sendViewModel.isShielded) getString(R.string.ns_shielded) else getString(R.string.ns_transparent)
        binding.tvAddress.text = getSpannedAddress()
        binding.tvSubTotal.text = getString(R.string.ns_zec_amount, sendViewModel.zatoshiAmount.convertZatoshiToZecString())
        binding.tvFee.text = getString(R.string.ns_zec_amount, ZcashSdk.MINERS_FEE.convertZatoshiToZecString())
        val total = (sendViewModel.zatoshiAmount!! + ZcashSdk.MINERS_FEE).convertZatoshiToZecString()
        binding.tvTotalAmount.text = total
    }

    private fun calculateZecConvertedAmount(zatoshi: Long): String? {
        return sendViewModel.getZecMarketPrice()?.let {
            val selectedCurrencyName = sendViewModel.getSelectedFiatCurrency().currencyName
            if (selectedCurrencyName.isNotBlank()) {
                getString(R.string.ns_around, Utils.getZecConvertedAmountText(WalletZecFormmatter.toZecStringShort(Zatoshi(zatoshi)), it, selectedCurrencyName))
            } else ""
        }
    }

    private fun getSpannedAddress(): CharSequence {
        return sendViewModel.toAddress.toSplitColorSpan(R.color.ns_white, R.color.ns_white, NO_OF_SPANNED_COUNT, NO_OF_SPANNED_COUNT)
    }

    companion object {
        const val NO_OF_SPANNED_COUNT = 10
    }
}
