package com.nighthawkapps.wallet.android.ui.setup

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSettingsBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.gone
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.showUpdateServerCriticalError
import com.nighthawkapps.wallet.android.ext.showUpdateServerDialog
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ext.visible
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity?.preventBackPress(this)
        viewModel.init()
        binding.apply {
            groupLoading.gone()
            hitAreaClose.onClickNavBack()
            buttonReset.setOnClickListener(::onResetClicked)
            buttonUpdate.setOnClickListener(::onUpdateClicked)
            buttonUpdate.isActivated = true
            buttonReset.isActivated = true
            inputTextLightwalletdServer.doAfterTextChanged {
                viewModel.pendingHost = it.toString()
            }
            inputTextLightwalletdPort.doAfterTextChanged {
                viewModel.pendingPortText = it.toString()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.uiModels.collectWith(resumedScope, ::onUiModelUpdated)
    }

    private fun onResetClicked(unused: View?) {
        mainActivity?.hideKeyboard()
        context?.showUpdateServerDialog(R.string.settings_buttons_restore) {
            resumedScope.launch {
                binding.groupLoading.visible()
                binding.loadingView.requestFocus()
                viewModel.resetServer()
            }
        }
    }

    private fun onUpdateClicked(unused: View?) {
        mainActivity?.hideKeyboard()
        context?.showUpdateServerDialog {
            resumedScope.launch {
                binding.groupLoading.visible()
                binding.loadingView.requestFocus()
                viewModel.submit()
            }
        }
    }

    private fun onUiModelUpdated(uiModel: SettingsViewModel.UiModel) {
        twig("onUiModelUpdated:::::$uiModel")
        binding.apply {
            if (handleCompletion(uiModel)) return@onUiModelUpdated

            // avoid moving the cursor on instances where the change originated from the UI
            if (inputTextLightwalletdServer.text.toString() != uiModel.host) inputTextLightwalletdServer.setText(
                uiModel.host
            )
            if (inputTextLightwalletdPort.text.toString() != uiModel.portText) inputTextLightwalletdPort.setText(
                uiModel.portText
            )

            buttonReset.isEnabled = uiModel.submitEnabled
            buttonUpdate.isEnabled = uiModel.submitEnabled && !uiModel.hasError

            uiModel.hostErrorMessage.let { it ->
                lightwalletdServer.helperText = it
                    ?: R.string.settings_host_helper_text.toAppString()
                lightwalletdServer.setHelperTextColor(it.toHelperTextColor())
            }
            uiModel.portErrorMessage.let { it ->
                lightwalletdPort.helperText = it
                    ?: R.string.settings_port_helper_text.toAppString()
                lightwalletdPort.setHelperTextColor(it.toHelperTextColor())
            }
        }
    }

    /**
     * Handle the exit conditions and return true if we're done here.
     */
    private fun handleCompletion(uiModel: SettingsViewModel.UiModel): Boolean {
        return if (uiModel.changeError != null) {
            binding.groupLoading.gone()
            onCriticalError(uiModel.changeError)
            true
        } else {
            if (uiModel.complete) {
                binding.groupLoading.gone()
                mainActivity?.safeNavigate(R.id.nav_home)
                Toast.makeText(NighthawkWalletApp.instance, getString(R.string.settings_toast_change_server_success), Toast.LENGTH_SHORT).show()
                true
            }
            false
        }
    }

    private fun onCriticalError(error: Throwable) {
        val details = if (error is LightWalletException.ChangeServerException.StatusException) {
            error.status.description
        } else {
            error.javaClass.simpleName
        }
        val message = "An error occured while changing servers. Please verify the info" +
                " and try again.\n\nError: $details"
        twig(message)
        Toast.makeText(NighthawkWalletApp.instance, getString(R.string.settings_toast_change_server_failure), Toast.LENGTH_SHORT).show()
        context?.showUpdateServerCriticalError(message)
    }

    //
    // Utilities
    //

    private fun String?.toHelperTextColor(): ColorStateList {
        val color = if (this == null) {
            R.color.text_light_dimmed
        } else {
            R.color.zcashRed
        }
        return ColorStateList.valueOf(color.toAppColor())
    }
}
