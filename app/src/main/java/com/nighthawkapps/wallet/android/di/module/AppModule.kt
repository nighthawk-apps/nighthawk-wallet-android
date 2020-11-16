package com.nighthawkapps.wallet.android.di.module

import android.content.ClipboardManager
import android.content.Context
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.di.component.MainActivitySubcomponent
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(subcomponents = [MainActivitySubcomponent::class])
class AppModule {

    @Provides
    @Singleton
    fun provideAppContext(): Context = NighthawkWalletApp.instance

    @Provides
    @Singleton
    fun provideClipboard(context: Context) =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Named(Const.Name.APP_PREFS)
    fun provideLockbox(appContext: Context): LockBox {
        return LockBox(appContext)
    }
}
