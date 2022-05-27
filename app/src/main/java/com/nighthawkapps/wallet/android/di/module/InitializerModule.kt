package com.nighthawkapps.wallet.android.di.module

import android.content.Context
import cash.z.ecc.android.sdk.Initializer
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
class InitializerModule {

    @Provides
    @Reusable
    fun provideInitializer(appContext: Context, config: Initializer.Config) = Initializer.newBlocking(appContext, config) }
