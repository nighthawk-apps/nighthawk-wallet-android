package com.nighthawkapps.wallet.android.ui.topup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewbinding.ViewBinding
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentTopupBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.transfer.TransferViewModel
import com.nighthawkapps.wallet.android.ui.util.Utils

class TopUpFragment : BaseFragment<FragmentTopupBinding>() {

    private val transferViewModel: TransferViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentTopupBinding {
        return FragmentTopupBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
        binding.hitAreaExit.onClickNavBack()
    }

    private fun updateUI() {
        with(binding) {

            updateTitle(getString(R.string.ns_top_up))

            viewMoonPay.updateTransferItemsData(R.drawable.ic_icon_moon_pay, getString(R.string.ns_buy_moonpay), getString(R.string.ns_buy_moonpay_text)) { onMoonPayClicked() }

            viewSideShift.updateTransferItemsData(R.drawable.ic_icon_side_shift, getString(R.string.ns_swap_sideshift), getString(R.string.ns_swap_sideshift_text)) { onSideShiftClicked() }

            viewStealthEx.updateTransferItemsData(R.drawable.ic_icon_side_shift, getString(R.string.ns_swap_stealthex), getString(R.string.ns_swap_stealthex_text)) { onStealthExClicked() }
        }
    }

    private fun onMoonPayClicked() {
        mainActivity?.copyTransparentAddress()
        showMaterialDialog(
            getString(R.string.buy_zec_dialog_title),
            getString(R.string.buy_zec_dialog_msg),
            getString(R.string.open_browser),
            getString(R.string.cancel),
            onPositiveClick = {
                Utils.openCustomTab(requireActivity(), Utils.createCustomTabIntent(), Uri.parse(transferViewModel.getMoonPayUrl()))
            })
    }

    private fun onSideShiftClicked() {
        mainActivity?.copyAddress()
        showMaterialDialog(
            getString(R.string.fund_wallet_sideshift_title),
            getString(R.string.fund_wallet_sideshift_description),
            getString(R.string.open_browser),
            getString(R.string.cancel),
            onPositiveClick = {
                mainActivity?.onLaunchUrl(getString(R.string.sideshift_affiliate_link))
            })
    }

    private fun onStealthExClicked() {
        mainActivity?.copyTransparentAddress()
        showMaterialDialog(
            getString(R.string.fund_wallet_stealthex_title),
            getString(R.string.fund_wallet_stealthex_description),
            getString(R.string.open_browser),
            getString(R.string.cancel),
            onPositiveClick = {
                mainActivity?.onLaunchUrl(getString(R.string.stealthex_affiliate_link))
            })
    }

    private fun updateTitle(title: String) {
        binding.tvTitle.text = title
    }

    private fun ViewBinding.updateTransferItemsData(
        @DrawableRes icon: Int,
        title: String,
        subTitle: String,
        iconRotationAngle: Float = 0f,
        onRootClick: () -> Unit
    ) {
        with(this.root) {
            findViewById<AppCompatImageView>(R.id.ivLeftIcon).apply {
                setImageResource(icon)
                if (iconRotationAngle != 0F) {
                    rotation = iconRotationAngle
                }
            }
            findViewById<TextView>(R.id.tvItemTitle).text = title
            findViewById<TextView>(R.id.tvItemSubTitle).text = subTitle
            setOnClickListener { onRootClick.invoke() }
        }
    }
}
