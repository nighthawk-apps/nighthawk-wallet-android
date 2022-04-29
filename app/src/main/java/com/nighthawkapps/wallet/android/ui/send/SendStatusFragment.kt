package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.databinding.FragmentSendStatusBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class SendStatusFragment : BaseFragment<FragmentSendStatusBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendStatusBinding {
        return FragmentSendStatusBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: We can start sending process from here
        binding.hitAreaExit.onClickNavBack()
        // TODO: OnDone reset SendViewModel
        binding.tvMoreDetails.setOnClickListener {
            mainActivity?.safeNavigate(SendStatusFragmentDirections.actionNavSendStatusToNavTransactionDetails(true))
        }
    }
}
