package com.nighthawkapps.wallet.android.ui.receive

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentTabReceiveTransparentBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.qrecycler.QRecycler
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class TransparentTabFragment : BaseFragment<FragmentTabReceiveTransparentBinding>() {

    private val viewModel: ReceiveViewModel by viewModel()

    lateinit var qrecycler: QRecycler

    override fun inflate(inflater: LayoutInflater): FragmentTabReceiveTransparentBinding =
        FragmentTabReceiveTransparentBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewContent.iconQrLogo.setImageResource(R.drawable.selector_transparent_qr_code)
        binding.viewContent.tvTitle.text = getString(R.string.ns_transparent_address)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                onAddressLoaded(viewModel.getTranparentAddress())
            }
        }
    }

    override fun onAttach(context: Context) {
        qrecycler = QRecycler() // inject! :)
        super.onAttach(context)
    }

    private fun onAddressLoaded(address: String) {
        twig("address loaded:  $address length: ${address.length}")
        qrecycler.load(address)
            .withQuietZoneSize(3)
            .withCorrectionLevel(QRecycler.CorrectionLevel.MEDIUM)
            .into(binding.viewContent.receiveQrCode)

        binding.viewContent.tvAddress.text = address
        binding.viewContent.btnCopy.setOnClickListener { mainActivity?.copyTransparentAddress(label = getString(R.string.ns_transparent_address)) }
    }
}
