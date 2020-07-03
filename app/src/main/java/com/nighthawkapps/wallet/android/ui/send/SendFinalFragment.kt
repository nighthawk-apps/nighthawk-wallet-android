package com.nighthawkapps.wallet.android.ui.send

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isCreated
import cash.z.ecc.android.sdk.db.entity.isCreating
import cash.z.ecc.android.sdk.db.entity.isFailedEncoding
import cash.z.ecc.android.sdk.db.entity.isFailedSubmit
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSendFinalBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.random.Random

class SendFinalFragment : BaseFragment<FragmentSendFinalBinding>() {

    override val screen = Report.Screen.SEND_FINAL

    private val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendFinalBinding =
        FragmentSendFinalBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonNext.setOnClickListener {
            onExit()
        }
        binding.buttonRetry.setOnClickListener {
            onRetry()
        }
        binding.backButtonHitArea.setOnClickListener {
            onExit()
        }
        binding.textConfirmation.text =
            "Sending ${sendViewModel.zatoshiAmount.convertZatoshiToZecString(8)} ZEC to ${sendViewModel.toAddress.toAbbreviatedAddress()}"
        sendViewModel.memo.trim().isNotEmpty().let { hasMemo ->
            binding.radioIncludeAddress.isChecked = hasMemo
            binding.radioIncludeAddress.goneIf(!hasMemo)
        }
        mainActivity?.preventBackPress(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity?.apply {
            mainActivity?.lifecycleScope?.let {
                sendViewModel.send().onEach {
                    onPendingTxUpdated(it)
                }.launchIn(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        flow {
            val max = binding.progressHorizontal.max - 1
            var progress = 0
            while (progress < max) {
                emit(progress)
                delay(Random.nextLong(1000))
                progress++
            }
        }.onEach {
            binding.progressHorizontal.progress = it
        }.launchIn(resumedScope)
    }

    private fun onPendingTxUpdated(pendingTransaction: PendingTransaction?) {
        try {
            if (pendingTransaction != null) sendViewModel.updateMetrics(pendingTransaction)
            val id = pendingTransaction?.id ?: -1
            var isSending = true
            var isFailure = false
            var step: Report.Funnel.Send? = null
            val message = when {
                pendingTransaction == null -> "Transaction not found".also {
                    step = Report.Funnel.Send.ErrorNotFound
                }
                pendingTransaction.isMined() -> "Transaction Mined!\n\nSEND COMPLETE".also {
                    isSending = false; step =
                    Report.Funnel.Send.Mined(pendingTransaction.minedHeight)
                }
                pendingTransaction.isSubmitSuccess() -> "Successfully submitted transaction!\nAwaiting confirmation . . .".also {
                    step = Report.Funnel.Send.Submitted
                }
                pendingTransaction.isFailedEncoding() -> "ERROR: failed to encode transaction! (id: $id)".also {
                    isSending = false; isFailure = true; step = Report.Funnel.Send.ErrorEncoding(
                    pendingTransaction?.errorCode,
                    pendingTransaction?.errorMessage
                )
                }
                pendingTransaction.isFailedSubmit() -> "ERROR: failed to submit transaction! (id: $id)".also {
                    isSending = false; isFailure = true; step = Report.Funnel.Send.ErrorSubmitting(
                    pendingTransaction?.errorCode,
                    pendingTransaction?.errorMessage
                )
                }
                pendingTransaction.isCreated() -> "Transaction creation complete!".also {
                    step = Report.Funnel.Send.Created(id)
                }
                pendingTransaction.isCreating() -> "Creating transaction . . .".also {
                    step = Report.Funnel.Send.Creating
                }
                else -> "Transaction updated!".also { twig("Unhandled TX state: $pendingTransaction") }
            }

            sendViewModel.funnel(step)

            twig("Pending TX (id: ${pendingTransaction?.id} Updated with message: $message")
            binding.textStatus.apply {
                text = "$message"
            }
            binding.backButton.goneIf(!binding.textStatus.text.toString().contains("Awaiting"))
            binding.buttonNext.goneIf((pendingTransaction?.isSubmitSuccess() != true) && (pendingTransaction?.isCreated() != true) && !isFailure)
            binding.buttonNext.text = if (isSending) "Done" else "Finished"
            binding.buttonRetry.goneIf(!isFailure)
            binding.progressHorizontal.goneIf(!isSending)

            if (pendingTransaction?.isSubmitSuccess() == true) {
                sendViewModel.reset()
            }
        } catch (t: Throwable) {
            val message = "ERROR: error while handling pending transaction update! $t"
            twig(message)
            mainActivity?.feedback?.report(Report.Error.NonFatal.TxUpdateFailed(t))
            mainActivity?.feedback?.report(t)
        }
    }

    private fun onExit() {
        mainActivity?.navController?.popBackStack(R.id.nav_home, false)
    }

    private fun onRetry() {
        mainActivity?.navController?.popBackStack(R.id.nav_send_address, false)
    }
}
