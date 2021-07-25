package com.nighthawkapps.wallet.android.ui.setup

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import javax.inject.Inject
import javax.inject.Named

class PasswordViewModel @Inject constructor(val context: Context) : ViewModel() {

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    fun verifyPassword(enteredPassword: String): Boolean {
        return enteredPassword.equals(getPassword(), false)
    }

    fun getPassword(): String {
        return prefs.get<String>(Const.PIN.PASSWORD) ?: ""
    }

    fun savePassword(password: String) {
        prefs[Const.PIN.PASSWORD] = password
    }

    fun isBioMetricOrFaceIdEnabled(): Boolean {
        return prefs.get<Boolean>(Const.PIN.IS_BIO_METRIC_OR_FACE_ID_ENABLED) ?: false
    }

    fun setBioMetricOrFaceIdEnableStatus(isEnabled: Boolean) {
        prefs[Const.PIN.IS_BIO_METRIC_OR_FACE_ID_ENABLED] = isEnabled
    }

    fun isBioMetricEnabledOnMobile(): Boolean {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
