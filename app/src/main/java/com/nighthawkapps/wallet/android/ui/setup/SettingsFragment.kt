package com.nighthawkapps.wallet.android.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.BuildConfig
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSettingsBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.showConfirmation
import com.nighthawkapps.wallet.android.ext.showRescanWalletDialog
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        with(binding) {
            viewSyncNotification.updateTransferItemsData(
                R.drawable.ic_icon_bell,
                getString(R.string.ns_sync_notifications),
                getString(R.string.ns_sync_notifications_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_sync)
            }

            viewFiatCurrency.updateTransferItemsData(
                R.drawable.ic_icon_fiat_currency,
                getString(R.string.ns_fiat_currency),
                getString(R.string.ns_fiat_currency_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_fiat_currency)
            }

            viewSecurity.updateTransferItemsData(
                R.drawable.ic_icon_security,
                getString(R.string.ns_security),
                getString(R.string.ns_security_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_security)
            }

            viewBackUpWallet.updateTransferItemsData(
                R.drawable.ic_icon_backup,
                getString(R.string.ns_backup_wallet),
                getString(R.string.ns_backup_wallet_text)
            ) {
                onBackUpSeedClicked()
            }

            viewRescan.updateTransferItemsData(
                R.drawable.ic_icon_rescan_wallet,
                getString(R.string.ns_rescan_wallet),
                getString(R.string.ns_rescan_wallet_text)
            ) {
                onRescanWallet()
            }

            viewUpdateServer.updateTransferItemsData(
                R.drawable.ic_icon_change_server,
                getString(R.string.ns_change_server),
                getString(R.string.ns_change_server_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_update_server)
            }

            viewExternalServices.updateTransferItemsData(
                R.drawable.ic_icon_external_services,
                getString(R.string.ns_external_services),
                getString(R.string.ns_external_services_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_external_services)
            }

            viewAbout.updateTransferItemsData(
                R.drawable.ic_icon_about,
                getString(R.string.ns_about),
                getString(R.string.ns_about_text, BuildConfig.VERSION_NAME)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_about)
            }
        }
    }

    private fun onBackUpSeedClicked() {
        showMaterialDialog(
            getString(R.string.ns_back_up_seed_dialog_title),
            getString(R.string.ns_back_up_seed_dialog_body),
            getString(R.string.ns_back_up_seed_dialog_positive),
            getString(R.string.ns_cancel),
            false,
            {
                mainActivity?.let { main ->
                    main.authenticate(
                        getString(R.string.biometric_backup_phrase_description),
                        getString(R.string.biometric_backup_phrase_title)
                    ) {
                        main.safeNavigate(SettingsFragmentDirections.actionNavSettingsToNavBackup(showExportPdf = true))
                    }
                }
            }
        )
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
                    getString(R.string.dialog_rescan_initiated_title),
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
            viewModel.wipe(context)
            mainActivity?.finish()
        }
    }

    private fun ViewBinding.updateTransferItemsData(@DrawableRes icon: Int, title: String, subTitle: String, iconRotationAngle: Float = 0f, onRootClick: () -> Unit) {
        with(this.root) {
            findViewById<AppCompatImageView>(R.id.ivLeftIcon).apply {
                setImageResource(icon)
                if (iconRotationAngle != 0F) {
                    rotation = iconRotationAngle
                }
            }
            findViewById<TextView>(R.id.tvItemTitle).text = title
            findViewById<TextView>(R.id.tvItemSubTitle).text = subTitle
            setOnClickListener { onRootClick.invoke() }
        }
    }
}
