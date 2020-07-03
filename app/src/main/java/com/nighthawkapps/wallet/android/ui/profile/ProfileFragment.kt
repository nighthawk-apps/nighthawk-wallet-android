package com.nighthawkapps.wallet.android.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import com.nighthawkapps.wallet.android.BuildConfig
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentProfileBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Tap.PROFILE_BACKUP
import com.nighthawkapps.wallet.android.feedback.Report.Tap.PROFILE_CLOSE
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    override val screen = Report.Screen.PROFILE

    private val viewModel: ProfileViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack() { tapped(PROFILE_CLOSE) }
        binding.buttonBackup.onClickNavTo(R.id.action_nav_profile_to_nav_backup) {
            tapped(
                PROFILE_BACKUP
            )
        }
        binding.buttonFeedback.setOnClickListener(View.OnClickListener {
            val url = "https://twitter.com/nighthawkwallet"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        })

        binding.textVersion.text = BuildConfig.VERSION_NAME
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            binding.textAddress.text = viewModel.getAddress().toAbbreviatedAddress(12, 12)
        }
    }
}