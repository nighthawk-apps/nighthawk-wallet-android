package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.util.WorkManagerUtils
import javax.inject.Inject

class SyncNotificationViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var lockBox: LockBox

    fun getPreSelectedNotificationSyncPref(): NotificationSyncPref {
        return NotificationSyncPref.getNotificationSyncPrefByText(lockBox[Const.AppConstants.KEY_SYNC_NOTIFICATION] ?: "")
    }

    fun updateNotificationSyncPref(notificationSyncPref: NotificationSyncPref) {
        lockBox[Const.AppConstants.KEY_SYNC_NOTIFICATION] = notificationSyncPref.text
        WorkManagerUtils.cancelSyncAppNotificationAndReRegister(notificationSyncPref)
    }

    enum class NotificationSyncPref(val text: String, val frequencyInDays: Int) {
        WEEKLY("Weekly", 7),
        MONTHLY("Monthly", 30),
        OFF("OFF", -1);

        companion object {
            fun getNotificationSyncPrefByText(text: String): NotificationSyncPref {
                return when (text.lowercase()) {
                    WEEKLY.text.lowercase() -> WEEKLY
                    MONTHLY.text.lowercase() -> MONTHLY
                    OFF.text.lowercase() -> OFF
                    else -> OFF
                }
            }
        }
    }
}
