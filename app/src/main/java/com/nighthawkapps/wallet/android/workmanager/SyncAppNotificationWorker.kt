package com.nighthawkapps.wallet.android.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
        Log.d("NightHawk SyncAppNotificationWorker", "SyncAppNotification worker do work called")
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        createNotificationChannel()
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

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
