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

    private var checkForPin = true

    fun verifyPassword(enteredPassword: String): Boolean {
        return enteredPassword.equals(getPassword(), false)
    }

    fun getPassword(): String {
        return prefs[Const.PIN.PASSWORD] ?: ""
    }

    fun savePassword(password: String) {
        prefs[Const.PIN.PASSWORD] = password
    }

    fun isPinCodeEnabled(): Boolean {
        return getPassword().isNotEmpty()
    }

    /**
     * @return Check whether we have to show enter again to EnterPinFragment
     */
    fun needToCheckPin(): Boolean {
        return checkForPin
    }

    /**
     * We are entering in EnterPinFragment from HomeFragment and will use this to update variable not to enter again for Pin
     */
    fun updateCheckForPin(checkPin: Boolean) {
        checkForPin = checkPin
    }

    /**
     * @return check user has enabled option of BioMetric/FaceId from App
     */
    fun isBioMetricOrFaceIdEnabled(): Boolean {
        return prefs[Const.PIN.IS_BIO_METRIC_OR_FACE_ID_ENABLED] ?: false
    }

    fun setBioMetricOrFaceIdEnableStatus(isEnabled: Boolean) {
        prefs[Const.PIN.IS_BIO_METRIC_OR_FACE_ID_ENABLED] = isEnabled
    }

    /**
     * @return check whether mobile has BioMetric available or not
     */
    fun isBioMetricEnabledOnMobile(): Boolean {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
