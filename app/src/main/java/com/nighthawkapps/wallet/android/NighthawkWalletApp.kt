package com.nighthawkapps.wallet.android

import android.app.Application
import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.nighthawkapps.wallet.android.ext.Twig
import cash.z.ecc.android.sdk.model.ZcashNetwork
import com.nighthawkapps.wallet.android.di.component.AppComponent
import com.nighthawkapps.wallet.android.di.component.DaggerAppComponent
import com.nighthawkapps.wallet.android.ext.StrictModeHelper

class NighthawkWalletApp : Application(), CameraXConfig.Provider {

    /**
     * Network of this build. The Synchronizer.network property should be preferred over this value.
     */
    lateinit var defaultNetwork: ZcashNetwork

    /** The amount of transparent funds that need to accumulate before autoshielding is triggered */
    val autoshieldThreshold: Long = 1_000_000L // 0.01 ZEC

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // Setting a global reference to the application object is icky; we should try to refactor
        // this away if possible.  Doing this in attachBaseContext instead of onCreate()
        // to avoid any lifecycle issues, as certain components can run before Application.onCreate()
        // (like ContentProvider initialization), but attachBaseContext will still run before that.
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        // Register this before the uncaught exception handler, because we want to make sure the
        // exception handler also doesn't do disk IO.  Since StrictMode only applies for debug builds,
        // we'll also see the crashes during development right away and won't miss them if they aren't
        // reported by the crash reporting.
        if (BuildConfig.DEBUG) {
            StrictModeHelper.enableStrictMode()
            Twig.enabled(true)
        }

        defaultNetwork = ZcashNetwork.from(resources.getInteger(R.integer.zcash_network_id))
        component = DaggerAppComponent.factory().create(this)
        component.inject(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    companion object {
        lateinit var instance: NighthawkWalletApp
        lateinit var component: AppComponent
    }
}
