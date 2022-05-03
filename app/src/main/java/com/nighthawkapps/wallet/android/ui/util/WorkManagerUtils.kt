package com.nighthawkapps.wallet.android.ui.util

import android.content.Context
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.ExistingPeriodicWorkPolicy
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ui.setup.SyncNotificationViewModel
import com.nighthawkapps.wallet.android.workmanager.SyncAppNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkManagerUtils {

    fun cancelSyncAppNotificationAndReRegister(newSyncNotificationPref: SyncNotificationViewModel.NotificationSyncPref) {
        CoroutineScope(Dispatchers.IO).launch {
            cancelWork(
                NighthawkWalletApp.instance.applicationContext,
                Const.AppConstants.WORKER_TAG_SYNC_NOTIFICATION
            )

            if (newSyncNotificationPref != SyncNotificationViewModel.NotificationSyncPref.OFF) {
                startPeriodicSyncNotificationWork(
                    NighthawkWalletApp.instance.applicationContext,
                    newSyncNotificationPref.frequencyInDays.toLong()
                )
            }
        }
    }

    private fun cancelWork(appContext: Context, tag: String) {
        WorkManager.getInstance(appContext).cancelAllWorkByTag(tag)
    }

    private fun startPeriodicSyncNotificationWork(appContext: Context, frequencyInDays: Long) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Set Execution around 07:00:00 AM
        dueDate.set(Calendar.HOUR_OF_DAY, 7)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff)

        val myConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequest.Builder(SyncAppNotificationWorker::class.java, frequencyInDays, TimeUnit.DAYS)
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setConstraints(myConstraints)
            .addTag(Const.AppConstants.WORKER_TAG_SYNC_NOTIFICATION)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(Const.AppConstants.WORKER_TAG_SYNC_NOTIFICATION,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
