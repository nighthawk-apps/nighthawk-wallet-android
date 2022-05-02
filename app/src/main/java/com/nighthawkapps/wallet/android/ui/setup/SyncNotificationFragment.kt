package com.nighthawkapps.wallet.android.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.databinding.FragmentSyncNotificationBinding
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class SyncNotificationFragment : BaseFragment<FragmentSyncNotificationBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentSyncNotificationBinding {
        return FragmentSyncNotificationBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
    }
}
