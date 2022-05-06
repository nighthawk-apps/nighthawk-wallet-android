package com.nighthawkapps.wallet.android.ui.send

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RawRes
import androidx.core.view.isInvisible
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isCancelled
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.db.entity.isFailure
import cash.z.ecc.android.sdk.db.entity.isFailedEncoding
import cash.z.ecc.android.sdk.db.entity.isCreating
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendStatusBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SendStatusFragment : BaseFragment<FragmentSendStatusBinding>() {

    private val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendStatusBinding {
        return FragmentSendStatusBinding.inflate(layoutInflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity?.apply {
            sendViewModel.send().onEach {
                onPendingTxUpdated(it)
            }.launchIn((sendViewModel.synchronizer as SdkSynchronizer).coroutineScope)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPrimary.setOnClickListener {
            onReturnToSendReview()
        }
        binding.hitAreaExit.setOnClickListener {
            onReturnToSendReview()
        }
        mainActivity?.preventBackPress(this)
    }

    private fun onPendingTxUpdated(tx: PendingTransaction?) {
        if (tx == null || !isResumed) return

        try {
            updateUi(tx.toUiModel())

            // only hold onto the view model if the transaction failed so that the user can retry
            if (tx.isSubmitSuccess()) {
                sendViewModel.reset()
                // celebrate
                mainActivity?.vibrate(0, 100, 100, 200, 200, 400)
            }
        } catch (t: Throwable) {
            val message = "ERROR: error while handling pending transaction update! $t"
            twig(message)
        }
    }

    private fun updateUi(model: UiModel) {
        binding.apply {
            ivBack.isInvisible = !model.showCloseIcon
            binding.tvTitle.text = model.title
            lottieSending.goneIf(!model.showProgress)
            lottieSending.setAnimation(model.animationRes)
            if (!model.showProgress) lottieSending.pauseAnimation() else lottieSending.playAnimation()
            btnPrimary.apply {
                text = model.primaryButtonText
                setOnClickListener { model.primaryAction() }
            }
            tvMoreDetails.apply {
                goneIf(!model.showSecondaryButton)
                text = model.secondaryButtonText
                setOnClickListener { model.secondaryAction() }
            }
        }
    }

    private fun PendingTransaction.toUiModel() = UiModel().also { model ->
        when {
            isCancelled() -> {
                model.title = getString(R.string.send_final_result_cancelled)
                model.primaryButtonText = getString(R.string.send_final_button_primary_back)
                model.primaryAction = { onReturnToSendReview() }
            }
            isSubmitSuccess() -> {
                model.title = getString(R.string.ns_success)
                model.primaryButtonText = getString(R.string.ns_done)
                model.primaryAction = { onExit() }
                model.showSecondaryButton = true
                model.secondaryButtonText = getString(R.string.ns_more_details)
                model.secondaryAction = { onSeeDetails() }
                model.animationRes = R.raw.lottie_success
            }
            isFailure() -> {
                model.title = getString(R.string.ns_failed)
                model.errorMessage = if (isFailedEncoding()) getString(R.string.send_final_error_encoding) else getString(
                    R.string.send_final_error_submitting
                )
                model.errorDescription = errorMessage.toString()
                model.primaryButtonText = getString(R.string.ns_try_again)
                model.primaryAction = { onReturnToSendReview() }
                model.showSecondaryButton = true
                model.secondaryButtonText = getString(R.string.ns_cancel)
                model.secondaryAction = { onExit() }
                model.animationRes = R.raw.lottie_failed
            }
            else -> {
                model.title = getString(R.string.send_final_sending)
                model.showProgress = true
                if (isCreating()) {
                    model.showCloseIcon = false
                    model.primaryButtonText = getString(R.string.ns_cancel)
                    model.primaryAction = { onCancel(this) }
                } else {
                    model.primaryButtonText = getString(R.string.ns_done)
                    model.primaryAction = { onExit() }
                }
            }
        }
    }

    private fun onExit() {
        sendViewModel.reset()
        mainActivity?.safeNavigate(R.id.action_nav_send_status_to_nav_home)
    }

    private fun onCancel(tx: PendingTransaction) {
        sendViewModel.cancel(tx.id)
    }

    private fun onReturnToSendReview() {
        mainActivity?.safeNavigate(R.id.action_nav_send_status_to_nav_send_review)
    }

    private fun onSeeDetails() {
        sendViewModel.reset()
        mainActivity?.safeNavigate(R.id.action_nav_send_status_to_nav_history)
    }

    // fields are ordered, as they appear, top-to-bottom in the UI because that makes it easier to reason about each screen state
    data class UiModel(
        var showCloseIcon: Boolean = true,
        var title: String = "",
        var errorDescription: String = "",
        var showProgress: Boolean = true,
        var errorMessage: String = "",
        var primaryButtonText: String = "Done",
        var primaryAction: () -> Unit = {},
        var showSecondaryButton: Boolean = false,
        var secondaryButtonText: String = "Done",
        var secondaryAction: () -> Unit = {},
        @RawRes var animationRes: Int = R.raw.lottie_sending
    )
}
