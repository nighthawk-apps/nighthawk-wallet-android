package com.nighthawkapps.wallet.android.ui.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewbinding.ViewBinding
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentReceiveBinding
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class ReceiveFragment : BaseFragment<FragmentReceiveBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentReceiveBinding {
        return FragmentReceiveBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
        binding.hitAreaExit.onClickNavBack()
    }

    private fun updateUI() {
        with(binding) {

            updateTitle(getString(R.string.ns_receive_money_securely))

            viewShowQRCode.updateTransferItemsData(
                R.drawable.ic_icon_scan_qr, getString(R.string.ns_show_qr_code), getString(
                    R.string.ns_show_qr_code_text
                )
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_receive_transfer_to_nav_receive)
            }

            viewCopyPrivateAddress.updateTransferItemsData(
                R.drawable.ic_content_copy, getString(R.string.ns_copy_private_address), getString(
                    R.string.ns_copy_private_address_text
                )
            ) {
                mainActivity?.copyAddress(label = getString(R.string.ns_private_address))
            }

            viewTopUpWalletReceive.updateTransferItemsData(
                R.drawable.ic_icon_top_up, getString(R.string.ns_top_up_wallet), getString(
                    R.string.ns_top_up_text
                )
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_receive_to_nav_topup)
            }

            viewCopyNonPrivateAddress.updateTransferItemsData(
                R.drawable.ic_icon_transparent, getString(
                    R.string.ns_copy_public_address
                ), getString(R.string.ns_receive_money_publicly_text)
            ) {
                mainActivity?.copyTransparentAddress(label = getString(R.string.ns_non_private_address))
            }
        }
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
