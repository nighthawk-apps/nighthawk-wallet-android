package com.nighthawkapps.wallet.android.ui.setup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentLandingBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Funnel.Restore
import com.nighthawkapps.wallet.android.feedback.Report.Tap.*
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITHOUT_BACKUP
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITH_BACKUP
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LandingFragment : BaseFragment<FragmentLandingBinding>() {
    override val screen = Report.Screen.LANDING

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)

    private var skipCount: Int = 0

    override fun inflate(inflater: LayoutInflater): FragmentLandingBinding =
        FragmentLandingBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonPositive.setOnClickListener {
            when (binding.buttonPositive.text.toString().toLowerCase()) {
                "new" -> onNewWallet().also { tapped(LANDING_NEW) }
                "backup" -> onBackupWallet().also { tapped(LANDING_BACKUP) }
            }
        }
        binding.buttonNegative.setOnClickListener {
            when (binding.buttonNegative.text.toString().toLowerCase()) {
                "restore" -> onRestoreWallet().also {
                    mainActivity?.reportFunnel(Restore.Initiated)
                    tapped(LANDING_RESTORE)
                }
                else -> onSkip(++skipCount)
            }
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)

        walletSetup.checkSeed().onEach {
            when(it) {
                SEED_WITHOUT_BACKUP, SEED_WITH_BACKUP -> {
                    mainActivity?.safeNavigate(R.id.nav_backup)
                }
            }
        }.launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        mainActivity?.hideKeyboard()
    }

    private fun onSkip(count: Int) {
        when (count) {
            1 -> {
                tapped(LANDING_BACKUP_SKIPPED_1)
                binding.textMessage.text =
                    "Are you sure? Without a backup, funds can be lost FOREVER!"
                binding.buttonNegative.text = "Later"
            }
            2 -> {
                tapped(LANDING_BACKUP_SKIPPED_2)
                binding.textMessage.text =
                    "You can't backup later. You're probably going to lose your funds!"
                binding.buttonNegative.text = "I've been warned"
            }
            else -> {
                tapped(LANDING_BACKUP_SKIPPED_3)
                onEnterWallet()
            }
        }
    }

    private fun onRestoreWallet() {
        mainActivity?.safeNavigate(R.id.action_nav_landing_to_nav_restore)
    }

    private fun onNewWallet() {
        lifecycleScope.launch {
            val ogText = binding.buttonPositive.text
            binding.buttonPositive.text = "creating"
            binding.buttonPositive.isEnabled = false

            mainActivity?.startSync(walletSetup.newWallet())

            binding.buttonPositive.isEnabled = true
            binding.textMessage.text = "Wallet created! Congratulations!"
            binding.buttonNegative.text = "Skip"
            binding.buttonPositive.text = "Backup"
            mainActivity?.playSound("sound_receive_small.mp3")
            mainActivity?.vibrateSuccess()
        }
    }

    private fun onBackupWallet() {
        skipCount = 0
        mainActivity?.safeNavigate(R.id.action_nav_landing_to_nav_backup)
    }

    private fun onEnterWallet() {
        skipCount = 0
        mainActivity?.navController?.popBackStack()
    }
}