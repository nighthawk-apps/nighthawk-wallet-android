package com.nighthawkapps.wallet.android.ui.send

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isCancelled
import cash.z.ecc.android.sdk.db.entity.isCreated
import cash.z.ecc.android.sdk.db.entity.isCreating
import cash.z.ecc.android.sdk.db.entity.isFailedEncoding
import cash.z.ecc.android.sdk.db.entity.isFailure
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.collectWith
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.databinding.FragmentAutoShieldBinding
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.invisibleIf
import com.nighthawkapps.wallet.android.ext.requireApplicationContext
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.preference.Preferences
import com.nighthawkapps.wallet.android.preference.model.get
import com.nighthawkapps.wallet.android.preference.model.put
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Clock

class AutoShieldFragment : BaseFragment<FragmentAutoShieldBinding>() {

    private val viewModel: AutoShieldViewModel by viewModel()

    private val uiModels = MutableStateFlow(UiModel())

    override fun inflate(inflater: LayoutInflater): FragmentAutoShieldBinding =
        FragmentAutoShieldBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (null == savedInstanceState) {
            setAutoshield(requireApplicationContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButtonHitArea.setOnClickListener {
            onExit()
        }
        mainActivity?.preventBackPress(this)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.pendingTransaction.collectLatest { p: PendingTransaction? ->
                    try {
                        p?.let {
                            uiModels.value = it.toUiModel()
                        }
                    } catch (t: Throwable) {
                        val message =
                            "ERROR: error while handling pending transaction update! $t"
                        twig(message)
                    }
                }
            }
        }
        uiModels.collectWith(lifecycleScope, ::updateUi)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity?.apply {
            viewModel.shieldFunds()
        }
    }

    private fun updateUi(uiModel: UiModel) = uiModel.apply {
        if (isResumed) {
            // if this is the first success
            if (!binding.lottieSuccess.isVisible && showSuccess) {
                mainActivity?.vibrateSuccess()
            }

            binding.backButton.goneIf(!showCloseIcon)
            binding.textTitle.text = title
            binding.lottieShielding.invisibleIf(!showShielding)
            if (pauseShielding) binding.lottieShielding.pauseAnimation()
            binding.lottieSuccess.invisibleIf(!showSuccess)
            binding.imageFailed.invisibleIf(!isFailure)
            binding.textStatus.text = statusMessage

            binding.textStatus.text = when {
                showStatusDetails && showStatusMessage -> statusDetails
                showStatusDetails -> statusDetails
                showStatusMessage -> statusMessage
                else -> ""
            }

            binding.buttonPrimary.text = primaryButtonText
            binding.buttonPrimary.setOnClickListener { primaryAction() }
            binding.buttonMoreInfo.text = moreInfoButtonText
            binding.buttonMoreInfo.goneIf(!showMoreInfoButton)
            binding.buttonMoreInfo.setOnClickListener { moreInfoAction() }

            if (showSuccess) {
                if (viewModel.updateAutoshieldAchievement()) {
                    mainActivity?.showSnackbar(getString(R.string.auto_shield_complete), "View")
                }
            }
        }
    }

    private fun onExit() {
        mainActivity?.safeNavigate(R.id.action_nav_shield_final_to_nav_home)
    }

    private fun onCancel(tx: PendingTransaction) {
        viewModel.cancel(tx.id)
    }

    private fun onSeeDetails() {
        mainActivity?.safeNavigate(R.id.action_nav_shield_final_to_nav_history)
    }

    private fun PendingTransaction.toUiModel() = UiModel().also { model ->
        when {
            isCancelled() -> {
                model.title = getString(R.string.send_final_result_cancelled)
                model.pauseShielding = true
                model.primaryButtonText = getString(R.string.send_final_button_primary_back)
                model.primaryAction = { mainActivity?.navController?.popBackStack() }
            }
            isSubmitSuccess() -> {
                model.showCloseIcon = true
                model.title = getString(R.string.send_final_button_primary_sent)
                model.showShielding = false
                model.showSuccess = true
                model.primaryButtonText = getString(R.string.done)
                model.primaryAction = ::onExit
                model.showMoreInfoButton = true
                model.moreInfoButtonText = getString(R.string.send_final_button_primary_details)
                model.moreInfoAction = ::onSeeDetails
            }
            isFailure() -> {
                model.showCloseIcon = true
                model.title =
                    if (isFailedEncoding()) getString(R.string.send_final_error_encoding) else getString(
                        R.string.send_final_error_submitting
                    )
                model.showShielding = false
                model.showSuccess = false
                model.isFailure = true
                model.showStatusDetails = false
                model.primaryButtonText = getString(R.string.translated_button_back)
                model.primaryAction = { mainActivity?.navController?.popBackStack() }
                model.showMoreInfoButton = errorMessage != null
                model.moreInfoButtonText = getString(R.string.send_more_info)
                model.moreInfoAction = {
                    showMoreInfo(errorMessage ?: "No details available")
                }
            }
            isCreating() -> {
                model.showCloseIcon = false
                model.primaryButtonText = getString(R.string.send_final_button_primary_cancel)
                model.showStatusMessage = true
                model.statusMessage = "Creating transaction..."
                model.primaryAction = { onCancel(this) }
            }
            isCreated() -> {
                model.showStatusMessage = true
                model.statusMessage = "Submitting transaction..."
                model.primaryButtonText = getString(R.string.translated_button_back)
                model.primaryAction = { mainActivity?.navController?.popBackStack() }
            }
            else -> {
                model.primaryButtonText = getString(R.string.translated_button_back)
                model.primaryAction = { mainActivity?.navController?.popBackStack() }
            }
        }
    }

    private fun showMoreInfo(info: String) {
        val current = uiModels.value
        uiModels.value = current.copy(
            showMoreInfoButton = true,
            moreInfoButtonText = getString(R.string.done),
            moreInfoAction = ::onExit,
            showStatusMessage = false,
            showStatusDetails = true,
            statusDetails = info
        )
    }

    // fields are ordered, as they appear, top-to-bottom in the UI because that makes it easier to reason about each screen state
    data class UiModel(
        var showCloseIcon: Boolean = false,
        var title: String = "Shielding Now!",
        var showShielding: Boolean = true,
        var pauseShielding: Boolean = false,
        var showSuccess: Boolean = false,
        var isFailure: Boolean = false,
        var statusMessage: String = "",
        var statusDetails: String = "",
        var showStatusDetails: Boolean = false,
        var showStatusMessage: Boolean = true,
        var primaryButtonText: String = "Cancel",
        var primaryAction: () -> Unit = {},
        var moreInfoButtonText: String = "",
        var showMoreInfoButton: Boolean = false,
        var moreInfoAction: () -> Unit = {}
    )

    companion object {
        private const val maxAutoshieldFrequency: Long = 30 * DateUtils.MINUTE_IN_MILLIS

        /**
         * @param clock Optionally allows injecting a clock, in order to make this testable.
         */
        fun canAutoshield(context: Context, clock: Clock = Clock.systemUTC()): Boolean {
            val currentEpochMillis = clock.millis()
            val lastAutoshieldEpochMillis = Preferences.lastAutoshieldingEpochMillis.get(context)

            val isLastAutoshieldOld =
                (currentEpochMillis - lastAutoshieldEpochMillis) > maxAutoshieldFrequency
            // Prevent a corner case where a user with a clock in the future during one autoshielding prompt
            // could prevent all subsequent autoshielding prompts.
            val isTimeTraveling = lastAutoshieldEpochMillis > currentEpochMillis

            return isLastAutoshieldOld || isTimeTraveling
        }

        /**
         * @param clock Optionally allows injecting a clock, in order to make this testable.
         */
        private fun setAutoshield(context: Context, clock: Clock = Clock.systemUTC()) =
            Preferences.lastAutoshieldingEpochMillis.put(context, clock.millis())
    }
}
