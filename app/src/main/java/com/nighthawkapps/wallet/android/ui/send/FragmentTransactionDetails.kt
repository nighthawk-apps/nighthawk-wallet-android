package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentTransactionDetailsBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.history.HistoryViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class FragmentTransactionDetails : BaseFragment<FragmentTransactionDetailsBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()
    private val historyViewModel: HistoryViewModel by activityViewModel()
    private val navArgs: FragmentTransactionDetailsArgs by navArgs()

    override fun inflate(inflater: LayoutInflater): FragmentTransactionDetailsBinding {
        return FragmentTransactionDetailsBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            if (navArgs.isFromSendFlow) {
                launch {
                    sendViewModel.transactionDetailsUIModel.collect {
                        onUIModelUpdated(it)
                    }
                }
            } else {
                launch {
                    historyViewModel.transactionDetailsUIModel.collect {
                        onUIModelUpdated(it)
                    }
                }
            }
        }
        binding.hitAreaExit.onClickNavBack()
        binding.tvAddress.apply {
            setOnClickListener { text?.let { copyText(it.toString(), "Address") } }
        }
        binding.tvMemo.apply {
            setOnClickListener { text?.let { copyText(it.toString(), "Memo") } }
        }
    }

    private fun onUIModelUpdated(uiModel: HistoryViewModel.TransactionDetailsUIModel) {
        uiModel.let {
            with(binding) {
                ivTransactionStatusIcon.rotation = it.iconRotation
                tvBalance.text = it.transactionAmount
                tvConvertedAmount.apply {
                    text = getString(R.string.ns_around, it.convertedAmount)
                    isVisible = it.convertedAmount.isNotBlank()
                }
                tvTransactionStatus.apply {
                    text = it.transactionStatus
                    setCompoundDrawablesRelativeWithIntrinsicBounds(ContextCompat.getDrawable(requireContext(), it.transactionStatusStartDrawableId), null, null, null)
                }
                groupMemo.isVisible = it.memo.isNullOrEmpty().not()
                tvMemo.text = it.memo
                tvTime.text = it.timestamp
                tvNetwork.text = it.network.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                tvBlockId.text = it.blockId
                tvConfirmation.text = it.confirmation
                tvTransactionId.text = it.transactionId
                tvRecipient.text = it.recipientAddressType
                tvAddress.text = it.toAddress
                tvSubTotal.text = it.subTotal
                groupFee.isVisible = it.networkFees.isNullOrBlank().not()
                tvFee.text = it.networkFees
                tvTotalAmount.text = it.totalAmount

                val exploreOnClick = View.OnClickListener {
                    uiModel.transactionId?.let { txId ->
                        mainActivity?.showFirstUseWarning(
                            Const.Default.FIRST_USE_VIEW_TX,
                            titleResId = R.string.dialog_first_use_view_tx_title,
                            msgResId = R.string.dialog_first_use_view_tx_message,
                            positiveResId = R.string.dialog_first_use_view_tx_positive,
                            negativeResId = R.string.dialog_first_use_view_tx_negative
                        ) {
                            onLaunchUrl(txId.toTransactionUrl())
                        }
                    }
                }
                btnViewOnExplorer.setOnClickListener(exploreOnClick)
            }
        }
    }

    private fun String.toTransactionUrl(): String {
        return getString(R.string.api_block_explorer, this)
    }

    private fun copyText(text: String, label: String) {
        if (text.isBlank()) return
        mainActivity?.copyText(textToCopy = text, label)
    }
}
