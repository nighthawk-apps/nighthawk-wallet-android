package com.nighthawkapps.wallet.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.net.toUri
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import cash.z.ecc.android.sdk.model.Zatoshi
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentTransferBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.send.SendViewModel
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil

class TransferFragment : BaseFragment<FragmentTransferBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()
    private val args: TransferFragmentArgs by navArgs()

    override fun inflate(inflater: LayoutInflater): FragmentTransferBinding {
        return FragmentTransferBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args.forBuyZecDeeplink.isNotBlank()) {
            val data = DeepLinkUtil.getSendDeepLinkData(uri = args.forBuyZecDeeplink.toUri())
            data?.let {
                sendViewModel.reset()
                sendViewModel.toAddress = data.address
                sendViewModel.memo = data.memo ?: ""
                data.amount?.let {
                    sendViewModel.zatoshiAmount = Zatoshi(it)
                }
                sendViewModel.setSendZecDeepLinkData(data)
                mainActivity?.safeNavigate(R.id.action_nav_transfer_to_nav_send_review)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        with(binding) {
            updateTitle(getString(R.string.ns_send_and_receive_zcash))

            viewSendMoney.updateTransferItemsData(
                R.drawable.ic_arrow_back_black_24dp,
                getString(R.string.ns_send_money),
                getString(R.string.ns_send_money_text),
                180f
            ) {
                sendViewModel.reset()
                mainActivity?.safeNavigate(R.id.action_nav_transfer_to_nav_send_enter_amount)
            }

            viewReceiveMoney.updateTransferItemsData(
                R.drawable.ic_arrow_back_black_24dp,
                getString(R.string.ns_receive_money),
                getString(R.string.ns_receive_money_publicly_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_transfer_to_nav_receive_transfer)
            }

            viewTopUpWallet.updateTransferItemsData(
                R.drawable.ic_icon_top_up,
                getString(R.string.ns_top_up_wallet),
                getString(R.string.ns_top_up_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_transfer_to_nav_topup)
            }
        }
    }

    private fun updateTitle(title: String) {
        binding.tvTitle.text = title
    }

    private fun ViewBinding.updateTransferItemsData(@DrawableRes icon: Int, title: String, subTitle: String, iconRotationAngle: Float = 0f, onRootClick: () -> Unit) {
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
