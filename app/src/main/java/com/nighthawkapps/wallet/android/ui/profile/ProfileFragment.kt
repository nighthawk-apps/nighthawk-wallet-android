package com.nighthawkapps.wallet.android.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.BuildConfig
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentProfileBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaSettings.onClickNavTo(R.id.action_nav_profile_to_nav_settings)
        binding.hitAreaClose.onClickNavBack()
        binding.buttonBackup.setOnClickListener(View.OnClickListener {
            MaterialAlertDialogBuilder(view.context)
                .setTitle("View Seed Words?")
                .setMessage("WARNING: Make sure that you are the only one viewing your phone as your wallet seed key will be shown in the next screen.")
                .setCancelable(false)
                .setPositiveButton("View Seed") { dialog, _ ->
                    mainActivity?.safeNavigate(R.id.action_nav_profile_to_nav_backup)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        })
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