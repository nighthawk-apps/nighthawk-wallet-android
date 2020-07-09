package com.nighthawkapps.wallet.android.ui.receive

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentReceiveNewBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.qrecycler.QRecycler
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class ReceiveFragment : BaseFragment<FragmentReceiveNewBinding>() {

    private val viewModel: ReceiveViewModel by viewModel()

    lateinit var qrecycler: QRecycler

    override fun inflate(inflater: LayoutInflater): FragmentReceiveNewBinding =
        FragmentReceiveNewBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonScan.setOnClickListener {
            mainActivity?.maybeOpenScan(R.id.action_nav_receive_to_nav_scan)
        }
        binding.backButtonHitArea.onClickNavBack()
    }

    override fun onAttach(context: Context) {
        qrecycler = QRecycler() // inject! :)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            onAddressLoaded(viewModel.getAddress())
        }
    }

    private fun onAddressLoaded(address: String) {
        twig("address loaded:  $address length: ${address.length}")
        qrecycler.load(address)
            .withQuietZoneSize(3)
            .withCorrectionLevel(QRecycler.CorrectionLevel.MEDIUM)
            .into(binding.receiveQrCode)

        binding.receiveAddress.text = address.toAbbreviatedAddress(12, 12)
    }
}
