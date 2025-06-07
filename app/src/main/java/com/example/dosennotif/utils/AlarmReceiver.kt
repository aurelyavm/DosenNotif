package com.example.dosennotif.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getStringExtra("notification_id") ?: return
        val title = intent.getStringExtra("title") ?: "Upcoming Class"
        val message = intent.getStringExtra("message") ?: "Your class is starting soon"
        val userId = intent.getStringExtra("user_id")

        val scheduleId = intent.getStringExtra("schedule_id")
        val scheduledTime = intent.getLongExtra("scheduled_time", 0L)
        val actualScheduleTime = intent.getLongExtra("actual_schedule_time", 0L)
        val room = intent.getStringExtra("room")
        val courseName = intent.getStringExtra("course_name")
        val className = intent.getStringExtra("class_name")

        val now = System.currentTimeMillis()

        if (scheduledTime == 0L || now >= scheduledTime) {
            NotificationUtils.showNotification(
                context, notificationId, title, message, userId,
                scheduleId, scheduledTime, actualScheduleTime, room, courseName, className
            )
        } else {
            Log.d("AlarmReceiver", "⏳ Alarm terlalu awal. Sekarang=$now, Dijadwalkan=$scheduledTime")
        }
    }
}
