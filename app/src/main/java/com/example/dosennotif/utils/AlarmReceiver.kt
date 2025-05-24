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
    // Di AlarmReceiver.kt, update untuk pass data ke showNotification:
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getStringExtra("notification_id") ?: return
        val title = intent.getStringExtra("title") ?: "Upcoming Class"
        val message = intent.getStringExtra("message") ?: "Your class is starting soon"
        val userId = intent.getStringExtra("user_id")

        // Get schedule data
        val scheduleId = intent.getStringExtra("schedule_id")
        val scheduledTime = intent.getLongExtra("scheduled_time", 0L)
        val actualScheduleTime = intent.getLongExtra("actual_schedule_time", 0L)
        val room = intent.getStringExtra("room")
        val courseName = intent.getStringExtra("course_name")
        val className = intent.getStringExtra("class_name")

        // Show notification with schedule data
        NotificationUtils.showNotification(
            context, notificationId, title, message, userId,
            scheduleId, scheduledTime, actualScheduleTime, room, courseName, className
        )
    }
    /*
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getStringExtra("notification_id") ?: return
        val title = intent.getStringExtra("title") ?: "Upcoming Class"
        val message = intent.getStringExtra("message") ?: "Your class is starting soon"
        val userId = intent.getStringExtra("user_id")

        // Show notification
        NotificationUtils.showNotification(context, notificationId, title, message, userId)
    }

     */
}