package com.nighthawkapps.wallet.android.ui.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.isCreated
import cash.z.ecc.android.sdk.db.entity.isCreating
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.validate.AddressType
import com.nighthawkapps.wallet.android.feedback.Feedback
import com.nighthawkapps.wallet.android.feedback.Feedback.Keyed
import com.nighthawkapps.wallet.android.feedback.Feedback.TimeMetric
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Funnel.Send.SendSelected
import com.nighthawkapps.wallet.android.feedback.Report.Funnel.Send.SpendingKeyFound
import com.nighthawkapps.wallet.android.feedback.Report.Issue
import com.nighthawkapps.wallet.android.feedback.Report.MetricType
import com.nighthawkapps.wallet.android.feedback.Report.MetricType.TRANSACTION_SUBMITTED
import com.nighthawkapps.wallet.android.feedback.Report.MetricType.TRANSACTION_MINED
import com.nighthawkapps.wallet.android.feedback.Report.MetricType.TRANSACTION_CREATED
import com.nighthawkapps.wallet.android.feedback.Report.MetricType.TRANSACTION_INITIALIZED
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.android.ui.util.INCLUDE_MEMO_PREFIX
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SendViewModel @Inject constructor() : ViewModel() {

    private val metrics = mutableMapOf<String, TimeMetric>()

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var initializer: Initializer

    @Inject
    lateinit var feedback: Feedback

    var fromAddress: String = ""
    var toAddress: String = ""
    var memo: String = ""
    var zatoshiAmount: Long = -1L
    var includeFromAddress: Boolean = false
        set(value) {
            require(!value || (value && !fromAddress.isNullOrEmpty())) {
                "Error: fromAddress was empty while attempting to include it in the memo. Verify" +
                        " that initFromAddress() has previously been called on this viewmodel."
            }
            field = value
        }
    val isShielded get() = toAddress.startsWith("z")

    fun send(): Flow<PendingTransaction> {
        funnel(SendSelected)
        val memoToSend = createMemoToSend()
        val keys = lockBox.getBytes(WalletSetupViewModel.LockBoxKey.SEED)?.let {
            initializer.deriveSpendingKeys(
                it
            )
        }
        funnel(SpendingKeyFound)
        reportIssues(memoToSend)
        return synchronizer.sendToAddress(
            keys?.get(0) ?: "",
            zatoshiAmount,
            toAddress,
            memoToSend.chunked(ZcashSdk.MAX_MEMO_SIZE).firstOrNull() ?: ""
        ).onEach {
            twig(it.toString())
        }
    }

    fun createMemoToSend() =
        if (includeFromAddress) "$memo\n$INCLUDE_MEMO_PREFIX\n$fromAddress" else memo

    private fun reportIssues(memoToSend: String) {
        if (toAddress == fromAddress) feedback.report(Issue.SelfSend)
        when {
            zatoshiAmount < ZcashSdk.MINERS_FEE_ZATOSHI -> feedback.report(Issue.TinyAmount)
            zatoshiAmount < 100 -> feedback.report(Issue.MicroAmount)
            zatoshiAmount == 1L -> feedback.report(Issue.MinimumAmount)
        }
        memoToSend.length.also {
            when {
                it > ZcashSdk.MAX_MEMO_SIZE -> feedback.report(Issue.TruncatedMemo(it))
                it > (ZcashSdk.MAX_MEMO_SIZE * 0.96) -> feedback.report(Issue.LargeMemo(it))
            }
        }
    }

    suspend fun validateAddress(address: String): AddressType =
        synchronizer.validateAddress(address)

    fun validate(maxZatoshi: Long?) = flow<String?> {

        when {
            synchronizer.validateAddress(toAddress).isNotValid -> {
                emit("Please enter a valid address.")
            }
            zatoshiAmount < 1 -> {
                emit("Please enter at least 1 Zatoshi.")
            }
            maxZatoshi != null && zatoshiAmount > maxZatoshi -> {
                emit("Please enter no more than ${maxZatoshi.convertZatoshiToZecString(8)} ZEC.")
            }
            createMemoToSend().length > ZcashSdk.MAX_MEMO_SIZE -> {
                emit("Memo must be less than ${ZcashSdk.MAX_MEMO_SIZE} in length.")
            }
            else -> emit(null)
        }
    }

    fun afterInitFromAddress(block: () -> Unit) {
        viewModelScope.launch {
            fromAddress = synchronizer.getAddress()
            block()
        }
    }

    fun reset() {
        fromAddress = ""
        toAddress = ""
        memo = ""
        zatoshiAmount = -1L
        includeFromAddress = false
    }

    fun updateMetrics(tx: PendingTransaction) {
        try {
            when {
                tx.isMined() -> TRANSACTION_SUBMITTED to TRANSACTION_MINED by tx.id
                tx.isSubmitSuccess() -> TRANSACTION_CREATED to TRANSACTION_SUBMITTED by tx.id
                tx.isCreated() -> TRANSACTION_INITIALIZED to TRANSACTION_CREATED by tx.id
                tx.isCreating() -> +TRANSACTION_INITIALIZED by tx.id
                else -> null
            }?.let { metricId ->
                report(metricId)
            }
        } catch (t: Throwable) {
            feedback.report(t)
        }
    }

    fun report(metricId: String?) {
        metrics[metricId]?.let { metric ->
            metric.takeUnless { (it.elapsedTime ?: 0) <= 0L }?.let {
                viewModelScope.launch {
                    withContext(IO) {
                        feedback.report(metric)

                        // does this metric complete another metric?
                        metricId?.toRelatedMetricId().let { relatedId ->
                            metrics[relatedId]?.let { relatedMetric ->
                                // then remove the related metric, itself. And the relation.
                                metrics.remove(metricId?.toTxId()?.let { it1 ->
                                    relatedMetric.toMetricIdFor(
                                        it1
                                    )
                                })
                                metrics.remove(relatedId)
                            }
                        }

                        // remove all top-level metrics
                        if (metric.key == Report.MetricType.TRANSACTION_MINED.key) metrics.remove(
                            metricId
                        )
                    }
                }
            }
        }
    }

    fun funnel(step: Report.Funnel.Send?) {
        step ?: return
        feedback.report(step)
    }

    private operator fun MetricType.unaryPlus(): TimeMetric =
        TimeMetric(key, description).markTime()

    private infix fun TimeMetric.by(txId: Long) =
        this.toMetricIdFor(txId).also { metrics[it] = this }

    private infix fun Pair<MetricType, MetricType>.by(txId: Long): String? {
        val startMetric = first.toMetricIdFor(txId).let { metricId ->
            metrics[metricId].also { if (it == null) println("Warning no start metric for id: $metricId") }
        }
        return startMetric?.endTime?.let { startMetricEndTime ->
            TimeMetric(second.key, second.description, mutableListOf(startMetricEndTime))
                .markTime().let { endMetric ->
                    endMetric.toMetricIdFor(txId).also { metricId ->
                        metrics[metricId] = endMetric
                        metrics[metricId.toRelatedMetricId()] = startMetric
                    }
                }
        }
}

    private fun Keyed<String>.toMetricIdFor(id: Long): String = "$id.$key"
    private fun String.toRelatedMetricId(): String = "$this.related"
    private fun String.toTxId(): Long = split('.').first().toLong()
}
