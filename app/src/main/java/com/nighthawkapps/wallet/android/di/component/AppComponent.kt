package com.nighthawkapps.wallet.android.di.component

import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.di.module.AppModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(nighthawkWalletApp: NighthawkWalletApp)

    // Subcomponents
    fun mainActivitySubcomponent(): MainActivitySubcomponent.Factory
    fun synchronizerSubcomponent(): SynchronizerSubcomponent.Factory
    fun initializerSubcomponent(): InitializerSubcomponent.Factory

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: NighthawkWalletApp): AppComponent
    }
}