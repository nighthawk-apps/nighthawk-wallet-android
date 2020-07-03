package com.nighthawkapps.wallet.android.feedback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackObserverTest {

    private val feedback: Feedback = Feedback()
    private val feedbackCoordinator: FeedbackCoordinator = FeedbackCoordinator(feedback)

    private var counter: Int = 0
    private val simpleAction = object : Feedback.Action {
        override val key = "ButtonClick"
    }

    @Test
    fun testConcurrency() = runBlocking {
        val actionCount = 50
        val processorCount = 50
        val expectedTotal = actionCount * processorCount

        repeat(processorCount) {
            addObserver()
        }

        feedback.start()
        repeat(actionCount) {
            sendAction()
        }

        feedback.await() // await sends
        feedbackCoordinator.await() // await processing
        feedback.stop()

        assertEquals(
            "Concurrent modification happened ${expectedTotal - counter} times",
            expectedTotal,
            counter
        )
    }

    private fun addObserver() {
        feedbackCoordinator.addObserver(object : FeedbackCoordinator.FeedbackObserver {
            override fun onAction(action: Feedback.Action) {
                counter++
            }
        })
    }

    private fun sendAction() {
        feedback.report(simpleAction)
    }
}