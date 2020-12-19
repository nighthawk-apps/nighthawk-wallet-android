package com.nighthawkapps.wallet.android.di.component

import cash.z.ecc.android.sdk.Initializer
import com.nighthawkapps.wallet.android.di.annotation.SynchronizerScope
import com.nighthawkapps.wallet.android.di.module.InitializerModule
import dagger.BindsInstance
import dagger.Subcomponent

@SynchronizerScope
@Subcomponent(modules = [InitializerModule::class])
interface InitializerSubcomponent {
    fun initializer(): Initializer
    fun config(): Initializer.Config

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance config: Initializer.Config): InitializerSubcomponent
    }
}
