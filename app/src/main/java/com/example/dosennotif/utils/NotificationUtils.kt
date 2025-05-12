package com.example.dosennotif.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.R
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.repository.ScheduleRepository
import com.example.dosennotif.ui.notification.NotificationDetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

object NotificationUtils {
    private const val CHANNEL_ID = "schedule_notification_channel"
    private const val CHANNEL_NAME = "Schedule Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for upcoming teaching schedules"

    private val repository = ScheduleRepository()

    // Create notification channel
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

        // Get current user
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Calculate notification time
        val startTimeCalendar = Calendar.getInstance().apply {
            // Parse schedule start time
            val hour = schedule.jam_mulai.split(":")[0].toInt()
            val minute = schedule.jam_mulai.split(":")[1].toInt()

            // Set calendar time to class start
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // Adjust to correct day of week
            val dayOfWeekFromSchedule = schedule.getDayOfWeekNumber()
            val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)

            // Calculate days to add
            val daysToAdd = if (dayOfWeekFromSchedule >= currentDayOfWeek) {
                dayOfWeekFromSchedule - currentDayOfWeek
            } else {
                7 - (currentDayOfWeek - dayOfWeekFromSchedule)
            }

            add(Calendar.DAY_OF_YEAR, daysToAdd)
        }

        // Subtract delay minutes for notification time
        val notificationTime = startTimeCalendar.clone() as Calendar
        notificationTime.add(Calendar.MINUTE, -delayMinutes)

        // Skip if notification time is in the past
        if (notificationTime.timeInMillis <= System.currentTimeMillis()) {
            Log.d("NotificationUtils", "Skipping notification for past schedule: ${schedule.nama_mata_kuliah}")
            return
        }

        // Create notification ID
        val notificationId = UUID.randomUUID().toString()

        // Create notification data
        val notificationData = ScheduleNotification(
            id = notificationId,
            scheduleId = schedule.id_dosen + "_" + schedule.kode_mata_kuliah + "_" + schedule.kelas,
            title = "Upcoming Class: ${schedule.nama_mata_kuliah}",
            message = "You have ${schedule.nama_mata_kuliah} class in ${delayMinutes} minutes at room ${schedule.ruang}",
            scheduledTime = notificationTime.timeInMillis,
            actualScheduleTime = startTimeCalendar.timeInMillis,
            room = schedule.ruang,
            courseName = schedule.nama_mata_kuliah,
            className = schedule.kelas,
            isRead = false
        )

        // Store notification data in Firestore
        CoroutineScope(Dispatchers.IO).launch {
            repository.saveNotification(currentUser.uid, notificationData)
        }

        // Create intent for notification tap action
        val intent = Intent(context, NotificationDetailActivity::class.java).apply {
            putExtra("notification_id", notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Create pending intent for notification tap
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create intent for alarm receiver
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", notificationData.title)
            putExtra("message", notificationData.message)
            putExtra("user_id", currentUser.uid)
        }

        // Create pending intent for alarm
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm
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

        Log.d("NotificationUtils", "Scheduled notification for ${schedule.nama_mata_kuliah} at ${notificationTime.time}")
    }

    // Show a notification immediately
    fun showNotification(
        context: Context,
        notificationId: String,
        title: String,
        message: String,
        userId: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent for notification tap action
        val intent = Intent(context, NotificationDetailActivity::class.java).apply {
            putExtra("notification_id", notificationId)
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

        // Mark notification as shown if userId is provided
        userId?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(it)
                .collection("notifications")
                .document(notificationId)
                .update("isShown", true)
        }
    }
}