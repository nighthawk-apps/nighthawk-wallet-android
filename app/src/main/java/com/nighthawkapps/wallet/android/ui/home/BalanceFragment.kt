package com.nighthawkapps.wallet.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentBalanceBinding
import com.nighthawkapps.wallet.android.ext.toColoredSpan
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.setup.FiatCurrencyViewModel
import com.nighthawkapps.wallet.android.ui.util.Utils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BalanceFragment : BaseFragment<FragmentBalanceBinding>() {

    private lateinit var sectionType: SectionType
    private val balanceViewModel: BalanceViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels({ requireParentFragment() })

    override fun inflate(inflater: LayoutInflater): FragmentBalanceBinding {
        return FragmentBalanceBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sectionType = SectionType.getSectionTypeFromInt(arguments?.getInt(ARG_SECTION_TYPE) ?: SectionType.SWIPE_LEFT_DIRECTION.sectionNo)
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.uiModels
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collect { homeUiModel ->
                    updateUI(balanceViewModel.getBalanceUIModel(sectionType, homeUiModel))
                }
        }
        binding.tvBalance.setOnClickListener { changeBalanceMode() }
    }

    private fun updateUI(balanceUIModel: BalanceViewModel.BalanceUIModel) {
        binding.icon.setImageDrawable(balanceUIModel.icon)
        binding.tvBalance.apply {
            visibility = if (balanceUIModel.balanceAmount.isEmpty().not()) View.VISIBLE else View.GONE
            updateBalanceText()
        }
        binding.tvExpectingBalance.apply {
            visibility = if (balanceUIModel.expectingBalance.isEmpty().not()) View.VISIBLE else View.GONE
            text = balanceUIModel.expectingBalance
        }
        binding.tvMessage.text = balanceUIModel.messageText
    }

    private fun changeBalanceMode() {
        balanceViewModel.isZecAmountState = !balanceViewModel.isZecAmountState
        updateBalanceText()
    }

    private fun updateBalanceText() {
        if (balanceViewModel.isZecAmountState) {
            binding.tvBalance.text = getString(R.string.ns_zec_amount, balanceViewModel.balanceAmountZec).toColoredSpan(R.color.ns_peach_100, "ZEC")
        } else {
            val convertedAmount = Utils.getZecConvertedAmountText(balanceViewModel.balanceAmountZec, homeViewModel.coinMetricsMarketData.value)
            binding.tvBalance.text = convertedAmount?.toColoredSpan(R.color.ns_peach_100,
                FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByMarket(homeViewModel.coinMetricsMarketData.value?.market ?: "").currencyName)
        }
    }

    companion object {
        private const val ARG_SECTION_TYPE = "section_type"

        fun newInstance(sectionNo: Int): BalanceFragment {
            return BalanceFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_TYPE, sectionNo)
                }
            }
        }

        enum class SectionType(val sectionNo: Int) {
            SWIPE_LEFT_DIRECTION(0),
            TOTAL_BALANCE(1),
            SHIELDED_BALANCE(2),
            TRANSPARENT_BALANCE(3);

            companion object {
                fun getSectionTypeFromInt(sectionNo: Int): SectionType {
                    for (section in values()) {
                        if (section.sectionNo == sectionNo) {
                            return section
                        }
                    }
                    return SWIPE_LEFT_DIRECTION
                }
            }
        }
    }
}
