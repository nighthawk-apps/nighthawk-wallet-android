package com.nighthawkapps.wallet.android

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.nighthawkapps.wallet.android.di.component.AppComponent
import com.nighthawkapps.wallet.android.di.component.DaggerAppComponent
import com.nighthawkapps.wallet.android.feedback.FeedbackCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class NighthawkWalletApp : Application(), CameraXConfig.Provider {

    @Inject
    lateinit var coordinator: FeedbackCoordinator

    var creationTime: Long = 0
        private set

    var creationMeasured: Boolean = false

    /**
     * Intentionally private Scope for use with launching Feedback jobs. The feedback object has the
     * longest scope in the app because it needs to be around early in order to measure launch times
     * and stick around late in order to catch crashes. We intentionally don't expose this because
     * application objects can have odd lifecycles, given that there is no clear onDestroy moment in
     * many cases.
     */
    private var feedbackScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        creationTime = System.currentTimeMillis()
        instance = this
        super.onCreate()

        component = DaggerAppComponent.factory().create(this)
        component.inject(this)
        feedbackScope.launch {
            coordinator.feedback.start()
        }
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    companion object {
        lateinit var instance: NighthawkWalletApp
        lateinit var component: AppComponent
    }
}
