package com.nighthawkapps.wallet.android.feedback

import android.util.Log

class FeedbackConsole : FeedbackCoordinator.FeedbackObserver {

    override fun onMetric(metric: Feedback.Metric) {
        log(metric.toString())
    }

    override fun onAction(action: Feedback.Action) {
        log(action.toString())
    }

    override fun flush() {
        // TODO: flush logs (once we have the real logging in place)
    }

    private fun log(message: String) {
        Log.d("@TWIG", message)
    }
}