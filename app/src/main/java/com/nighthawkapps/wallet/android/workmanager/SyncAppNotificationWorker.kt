package com.nighthawkapps.wallet.android.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ui.MainActivity

class SyncAppNotificationWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        createNotificationChannel()
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = TaskStackBuilder.create(appContext).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = NotificationCompat.Builder(appContext, "syncNotificationWork").apply {
            setContentIntent(pendingIntent)
        }
        notification.setContentTitle(R.string.ns_sync_notifications_text.toAppString())
        notification.setContentText(R.string.ns_sync_notifications_text.toAppString())
        notification.priority = NotificationCompat.PRIORITY_HIGH
        notification.setCategory(NotificationCompat.CATEGORY_ALARM)
        notification.setSmallIcon(R.drawable.ic_nighthawk_logo)
        val vibrate = longArrayOf(0, 100, 200, 300)
        notification.setVibrate(vibrate)

        with(NotificationManagerCompat.from(appContext)) {
            notify(2, notification.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channelPeriodic = NotificationChannel("syncNotificationWork", "Period Work Request", importance)
            channelPeriodic.description = "Periodic Work"
            val notificationManager = NotificationManagerCompat.from(appContext)
            notificationManager.createNotificationChannel(channelPeriodic)
        }
    }
}
