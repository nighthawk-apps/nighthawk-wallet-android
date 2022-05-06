package com.nighthawkapps.wallet.android.ui.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentBuySellTabBinding
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class BuySellTabFragment : BaseFragment<FragmentBuySellTabBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentBuySellTabBinding {
        return FragmentBuySellTabBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSeeMore.setOnClickListener { mainActivity?.safeNavigate(R.id.action_nav_buy_sell_to_nav_top_up) }
    }
}
