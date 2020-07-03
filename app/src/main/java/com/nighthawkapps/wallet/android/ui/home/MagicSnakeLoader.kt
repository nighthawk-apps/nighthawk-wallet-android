package com.nighthawkapps.wallet.android.ui.home

import android.animation.ValueAnimator
import com.airbnb.lottie.LottieAnimationView

class MagicSnakeLoader(
    val lottie: LottieAnimationView,
    private val scanningStartFrame: Int = 100,
    private val scanningEndFrame: Int = 187,
    val totalFrames: Int = 200
) : ValueAnimator.AnimatorUpdateListener {
    private var isPaused: Boolean = true
    private var isStarted: Boolean = false

    var isSynced: Boolean = false
        set(value) {
            if (value && !isStarted) {
                lottie.progress = 1.0f
                field = value
                return
            }

            // it is started but it hadn't reached the synced state yet
            if (value && !field) {
                field = value
                playToCompletion()
            } else {
                field = value
            }
        }

    var scanProgress: Int = 0
        set(value) {
            field = value
            if (value > 0) {
                startMaybe()
                onScanUpdated()
            }
        }

    var downloadProgress: Int = 0
        set(value) {
            field = value
            if (value > 0) startMaybe()
        }

    private fun startMaybe() {

        if (!isSynced && !isStarted) lottie.postDelayed({
            // after some delay, if we're still not synced then we better start animating (unless we already are)!
            if (!isSynced && isPaused) {
                lottie.resumeAnimation()
                isPaused = false
                isStarted = true
            }
        }, 200L)
    }

    private val isDownloading get() = downloadProgress in 1..99
    private val isScanning get() = scanProgress in 1..99

    init {
        lottie.addAnimatorUpdateListener(this)
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        if (isSynced || isPaused) {
//            playToCompletion()
            return
        }

        // if we are scanning, then set the animation progress, based on the scan progress
        // if we're not scanning, then we're looping
        animation.currentFrame().let { frame ->
            if (isDownloading) allowLoop(frame) else applyScanProgress(frame)
        }
    }

    private val acceptablePauseFrames = arrayOf(33, 34, 67, 68, 99)
    private fun applyScanProgress(frame: Int) {
        // don't hardcode the progress until the loop animation has completed, cleanly
        if (isPaused) {
            onScanUpdated()
        } else {
            // once we're ready to show scan progress, do it! Don't do extra loops.
            if (frame >= scanningStartFrame || frame in acceptablePauseFrames) {
                pause()
            }
        }
    }

    private fun onScanUpdated() {
        if (isSynced) {
//            playToCompletion()
            return
        }

        if (isPaused && isStarted) {
            // move forward within the scan range, proportionate to how much scanning is complete
            val scanRange = scanningEndFrame - scanningStartFrame
            val scanRangeProgress = scanProgress.toFloat() / 100.0f * scanRange.toFloat()
            lottie.progress = (scanningStartFrame.toFloat() + scanRangeProgress) / totalFrames
        }
    }

    private fun playToCompletion() {
        removeLoops()
        unpause()
    }

    private fun removeLoops() {
        lottie.frame.let { frame ->
            if (frame in 33..67) {
                lottie.frame = frame + 34
            } else if (frame in 0..33) {
                lottie.frame = frame + 67
            }
        }
    }

    private fun allowLoop(frame: Int) {
        unpause()
        if (frame >= scanningStartFrame) {
            lottie.progress = 0f
        }
    }

    fun unpause() {
        if (isPaused) {
            lottie.resumeAnimation()
            isPaused = false
        }
    }

    fun pause() {
        if (!isPaused) {
            lottie.pauseAnimation()
            isPaused = true
        }
    }

    private fun ValueAnimator.currentFrame(): Int {
        return ((animatedValue as Float) * totalFrames).toInt()
    }
}
