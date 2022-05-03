package com.nighthawkapps.wallet.android.ui.setup

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSettingsBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.showConfirmation
import com.nighthawkapps.wallet.android.ext.showRescanWalletDialog
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()
    private val passwordViewModel: PasswordViewModel by activityViewModel()
    private var dialog: Dialog? = null

    override fun inflate(inflater: LayoutInflater): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            textSetChangePinCode.setOnClickListener { onSetChangePassWordSelected() }
            switchEnableDisableBiometric.isChecked = passwordViewModel.isBioMetricOrFaceIdEnabled()
            switchEnableDisableBiometric.setOnCheckedChangeListener { _, isChecked ->
                onBioMetricSwitchedChanged(isChecked)
            }
        }
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

            viewBackUpWallet.updateTransferItemsData(
                R.drawable.ic_icon_backup,
                getString(R.string.ns_backup_wallet),
                getString(R.string.ns_backup_wallet_text)
            ) {
                onBackUpSeedClicked()
            }

            viewFiatCurrency.updateTransferItemsData(
                R.drawable.ic_icon_fiat_currency,
                getString(R.string.ns_fiat_currency),
                getString(R.string.ns_fiat_currency_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_fiat_currency)
            }

            viewSecurity.updateTransferItemsData(
                R.drawable.ic_icon_fiat_currency,
                getString(R.string.ns_security),
                getString(R.string.ns_security_text)
            ) {
//                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_fiat_currency)
            }

            viewRescan.updateTransferItemsData(
                R.drawable.ic_icon_fiat_currency,
                getString(R.string.ns_rescan_wallet),
                getString(R.string.ns_rescan_wallet_text)
            ) {
                onRescanWallet()
            }

            viewUpdateServer.updateTransferItemsData(
                R.drawable.ic_icon_fiat_currency,
                getString(R.string.ns_change_server),
                getString(R.string.ns_change_server_text)
            ) {
                mainActivity?.safeNavigate(R.id.action_nav_settings_to_nav_update_server)
            }

            viewAbout.updateTransferItemsData(
                R.drawable.ic_icon_fiat_currency,
                getString(R.string.ns_about),
                getString(R.string.ns_about_text)
            ) {
                OssLicensesMenuActivity.setActivityTitle("Open source licences")
                startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
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

    private fun onSetChangePassWordSelected() {
        val action = SettingsFragmentDirections.actionNavSettingsToEnterPinFragment(forNewPinSetup = true)
        mainActivity?.navController?.navigate(action)
    }

    private fun onBioMetricSwitchedChanged(checked: Boolean) {
        if (checked) {
            if (!passwordViewModel.isPinCodeEnabled()) {
                mainActivity?.showSnackbar(getString(R.string.set_pin_code_request))
                binding.switchEnableDisableBiometric.isChecked = false
                return
            }
            if (passwordViewModel.isBioMetricEnabledOnMobile()) {
                mainActivity?.showMessage(getString(R.string.settings_toast_face_touch_id_enabled))
                passwordViewModel.setBioMetricOrFaceIdEnableStatus(checked)
            } else {
                showDialogToEnableBioMetricOrFaceId()
            }
        } else {
            mainActivity?.showMessage(getString(R.string.settings_toast_face_touch_id_disabled))
            passwordViewModel.setBioMetricOrFaceIdEnableStatus(checked)
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

    private fun showDialogToEnableBioMetricOrFaceId() {
        dialog?.dismiss()
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_bio_metric_not_enabled_title))
            .setMessage(getString(R.string.dialog_bio_metric_not_enabled_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                binding.switchEnableDisableBiometric.isChecked = false
                dialog.dismiss()
            }.show()
    }
}
