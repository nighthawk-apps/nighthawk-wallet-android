package com.nighthawkapps.wallet.android.ui.transfer

import android.view.LayoutInflater
import com.nighthawkapps.wallet.android.databinding.FragmentTransferBinding
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class TransferFragment : BaseFragment<FragmentTransferBinding>() {
    override fun inflate(inflater: LayoutInflater): FragmentTransferBinding {
        return FragmentTransferBinding.inflate(layoutInflater)
    }
}
