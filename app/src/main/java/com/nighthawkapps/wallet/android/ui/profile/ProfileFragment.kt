package com.nighthawkapps.wallet.android.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.BuildConfig
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentProfileBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.ext.showConfirmation
import com.nighthawkapps.wallet.android.ext.showRescanWalletDialog
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
        binding.buttonSideshift.setOnClickListener {
            mainActivity?.copyAddress()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.fund_wallet_sideshift_title))
                .setMessage(getString(R.string.fund_wallet_sideshift_description))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.open_browser)) { dialog, _ ->
                    mainActivity?.onLaunchUrl(getString(R.string.sideshift_affiliate_link))
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        binding.buttonFeedback.setOnClickListener {
            val url = "https://twitter.com/nighthawkwallet"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        binding.buttonBackup.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("View Seed Words?")
                .setMessage("WARNING: Please make sure that you are the only one viewing your phone as your wallet seed key will be shown in the next screen.")
                .setCancelable(false)
                .setPositiveButton("View Seed") { dialog, _ ->
                    dialog.dismiss()
                    mainActivity?.let { main ->
                        main.authenticate(
                            getString(R.string.biometric_backup_phrase_description),
                            getString(R.string.biometric_backup_phrase_title)
                        ) {
                            main.safeNavigate(R.id.action_nav_profile_to_nav_backup)
                        }
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        binding.buttonRescan.setOnClickListener {
            onRescanWallet()
        }

        binding.textVersion.text = BuildConfig.VERSION_NAME
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            binding.textAddress.text = viewModel.getAddress().toAbbreviatedAddress(12, 12)
        }
    }

    private fun onRescanWallet() {
        val distance = viewModel.scanDistance()
        mainActivity?.showRescanWalletDialog(
            String.format("%,d", distance),
            viewModel.blocksToMinutesString(distance),
            onFullRescan = ::onFullRescan,
            onWipe = ::onWipe
        )
    }

    private fun onFullRescan() {
        mainActivity?.lifecycleScope?.launch {
            try {
                viewModel.fullRescan()
                Toast.makeText(
                    NighthawkWalletApp.instance,
                    "Performing full rescan!",
                    Toast.LENGTH_LONG
                ).show()
                mainActivity?.navController?.popBackStack()
            } catch (t: Throwable) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.dialog_rescan_failed_title)
                    .setMessage("Unable to perform full rescan due to error:\n\n${t.message}")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { d, _ ->
                        d.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun onWipe() {
        mainActivity?.showConfirmation(
            getString(R.string.profile_wipe_wallet_title),
            getString(R.string.profile_wipe_wallet_text),
            getString(R.string.profile_wipe_wallet_wipe)
        ) {
            viewModel.wipe()
            mainActivity?.finish()
        }
    }
}
