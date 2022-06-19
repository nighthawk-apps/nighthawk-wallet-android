package com.nighthawkapps.wallet.android.ui.setup

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentFiatCurrencyBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.home.HomeViewModel

class FiatCurrencyFragment : BaseFragment<FragmentFiatCurrencyBinding>() {

    private val fiatCurrencyViewModel: FiatCurrencyViewModel by viewModel()
    private val homeViewModel: HomeViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentFiatCurrencyBinding {
        return FragmentFiatCurrencyBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
        addCurrencyOptions()
        addUnitOptions()
    }

    private fun addCurrencyOptions() {
        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.radio_button_padding_horizontal)
        val paddingVertical = resources.getDimensionPixelSize(R.dimen.radio_button_padding_vertical)
        FiatCurrencyViewModel.FiatCurrency.values().forEachIndexed { index, fiatCurrencies ->
            val radioButton = RadioButton(requireContext()).also {
                it.id = index
                it.isChecked = fiatCurrencies == fiatCurrencyViewModel.localCurrencySelected()
                it.text = fiatCurrencies.currencyText
                it.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                it.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.switch_tracker_color
                    )
                )
                it.setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        twig("Selected local currency is $fiatCurrencies")
                        fiatCurrencyViewModel.updateLocalCurrency(fiatCurrencies)
                        homeViewModel.getZecMarketPrice(fiatCurrencies.serverUrl)
                    }
                }
            }
            binding.radioGroupCurrencyTypes.addView(radioButton)
        }
    }

    private fun addUnitOptions() {
        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.radio_button_padding_horizontal)
        val paddingVertical = resources.getDimensionPixelSize(R.dimen.radio_button_padding_vertical)
        FiatCurrencyViewModel.FiatUnit.values().forEachIndexed { index, fiatUnit ->
            val linearLayout = LinearLayoutCompat(requireContext()).also {
                it.orientation = LinearLayoutCompat.HORIZONTAL
                it.layoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT)
                it.weightSum = 1F
            }
            val radioButton = RadioButton(requireContext()).also {
                it.id = index
                it.isChecked = fiatUnit == fiatCurrencyViewModel.localUnitSelected()
                it.text = fiatUnit.currencyName
                it.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                it.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.switch_tracker_color
                    )
                )
                it.setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        twig("Selected local unit is $fiatUnit")
                        fiatCurrencyViewModel.updateLocalUnit(fiatUnit)
                    }
                }
                it.layoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT, 0.35F)
            }
            val conversionTv = TextView(requireContext()).also {
                val text = "${fiatUnit.zatoshiPerUnit} zat"
                it.text = text
                it.gravity = Gravity.END
                it.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
                it.ellipsize = TextUtils.TruncateAt.END
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.ns_white))
                it.layoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT, 0.65F)
            }

            linearLayout.addView(radioButton)
            linearLayout.addView(conversionTv)

            binding.radioGroupUnitTypes.addView(linearLayout)
        }
    }
}
