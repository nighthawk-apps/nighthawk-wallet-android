package com.nighthawkapps.wallet.android.di.module

import com.nighthawkapps.wallet.android.di.annotation.SynchronizerScope
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import dagger.Module
import dagger.Provides

/**
 * Module that creates the synchronizer from an initializer and also everything that depends on the
 * synchronizer (because it doesn't exist prior to this module being installed).
 */
@Module(includes = [ViewModelsSynchronizerModule::class])
class SynchronizerModule {

    @Provides
    @SynchronizerScope
    fun provideSynchronizer(initializer: Initializer): Synchronizer {
        return Synchronizer(initializer)
    }
}
