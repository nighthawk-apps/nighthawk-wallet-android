package com.nighthawkapps.wallet.android.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentExternalServicesBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class ExternalServicesFragment : BaseFragment<FragmentExternalServicesBinding>() {

    private val viewModel: ExternalServicesViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentExternalServicesBinding {
        return FragmentExternalServicesBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
        binding.apply {
            switchEnableDisableUns.isChecked = viewModel.isUnsEnabled()
            switchEnableDisableUns.setOnCheckedChangeListener { _, isChecked ->
                onUnsSwitchChanged(isChecked)
            }
        }
    }

    private fun onUnsSwitchChanged(checked: Boolean) {
        if (checked) {
            viewModel.setUnsEnableStatus(true)
            mainActivity?.showMessage(getString(R.string.settings_toast_face_uns_enabled))
        } else {
            viewModel.setUnsEnableStatus(false)
            mainActivity?.showMessage(getString(R.string.settings_toast_face_uns_disabled))
        }
    }
}
