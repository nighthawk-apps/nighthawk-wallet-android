package com.nighthawkapps.wallet.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentFundsAvailableBinding
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class FundsAvailableFragment : BaseFragment<FragmentFundsAvailableBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentFundsAvailableBinding =
        FragmentFundsAvailableBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonAction.setOnClickListener {
            onProceedWithAutoshielding()
        }
    }

    /**
     * This function probably serves no purpose other than to click through to the next screen
     */
    private fun onProceedWithAutoshielding() {
        mainActivity?.let { main ->
            main.authenticate(
                "Shield transparent funds",
                getString(R.string.biometric_backup_phrase_title)
            ) {
                main.safeNavigate(R.id.action_nav_funds_available_to_nav_shield_final)
            }
        }
    }
}
