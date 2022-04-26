package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.databinding.FragmentSendEnterAddressBinding
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class SendEnterAddressFragment : BaseFragment<FragmentSendEnterAddressBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentSendEnterAddressBinding {
        return FragmentSendEnterAddressBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack()
    }
}
