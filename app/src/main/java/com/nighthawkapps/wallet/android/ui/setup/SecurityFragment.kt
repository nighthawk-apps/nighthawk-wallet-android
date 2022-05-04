package com.nighthawkapps.wallet.android.ui.setup

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSecurityBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class SecurityFragment : BaseFragment<FragmentSecurityBinding>() {

    private val passwordViewModel: PasswordViewModel by activityViewModel()
    private var dialog: Dialog? = null

    override fun inflate(inflater: LayoutInflater): FragmentSecurityBinding {
        return FragmentSecurityBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
        binding.apply {
            textSetChangePinCode.setOnClickListener { onSetChangePassWordSelected() }
            switchEnableDisableBiometric.isChecked = passwordViewModel.isBioMetricOrFaceIdEnabled()
            switchEnableDisableBiometric.setOnCheckedChangeListener { _, isChecked ->
                onBioMetricSwitchedChanged(isChecked)
            }
            btnDisablePin.setOnClickListener { onDisablePinClicked() }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btnDisablePin.isVisible = passwordViewModel.isPinCodeEnabled()
    }

    private fun onDisablePinClicked() {
        mainActivity?.showMessage(getString(R.string.ns_disable_pin_message))
        passwordViewModel.savePassword("")
        if (binding.switchEnableDisableBiometric.isChecked) {
            binding.switchEnableDisableBiometric.isChecked = false
        }
        binding.btnDisablePin.isVisible = false
    }

    private fun onSetChangePassWordSelected() {
        val action = SecurityFragmentDirections.actionNavSecurityToEnterPinFragment(forNewPinSetup = true)
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
