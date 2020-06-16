package com.nighthawkapps.wallet.android.feedback

class FeedbackCrashlytics :
    FeedbackCoordinator.FeedbackObserver {
    /**
     * Report non-fatal crashes because fatal ones already get reported by default.
     */
    override fun onAction(action: Feedback.Action) {
        var exception: Throwable? = null
        exception = when (action) {
            is Feedback.Crash -> action.exception
            is Feedback.NonFatal -> action.exception
            is Report.Error.NonFatal.Reorg -> ReorgException(
                action.errorHeight,
                action.rewindHeight,
                action.toString()
            )
            else -> null
        }
        exception?.let {  }
    }

    private class ReorgException(errorHeight: Int, rewindHeight: Int, reorgMesssage: String) :
        Throwable(reorgMesssage)
}