package com.nighthawkapps.wallet.android.ui.history

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import javax.inject.Inject
import javax.inject.Named

class HistoryViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    val transactions get() = synchronizer.clearedTransactions
    val balance get() = synchronizer.balances
    val latestHeight get() = synchronizer.latestHeight

    var selectedTransaction: ConfirmedTransaction? = null

    suspend fun getAddress() = synchronizer.getAddress()

    override fun onCleared() {
        super.onCleared()
        twig("HistoryViewModel cleared!")
    }
}