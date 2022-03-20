package com.nighthawkapps.wallet.android.ui.setup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentLandingBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.locale
import com.nighthawkapps.wallet.android.ext.showSharedLibraryCriticalError
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ext.toClickableSpan
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITHOUT_BACKUP
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITH_BACKUP
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LandingFragment : BaseFragment<FragmentLandingBinding>() {

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)

    private var skipCount: Int = 0

    override fun inflate(inflater: LayoutInflater): FragmentLandingBinding =
        FragmentLandingBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonPositive.setOnClickListener {
            when (binding.buttonPositive.text.toString().lowercase(locale())) {
                R.string.ns_create_wallet.toAppString(true) -> onNewWallet()
                R.string.ns_backup_wallet.toAppString(true) -> onBackupWallet()
            }
        }
        binding.buttonNegative.setOnClickListener {
            when (binding.buttonNegative.text.toString().lowercase(locale())) {
                R.string.ns_restore_from_backup.toAppString(true) -> onRestoreWallet()
                else -> onSkip(++skipCount)
            }
        }
        binding.termsMessage.toClickableSpan(getString(R.string.ns_terms_conditions)) {
            mainActivity?.onLaunchUrl(getString(R.string.ns_privacy_policy_link))
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)

        walletSetup.checkSeed().onEach {
            when (it) {
                SEED_WITHOUT_BACKUP, SEED_WITH_BACKUP -> {
                    mainActivity?.safeNavigate(R.id.nav_backup)
                }
                else -> {}
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
                binding.textMessage.setText(R.string.landing_backup_skipped_message_1)
                binding.buttonNegative.setText(R.string.landing_button_backup_skipped_1)
            }
            2 -> {
                binding.textMessage.setText(R.string.landing_backup_skipped_message_2)
                binding.buttonNegative.setText(R.string.landing_button_backup_skipped_2)
            }
            else -> {
                onEnterWallet()
            }
        }
    }

    private fun onRestoreWallet() {
        mainActivity?.safeNavigate(R.id.action_nav_landing_to_nav_restore)
    }

    private fun onNewWallet() {
        lifecycleScope.launch {
            binding.buttonPositive.setText(R.string.landing_button_progress_create)
            binding.buttonPositive.isEnabled = false

            try {
                val initializer = walletSetup.newWallet()
                mainActivity?.startSync(initializer)

                binding.buttonPositive.isEnabled = true
                binding.textMessage.setText(R.string.landing_create_success_message)
                binding.buttonNegative.setText(R.string.landing_button_secondary_create_success)
                binding.buttonPositive.setText(R.string.ns_backup_wallet)
                mainActivity?.playSound("sound_receive_small.mp3")
                mainActivity?.vibrateSuccess()
            } catch (e: UnsatisfiedLinkError) {
                // For developer sanity:
                // show a nice dialog, rather than a toast, when the rust didn't get compile
                // which can happen often when working from a local SDK build
                mainActivity?.showSharedLibraryCriticalError(e)
            } catch (t: Throwable) {
                binding.buttonPositive.isEnabled = true
                binding.buttonPositive.setText(R.string.ns_create_wallet)
                Toast.makeText(
                    context,
                    "Failed to create wallet. See logs for details. Try restarting the app.\n\nMessage: \n${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun onBackupWallet() {
        skipCount = 0
        mainActivity?.safeNavigate(R.id.action_nav_landing_to_nav_backup)
    }

    private fun onEnterWallet() {
        skipCount = 0
        mainActivity?.safeNavigate(R.id.action_nav_landing_to_nav_home)
    }
}
