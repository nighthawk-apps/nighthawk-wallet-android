package com.nighthawkapps.wallet.android.feedback

import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.feedback.Feedback
import okio.Okio
import java.io.File
import java.text.SimpleDateFormat

class FeedbackFile(fileName: String = "user_log.txt") :
    FeedbackCoordinator.FeedbackObserver {

    val file = File("${NighthawkWalletApp.instance.filesDir}/logs", fileName)
    private val format = SimpleDateFormat("MM-dd HH:mm:ss.SSS")

    init {
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
    }

    override fun onMetric(metric: Feedback.Metric) {
        appendToFile(metric.toString())
    }

    override fun onAction(action: Feedback.Action) {
        appendToFile(action.toString())
    }

    override fun flush() {
        // TODO: be more sophisticated about how we open/close the file. And then flush it here.
    }

    private fun appendToFile(message: String) {
        Okio.buffer(Okio.appendingSink(file)).use {
            it.writeUtf8("${format.format(System.currentTimeMillis())}|\t$message\n")
        }
    }
}