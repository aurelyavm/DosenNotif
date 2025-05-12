package com.example.dosennotif.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

// Alarm Receiver to handle scheduled notifications
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getStringExtra("notification_id") ?: return
        val title = intent.getStringExtra("title") ?: "Upcoming Class"
        val message = intent.getStringExtra("message") ?: "Your class is starting soon"
        val userId = intent.getStringExtra("user_id")

        // Show notification
        NotificationUtils.showNotification(context, notificationId, title, message, userId)
    }
}