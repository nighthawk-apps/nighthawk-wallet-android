package com.nighthawkapps.wallet.android.ui.receive

import android.content.Context
import android.view.LayoutInflater
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentTabReceiveShieldedBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.qrecycler.QRecycler
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class ReceiveTabFragment : BaseFragment<FragmentTabReceiveShieldedBinding>() {

        private val viewModel: ReceiveViewModel by viewModel()

        lateinit var qrecycler: QRecycler

        override fun inflate(inflater: LayoutInflater): FragmentTabReceiveShieldedBinding =
            FragmentTabReceiveShieldedBinding.inflate(inflater)

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
                .into(binding.viewContent.receiveQrCode)

            binding.viewContent.tvAddress.text = address
            binding.viewContent.tvTitle.text = getString(R.string.ns_shielded_address)
            binding.viewContent.btnCopy.setOnClickListener { mainActivity?.copyAddress(label = getString(R.string.ns_shielded_address)) }
        }
    }
