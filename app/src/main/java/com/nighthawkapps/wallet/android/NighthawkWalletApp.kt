package com.nighthawkapps.wallet.android

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.nighthawkapps.wallet.android.di.component.AppComponent
import com.nighthawkapps.wallet.android.di.component.DaggerAppComponent

class NighthawkWalletApp : Application(), CameraXConfig.Provider {

    override fun onCreate() {
        instance = this
        super.onCreate()

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
