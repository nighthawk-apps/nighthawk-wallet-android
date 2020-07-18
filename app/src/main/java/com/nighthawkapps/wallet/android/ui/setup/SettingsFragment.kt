package com.nighthawkapps.wallet.android.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.databinding.FragmentSettingsBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
        binding.buttonUpdate.setOnClickListener(View.OnClickListener {
            MaterialAlertDialogBuilder(view.context)
                .setTitle("Modify Lightwalletd Server?")
                .setMessage("WARNING: Make sure that you are the only one viewing your phone as your wallet seed key will be shown in the next screen.")
                .setCancelable(false)
                .setPositiveButton("Update") { dialog, _ ->
                    try {
                        viewModel.restartSynchronizer()
                    } finally {
                        dialog.dismiss()
                        val restartIntent: Intent? = requireContext().getPackageManager().getLaunchIntentForPackage(requireContext().getPackageName())
                        restartIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(restartIntent)
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        })
        binding.buttonReset.setOnClickListener(View.OnClickListener {
            binding.inputTextLightwalletdServer.setText("")
            binding.inputTextLightwalletdPort.setText("")
        })
    }
}
