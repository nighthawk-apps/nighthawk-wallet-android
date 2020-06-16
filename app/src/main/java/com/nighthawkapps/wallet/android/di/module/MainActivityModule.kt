package com.nighthawkapps.wallet.android.di.module

import com.nighthawkapps.wallet.android.di.component.InitializerSubcomponent
import com.nighthawkapps.wallet.android.di.component.SynchronizerSubcomponent
import dagger.Module

@Module(includes = [ViewModelsActivityModule::class], subcomponents = [SynchronizerSubcomponent::class, InitializerSubcomponent::class])
class MainActivityModule {

}
