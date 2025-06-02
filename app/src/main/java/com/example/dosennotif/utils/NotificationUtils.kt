package com.example.dosennotif.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.R
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.repository.ScheduleRepository
import com.example.dosennotif.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object NotificationUtils {
    private const val CHANNEL_ID = "schedule_notification_channel"
    private const val CHANNEL_NAME = "Schedule Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for upcoming teaching schedules"

    private val repository = ScheduleRepository()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Schedule a notification for a specific time
    fun scheduleNotification(context: Context, schedule: Schedule, delayMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val startTimeCalendar = Calendar.getInstance().apply {
            val hour = schedule.jam_mulai.split(":")[0].toInt()
            val minute = schedule.jam_mulai.split(":")[1].toInt()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val dayOfWeekFromSchedule = schedule.getDayOfWeekNumber()
            val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToAdd = if (dayOfWeekFromSchedule >= currentDayOfWeek) {
                dayOfWeekFromSchedule - currentDayOfWeek
            } else {
                7 - (currentDayOfWeek - dayOfWeekFromSchedule)
            }

            add(Calendar.DAY_OF_YEAR, daysToAdd)
        }

        val notificationTime = startTimeCalendar.clone() as Calendar
        notificationTime.add(Calendar.MINUTE, -delayMinutes)

        val systemNow = System.currentTimeMillis()
        val timeDiff = notificationTime.timeInMillis - systemNow

        // Format waktu dalam bentuk yang mudah dibaca
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val readableNotificationTime = formatter.format(notificationTime.time)
        val readableSystemTime = formatter.format(systemNow)

        Log.d("NotificationUtils", "System time        : $readableSystemTime")
        Log.d("NotificationUtils", "Notification time  : $readableNotificationTime")
        Log.d("NotificationUtils", "Trigger in (ms)    : $timeDiff")

        val MIN_TRIGGER_DELAY_MS = 15_000L // 5 detik

        if (timeDiff < MIN_TRIGGER_DELAY_MS) {
            Log.w("NotificationUtils", "Rescheduling: alarm too close or already passed for ${schedule.nama_mata_kuliah}, pushing +5 minutes")

            // Geser alarm +5 menit ke depan dari sekarang
            notificationTime.timeInMillis = systemNow + 2 * 60 * 1000 // 5 menit
            startTimeCalendar.timeInMillis = notificationTime.timeInMillis + delayMinutes * 60 * 1000

            val updatedTimeDiff = notificationTime.timeInMillis - System.currentTimeMillis()
            if (updatedTimeDiff < MIN_TRIGGER_DELAY_MS) {
                Log.w("NotificationUtils", "Skipping: even adjusted alarm is too close for ${schedule.nama_mata_kuliah}")
                return
            }
        }


        val notificationId = UUID.randomUUID().toString()

        val intent = Intent(context, MainActivity::class.java).apply {
            // Bisa tambah extra untuk navigate ke notification tab
            putExtra("navigate_to", "notification")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", "Upcoming Class: ${schedule.nama_mata_kuliah}")
            putExtra("message", "You have ${schedule.nama_mata_kuliah} class in $delayMinutes minutes at room ${schedule.ruang}")
            putExtra("user_id", currentUser.uid)
            putExtra("schedule_id", "${schedule.id_dosen}_${schedule.kode_mata_kuliah}_${schedule.kelas}")
            putExtra("scheduled_time", notificationTime.timeInMillis)
            putExtra("actual_schedule_time", startTimeCalendar.timeInMillis)
            putExtra("room", schedule.ruang)
            putExtra("course_name", schedule.nama_mata_kuliah)
            putExtra("class_name", schedule.kelas)
        }

        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTime.timeInMillis,
                alarmPendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTime.timeInMillis,
                alarmPendingIntent
            )
        }

        Log.d("NotificationUtils", "✅ Scheduled alarm for ${schedule.nama_mata_kuliah} at $readableNotificationTime")
    }

    // Show a notification immediately
    fun showNotification(
        context: Context,
        notificationId: String,
        title: String,
        message: String,
        userId: String? = null,
        scheduleId: String? = null,
        scheduledTime: Long? = null,
        actualScheduleTime: Long? = null,
        room: String? = null,
        courseName: String? = null,
        className: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent for notification tap action
        val intent = Intent(context, MainActivity::class.java).apply {
            // Bisa tambah extra untuk navigate ke notification tab
            putExtra("navigate_to", "notification")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Create pending intent for notification tap
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create snooze intent
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("user_id", userId)
        }

        // Create pending intent for snooze action
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (notificationId + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create dismiss intent
        val dismissIntent = Intent(context, DismissReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
        }

        // Create pending intent for dismiss action
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (notificationId + "_dismiss").hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .addAction(R.drawable.ic_snooze, "Snooze 10 min", snoozePendingIntent)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)

        // Show notification
        notificationManager.notify(notificationId.hashCode(), notificationBuilder.build())

        // SAVE TO DATABASE - Only when notification is actually shown
        userId?.let { uid ->
            val notificationData = ScheduleNotification(
                id = notificationId,
                scheduleId = scheduleId ?: "",
                title = title,
                message = message,
                scheduledTime = scheduledTime ?: System.currentTimeMillis(),
                actualScheduleTime = actualScheduleTime ?: System.currentTimeMillis(),
                room = room ?: "",
                courseName = courseName ?: extractCourseNameFromTitle(title),
                className = className ?: "",
                isRead = false,
                createdAt = System.currentTimeMillis()  // When notification actually appeared
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.saveNotification(uid, notificationData)
                    Log.d("NotificationUtils", "Notification saved to history: $notificationId")
                } catch (e: Exception) {
                    Log.e("NotificationUtils", "Failed to save notification to history: ${e.message}")
                }
            }
        }

        // Mark notification as shown in Firestore (if needed for other purposes)
        userId?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(it)
                .collection("notifications")
                .document(notificationId)
                .update("isShown", true)
                .addOnFailureListener { e ->
                    Log.w("NotificationUtils", "Failed to update isShown flag: ${e.message}")
                }
        }
    }

    // Helper function to extract course name from title
    private fun extractCourseNameFromTitle(title: String): String {
        return if (title.startsWith("Upcoming Class: ")) {
            title.substring("Upcoming Class: ".length)
        } else {
            title
        }
    }
}
//pengecekan agar tidak terkirim terus menerus
