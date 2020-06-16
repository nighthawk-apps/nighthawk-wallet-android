package com.nighthawkapps.wallet.android.feedback

import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.ext.toAppString
import com.mixpanel.android.mpmetrics.MixpanelAPI

class FeedbackMixpanel : FeedbackCoordinator.FeedbackObserver {

    private val mixpanel =
        MixpanelAPI.getInstance(NighthawkWalletApp.instance, R.string.mixpanel_project.toAppString())

    override fun onMetric(metric: Feedback.Metric) {
        track(metric.key, metric.toMap())
    }

    override fun onAction(action: Feedback.Action) {
        track(action.key, action.toMap())
    }

    override fun flush() {
        mixpanel.flush()
    }

    private fun track(eventName: String, properties: Map<String, Any>) {
    }

}