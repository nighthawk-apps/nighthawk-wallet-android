package com.nighthawkapps.wallet.android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.doOnLayout
import com.nighthawkapps.wallet.android.databinding.FragmentFeedbackBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Funnel.UserFeedback
import com.nighthawkapps.wallet.android.feedback.Report.Tap.FEEDBACK_CANCEL
import com.nighthawkapps.wallet.android.feedback.Report.Tap.FEEDBACK_SUBMIT
import com.nighthawkapps.wallet.android.ui.base.BaseFragment


/**
 * Fragment representing the home screen of the app. This is the screen most often seen by the user when launching the
 * application.
 */
class FeedbackFragment :  BaseFragment<FragmentFeedbackBinding>() {
    override val screen = Report.Screen.FEEDBACK

    override fun inflate(inflater: LayoutInflater): FragmentFeedbackBinding =
        FragmentFeedbackBinding.inflate(inflater)

    private lateinit var ratings: Array<View>

//    private val padder = ViewTreeObserver.OnGlobalLayoutListener {
//        Toast.makeText(mainActivity, "LAYOUT", Toast.LENGTH_SHORT).show()
//    }

    //
    // LifeCycle
    //

    override fun onResume() {
        super.onResume()
//        mainActivity!!.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(padder)
//        mainActivity!!.findViewById<View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener(padder)
    }

    override fun onPause() {
        super.onPause()
//        mainActivity!!.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(padder)
//        mainActivity!!.findViewById<View>(android.R.id.content).viewTreeObserver.removeOnGlobalLayoutListener(padder)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButtonHitArea.setOnClickListener(::onFeedbackCancel)
            buttonSubmit.setOnClickListener(::onFeedbackSubmit)

            ratings = arrayOf(feedbackExp1, feedbackExp2, feedbackExp3, feedbackExp4, feedbackExp5)
            ratings.forEach {
                it.setOnClickListener(::onRatingClicked)
            }
        }
    }


    //
    // Private API
    //

    private fun onFeedbackSubmit(view: View) {
        Toast.makeText(mainActivity, "Thanks for the feedback!", Toast.LENGTH_LONG).show()
        tapped(FEEDBACK_SUBMIT)

        val q1 = binding.inputQuestion1.editText?.text.toString()
        val q2 = binding.inputQuestion2.editText?.text.toString()
        val q3 = binding.inputQuestion3.editText?.text.toString()
        val rating = ratings.indexOfFirst { it.isActivated } + 1

        mainActivity?.reportFunnel(UserFeedback.Submitted(rating, q1, q2, q3))

        mainActivity?.navController?.navigateUp()
    }
    private fun onFeedbackCancel(view: View) {
        tapped(FEEDBACK_CANCEL)
        mainActivity?.reportFunnel(UserFeedback.Cancelled)
        mainActivity?.navController?.navigateUp()
    }

    private fun onRatingClicked(view: View) {
        ratings.forEach { it.isActivated = false }
        view.isActivated = !view.isActivated
    }
}
