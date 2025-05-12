package com.example.dosennotif.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

// Snooze Receiver to handle snooze action
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getStringExtra("notification_id") ?: return
        val title = intent.getStringExtra("title") ?: "Upcoming Class"
        val message = intent.getStringExtra("message") ?: "Your class is starting soon"
        val userId = intent.getStringExtra("user_id")

        // Cancel current notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId.hashCode())

        // Schedule new notification after 10 minutes
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", "Snoozed: $title")
            putExtra("message", message)
            putExtra("user_id", userId)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (notificationId + "_snoozed").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule alarm for 10 minutes later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + (10 * 60 * 1000),
                snoozePendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + (10 * 60 * 1000),
                snoozePendingIntent
            )
        }
    }
}