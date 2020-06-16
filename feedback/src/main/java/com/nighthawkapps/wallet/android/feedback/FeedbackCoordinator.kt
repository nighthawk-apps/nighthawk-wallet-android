package com.nighthawkapps.wallet.android.feedback

import com.nighthawkapps.wallet.android.feedback.util.CompositeJob
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext


/**
 * Takes care of the boilerplate involved in processing feedback emissions. Simply provide callbacks
 * and emissions will occur in a mutually exclusive way, across all processors, so that things like
 * writing to a file can occur without clobbering changes. This class also provides a mechanism for
 * waiting for any in-flight emissions to complete. Lastly, all monitoring will cleanly complete
 * whenever the feedback is stopped or its parent scope is cancelled.
 */
class FeedbackCoordinator(val feedback: Feedback, defaultObservers: Set<FeedbackObserver> = setOf()) {

    init {
        feedback.apply {
            onStart {
                invokeOnCompletion {
                    flush()
                }
            }
        }
        defaultObservers.forEach {
            addObserver(it)
        }
    }

    private var contextMetrics = Dispatchers.IO
    private var contextActions = Dispatchers.IO
    private val jobs = CompositeJob()
    val observers = mutableSetOf<FeedbackObserver>()

    /**
     * Wait for any in-flight listeners to complete.
     */
    suspend fun await() {
        jobs.await()
        flush()
    }

    /**
     * Cancel all in-flight observer functions.
     */
    fun cancel() {
        jobs.cancel()
        flush()
    }

    /**
     * Flush all observers so they can clear all pending buffers.
     */
    fun flush() {
        observers.forEach { it.flush() }
    }

    /**
     * Inject the context on which to observe metrics, mostly for testing purposes.
     */
    fun metricsOn(dispatcher: CoroutineDispatcher): FeedbackCoordinator {
        contextMetrics = dispatcher
        return this
    }

    /**
     * Inject the context on which to observe actions, mostly for testing purposes.
     */
    fun actionsOn(dispatcher: CoroutineDispatcher): FeedbackCoordinator {
        contextActions = dispatcher
        return this
    }

    /**
     * Add a coordinated observer that will not clobber all other observers because their actions
     * are coordinated via a global mutex.
     */
    fun addObserver(observer: FeedbackObserver) {
        feedback.onStart {
            observers += observer
            observeMetrics(observer::onMetric)
            observeActions(observer::onAction)
        }
    }

    inline fun <reified T: FeedbackObserver> findObserver(): T? {
        return observers.firstOrNull { it::class == T::class } as T
    }

    private fun observeMetrics(onMetricListener: (Feedback.Metric) -> Unit) {
        feedback.metrics.onEach {
            jobs += feedback.scope.launch {
                withContext(contextMetrics) {
                    mutex.withLock {
                        onMetricListener(it)
                    }
                }
            }
        }.launchIn(feedback.scope)
    }

    private fun observeActions(onActionListener: (Feedback.Action) -> Unit) {
        feedback.actions.onEach {
            val id = coroutineContext.hashCode()
            jobs += feedback.scope.launch {
                withContext(contextActions) {
                    mutex.withLock {
                        onActionListener(it)
                    }
                }
            }
        }.launchIn(feedback.scope)
    }

    interface FeedbackObserver {
        fun onMetric(metric: Feedback.Metric) {}
        fun onAction(action: Feedback.Action) {}
        fun flush() {}
    }

    companion object {
        const val ENABLED = "setting.feedbackcoordinater.enabled"
        private val mutex: Mutex = Mutex()
    }
}
