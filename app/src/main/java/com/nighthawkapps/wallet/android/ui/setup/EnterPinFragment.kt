package com.nighthawkapps.wallet.android.ui.setup

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentEnterPinBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.gone
import com.nighthawkapps.wallet.android.ext.visible
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.util.SixDigitPasswordView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnterPinFragment : BaseFragment<FragmentEnterPinBinding>() {

    private lateinit var numberPad: List<TextView>
    private val viewModel: PasswordViewModel by activityViewModel()
    private var chosenPassword: String? = null
    private val args: EnterPinFragmentArgs by navArgs()
    private var dialog: Dialog? = null

    override fun inflate(inflater: LayoutInflater) = FragmentEnterPinBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        twig("EnterPinFragment created ${viewModel.isBioMetricEnabledOnMobile()}")

        updateUI()
        checkBioMetricAndFaceId()

        with(binding) {
            numberPad = arrayListOf(
                buttonNumberPad0.asKey(),
                buttonNumberPad1.asKey(),
                buttonNumberPad2.asKey(),
                buttonNumberPad3.asKey(),
                buttonNumberPad4.asKey(),
                buttonNumberPad5.asKey(),
                buttonNumberPad6.asKey(),
                buttonNumberPad7.asKey(),
                buttonNumberPad8.asKey(),
                buttonNumberPad9.asKey()
            )
        }

        binding.buttonNumberPadBack.setOnClickListener {
            binding.viewPassword.removeLastDigit()
        }

        binding.hitAreaClose.setOnClickListener {
            handleCloseClick()
        }

        mainActivity?.onFragmentBackPressed(this) {
            if (!args.forNewPinSetup) {
                twig("EnterPinFragment: User clicked back button")
                mainActivity?.finish()
            }
        }
    }

    private fun checkBioMetricAndFaceId() {
        CoroutineScope(Dispatchers.Main).launch {
            if (!args.forNewPinSetup && viewModel.isBioMetricOrFaceIdEnabled()) {
                mainActivity?.let {
                    it.authenticate("", getString(R.string.biometric_backup_phrase_title)) {
                        handleNextNavigation()
                    }
                }
            }
        }
    }

    private fun updateUI() {
        binding.viewPassword.clear()
        if (args.forNewPinSetup) {
            binding.titleGroup.visible()
            if (chosenPassword.isNullOrEmpty()) {
                binding.textTitle.text = getString(R.string.choose_pin_code)
                binding.textPinCodeMsg.text = getString(R.string.choose_six_digit_pin_code)
            } else {
                binding.textTitle.text = getString(R.string.verify_pin_code)
                binding.textPinCodeMsg.text = getString(R.string.enter_the_same_six_digits)
            }
        } else { // password already saved, user is trying to verify password to enter in the app
            binding.titleGroup.gone()
            chosenPassword = viewModel.getPassword()
        }
    }

    private fun handleCloseClick() {
        if (args.forNewPinSetup && chosenPassword.isNullOrEmpty()) {
            handleNextNavigation()
            return
        }
        chosenPassword = ""
        updateUI()
    }

    /**
     * Only number key textViews are used for clickListener. If we want to modify in future then we should take care of char conversion to int
     */
    private fun TextView.asKey(): TextView {
        val c = text.toString().toInt()
        setOnClickListener {
            lifecycleScope.launch {
                onInt(c)
            }
        }
        return this
    }

    private fun onInt(number: Int) {
        val isNewDigitAdded = binding.viewPassword.onNewDigit(number)
        if (!isNewDigitAdded) {
            return
        }
        if (binding.viewPassword.getPasswordSize() == SixDigitPasswordView.PASSWORD_LENGTH) {
            val enteredPassword = binding.viewPassword.getPassword()
            if (chosenPassword.isNullOrEmpty()) {
                chosenPassword = enteredPassword
            } else {
                verifyPassword(enteredPassword)
            }
            updateUI()
        }
    }

    private fun verifyPassword(enteredPassword: String) {
        if (chosenPassword == enteredPassword) {
            // Password Verified
            if (args.forNewPinSetup) {
                viewModel.savePassword(enteredPassword)
                mainActivity?.showMessage(getString(R.string.pin_code_setup_done))
            }
            handleNextNavigation()
        } else { // Wrong password entered.
            if (args.forNewPinSetup) {
                showPasswordDidNotMatchError()
            } else {
                updateUI()
            }
            mainActivity?.vibrate(0, 50, 100, 50, 100)
        }
    }

    private fun showPasswordDidNotMatchError() {
        binding.viewPassword.clear()
        dialog?.dismiss()
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.wrong_pin_code))
            .setMessage(getString(R.string.wrong_pin_code_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun handleNextNavigation() {
        viewModel.updateCheckForPin(checkPin = false)
        if (args.forNewPinSetup) {
            mainActivity?.navController?.popBackStack()
        } else {
            onEnterAppWallet()
        }
    }

    private fun onEnterAppWallet() {
        mainActivity?.safeNavigate(R.id.action_enter_pin_to_nav_home)
    }
}
