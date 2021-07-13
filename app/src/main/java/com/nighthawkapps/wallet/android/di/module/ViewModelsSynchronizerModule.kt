package com.nighthawkapps.wallet.android.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nighthawkapps.wallet.android.di.annotation.SynchronizerScope
import com.nighthawkapps.wallet.android.di.annotation.ViewModelKey
import com.nighthawkapps.wallet.android.di.viewmodel.ViewModelFactory
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ui.history.HistoryViewModel
import com.nighthawkapps.wallet.android.ui.home.BalanceDetailViewModel
import com.nighthawkapps.wallet.android.ui.home.HomeViewModel
import com.nighthawkapps.wallet.android.ui.profile.ProfileViewModel
import com.nighthawkapps.wallet.android.ui.receive.ReceiveViewModel
import com.nighthawkapps.wallet.android.ui.scan.ScanViewModel
import com.nighthawkapps.wallet.android.ui.send.AutoShieldViewModel
import com.nighthawkapps.wallet.android.ui.send.SendViewModel
import com.nighthawkapps.wallet.android.ui.setup.SettingsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Named

/**
 * View model related objects, scoped to the synchronizer.
 */
@Module
abstract class ViewModelsSynchronizerModule {

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(implementation: HomeViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(SendViewModel::class)
    abstract fun bindSendViewModel(implementation: SendViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(HistoryViewModel::class)
    abstract fun bindHistoryViewModel(implementation: HistoryViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(ReceiveViewModel::class)
    abstract fun bindReceiveViewModel(implementation: ReceiveViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(ScanViewModel::class)
    abstract fun bindScanViewModel(implementation: ScanViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(implementation: ProfileViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(implementation: SettingsViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(BalanceDetailViewModel::class)
    abstract fun bindBalanceDetailViewModel(implementation: BalanceDetailViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(AutoShieldViewModel::class)
    abstract fun bindAutoShieldViewModel(implementation: AutoShieldViewModel): ViewModel

    /**
     * Factory for view models that are not created until the Synchronizer exists. Only VMs that
     * require the Synchronizer should wait until it is created. In other words, these are the VMs
     * that live within the scope of the Synchronizer.
     */
    @SynchronizerScope
    @Named(Const.Name.SYNCHRONIZER)
    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}
