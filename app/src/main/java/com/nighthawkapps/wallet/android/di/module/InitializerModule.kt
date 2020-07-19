package com.nighthawkapps.wallet.android.di.module

import android.content.Context
import cash.z.ecc.android.sdk.Initializer
import com.nighthawkapps.wallet.android.ext.sharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
class InitializerModule {

    companion object {
        val defaultHost = "lightwalletd.z.cash"
        val defaultPort = 9067
    }

    private val host: String = sharedPreferences.getString("SERVER_HOST", defaultHost) ?: defaultHost
    private val port: Int = sharedPreferences.getInt("SERVER_PORT", defaultPort)

    @Provides
    @Reusable
    fun provideInitializer(appContext: Context) = Initializer(appContext, host, port)
}
