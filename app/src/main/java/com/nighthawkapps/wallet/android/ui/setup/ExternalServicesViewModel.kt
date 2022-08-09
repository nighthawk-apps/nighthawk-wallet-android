package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import javax.inject.Inject
import javax.inject.Named

class ExternalServicesViewModel @Inject constructor() : ViewModel() {

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    /**
     * @return check user has enabled option of Unstoppable Domain Service from App
     */
    fun isUnsEnabled(): Boolean {
        return prefs[Const.AppConstants.USE_UNSTOPPABLE_NAME_SERVICE] ?: false
    }

    fun setUnsEnableStatus(isEnabled: Boolean) {
        prefs[Const.AppConstants.USE_UNSTOPPABLE_NAME_SERVICE] = isEnabled
    }
}
