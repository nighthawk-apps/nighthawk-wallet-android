package com.nighthawkapps.wallet.android.ui.setup

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentRestoreBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.showInvalidSeedPhraseError
import com.nighthawkapps.wallet.android.ext.showSharedLibraryCriticalError
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class RestoreFragment : BaseFragment<FragmentRestoreBinding>() {

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)

    private val textWatcher: TextWatcher by lazy {
        object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onSeedWordEntered(s)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        }
    }

    private fun onSeedWordEntered(editable: Editable?) {
        var enableDoneButton = false
        var seedWordBg = ContextCompat.getDrawable(requireContext(), R.drawable.background_r4_empty_et)
        if (editable?.trim()?.split(Const.SEED_WORDS_SEPERATOR)?.size == Const.NO_OF_SEED_WORDS) {
            enableDoneButton = true
            seedWordBg = ContextCompat.getDrawable(requireContext(), R.drawable.background_r4_complete_et)
        }
        binding.buttonDone.isEnabled = enableDoneButton
        binding.seedInput.background = seedWordBg
    }

    override fun inflate(inflater: LayoutInflater): FragmentRestoreBinding =
        FragmentRestoreBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.seedInput.addTextChangedListener(textWatcher)
        binding.buttonDone.setOnClickListener {
            onDone()
        }
        binding.buttonSuccess.setOnClickListener {
            onEnterWallet()
        }
        binding.inputBirthdate.doAfterTextChanged {
            var bgDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.background_r4_empty_et)
            if (it.isNullOrEmpty().not()) {
                bgDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.background_r4_complete_et)
            }
            binding.inputBirthdate.background = bgDrawable
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity?.onFragmentBackPressed(this) {
            if (binding.seedInput.text.isNullOrBlank()) {
                onExit()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Are you sure? For security, the words that you have entered will be cleared!")
                    .setTitle("Abort?")
                    .setPositiveButton("Stay") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton("Exit") { dialog, _ ->
                        dialog.dismiss()
                        onExit()
                    }
                    .show()
            }
        }
    }

    private fun onExit() {
        mainActivity?.hideKeyboard()
        mainActivity?.navController?.popBackStack()
    }

    private fun onEnterWallet() {
        mainActivity?.safeNavigate(R.id.action_nav_restore_to_nav_home)
    }

    private fun onDone() {
        mainActivity?.hideKeyboard()
        val seedPhrase = binding.seedInput.text.toString().trim()
        // we only use the default network here because the synchronizer doesn't exist yet
        val activation = NighthawkWalletApp.instance.defaultNetwork.saplingActivationHeight
        val birthday = binding.inputBirthdate.text.toString()
            .let { birthdateString ->
                if (birthdateString.isEmpty()) activation else birthdateString.toInt()
            }.coerceAtLeast(activation)

        try {
            walletSetup.validatePhrase(seedPhrase)
            importWallet(seedPhrase, birthday)
        } catch (t: Throwable) {
            mainActivity?.showInvalidSeedPhraseError(t)
        }
    }

    private fun importWallet(seedPhrase: String, birthday: Int) {
        mainActivity?.hideKeyboard()
        mainActivity?.apply {
            lifecycleScope.launch {
                try {
                    mainActivity?.startSync(walletSetup.importWallet(seedPhrase, birthday))
                    // bugfix: if the user proceeds before the synchronizer is created the app will crash!
                    binding.buttonSuccess.isEnabled = true
                    playSound("sound_receive_small.mp3")
                    vibrateSuccess()
                } catch (e: UnsatisfiedLinkError) {
                    mainActivity?.showSharedLibraryCriticalError(e)
                }
            }
        }

        binding.groupDone.visibility = View.GONE
        binding.groupStart.visibility = View.GONE
        binding.groupSuccess.visibility = View.VISIBLE
        binding.buttonSuccess.isEnabled = false
    }

    object Const {
        const val SEED_WORDS_SEPERATOR = " "
        const val NO_OF_SEED_WORDS = 24
    }
}
