package com.nighthawkapps.wallet.android.di.component

import cash.z.ecc.android.sdk.Initializer
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.di.annotation.SynchronizerScope
import com.nighthawkapps.wallet.android.di.module.InitializerModule
import dagger.BindsInstance
import dagger.Subcomponent

@SynchronizerScope
@Subcomponent(modules = [InitializerModule::class])
interface InitializerSubcomponent {

    fun initializer(): Initializer
    fun birthdayStore(): Initializer.WalletBirthdayStore

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance birthdayStore: Initializer.WalletBirthdayStore = Initializer.DefaultBirthdayStore(
                NighthawkWalletApp.instance
            )
        ): InitializerSubcomponent
    }
}
