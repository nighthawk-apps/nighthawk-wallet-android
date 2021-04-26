package com.nighthawkapps.wallet.android.ui.util

import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ui.MainActivity
import android.media.AudioAttributes
import android.media.AudioManager

object NotificationHelper {
    private const val BALANCE_NOTIFICATION_CHANNEL_ID = "balance_notification_channel_id"
    private const val BALANCE_NOTIFICATION_CHANNEL_NAME = "balance_notification_channel_name"
    private const val BALANCE_NOTIFICATION_ID = 101
    private const val BALANCE_NOTIFICATION_PENDING_REQUEST_CODE = 201
    fun showBalanceNotification(context: Context, title: String, description: String) {
        createBalanceNotificationChannel(context)
        createBalanceNotificationBuilder(context, title, description)
    }

    private fun createBalanceNotificationBuilder(context: Context, title: String, description: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val soundUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.sound_receive_small)
        val pendingIntent = PendingIntent.getActivity(context, BALANCE_NOTIFICATION_PENDING_REQUEST_CODE, intent, FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, BALANCE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        if (soundUri != null) {
            builder.setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
        }
        NotificationManagerCompat.from(context).notify(BALANCE_NOTIFICATION_ID, builder.build())
    }

    private fun createBalanceNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.sound_receive_small)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val notificationChannel = NotificationChannel(BALANCE_NOTIFICATION_CHANNEL_ID, BALANCE_NOTIFICATION_CHANNEL_NAME, NotificationManagerCompat.IMPORTANCE_DEFAULT).apply {
                enableLights(true)
                lightColor = Color.GREEN
            }
            if (soundUri != null) {
                notificationChannel.setSound(soundUri, audioAttributes)
            }
            NotificationManagerCompat.from(context).createNotificationChannel(notificationChannel)
        }
    }
}
