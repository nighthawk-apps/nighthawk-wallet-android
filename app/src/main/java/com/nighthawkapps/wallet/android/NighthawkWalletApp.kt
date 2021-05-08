package com.nighthawkapps.wallet.android

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import cash.z.ecc.android.sdk.type.ZcashNetwork
import com.nighthawkapps.wallet.android.di.component.AppComponent
import com.nighthawkapps.wallet.android.di.component.DaggerAppComponent

class NighthawkWalletApp : Application(), CameraXConfig.Provider {

    /**
     * Network of this build. The Synchronizer.network property should be preferred over this value.
     */
    lateinit var defaultNetwork: ZcashNetwork

    override fun onCreate() {
        instance = this
        super.onCreate()
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
