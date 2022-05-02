package com.nighthawkapps.wallet.android.ui.setup

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentFiatCurrencyBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class FiatCurrencyFragment : BaseFragment<FragmentFiatCurrencyBinding>() {

    private val fiatCurrencyViewModel: FiatCurrencyViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentFiatCurrencyBinding {
        return FragmentFiatCurrencyBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
        addCurrencyOptions()
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
                    }
                }
            }
            binding.radioGroupCurrencyTypes.addView(radioButton)
        }
    }
}
