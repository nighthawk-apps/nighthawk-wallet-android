package com.nighthawkapps.wallet.android.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentAboutBinding
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.toClickableSpan
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class AboutFragment : BaseFragment<FragmentAboutBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentAboutBinding {
        return FragmentAboutBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
        binding.tvSource.toClickableSpan(getString(R.string.ns_view_source)) {
            mainActivity?.onLaunchUrl(getString(R.string.ns_source_code_link))
        }
        binding.tvTerms.toClickableSpan(getString(R.string.ns_terms_conditions)) {
            mainActivity?.onLaunchUrl(getString(R.string.ns_privacy_policy_link))
        }
        binding.btnViewLicences.setOnClickListener {
            startActivity(Intent(context, LicenseActivity::class.java))
        }
    }
}
