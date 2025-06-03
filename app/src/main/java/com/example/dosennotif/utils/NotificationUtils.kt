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
import kotlinx.coroutines.tasks.await

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

    val dayOfWeek = schedule.getDayOfWeekNumber()
    val scheduleHour = schedule.jam_mulai.split(":")[0].toInt()
    val scheduleMinute = schedule.jam_mulai.split(":")[1].toInt()

    val startTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, scheduleHour)
        set(Calendar.MINUTE, scheduleMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        val today = get(Calendar.DAY_OF_WEEK)
        val daysUntil = (dayOfWeek + 7 - today) % 7
        add(Calendar.DAY_OF_YEAR, if (daysUntil == 0 && timeInMillis < System.currentTimeMillis()) 7 else daysUntil)
    }

    val notifyTime = (startTime.clone() as Calendar).apply {
        add(Calendar.MINUTE, -delayMinutes)
    }

    val now = System.currentTimeMillis()
    val triggerIn = notifyTime.timeInMillis - now

    // Skip scheduling if notification is too close
    if (triggerIn < 15_000L) {
        Log.w("NotificationUtils", "⏩ Skip schedule: notification for ${schedule.nama_mata_kuliah} is too close or already passed.")
        return
    }

    // Cek apakah sudah pernah dijadwalkan berdasarkan scheduleId
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val db = FirebaseFirestore.getInstance()
            val scheduleId = "${schedule.id_dosen}_${schedule.kode_mata_kuliah}_${schedule.kelas}"
            val snapshot = db.collection("users")
                .document(currentUser.uid)
                .collection("notifications")
                .whereEqualTo("scheduleId", scheduleId)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                Log.d("NotificationUtils", "❌ Jadwal sudah pernah dijadwalkan: $scheduleId")
                return@launch
            }

            // Schedule alarm
            val notificationId = UUID.randomUUID().toString()

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("notification_id", notificationId)
                putExtra("title", "Upcoming Class: ${schedule.nama_mata_kuliah}")
                putExtra("message", "You have ${schedule.nama_mata_kuliah} class in $delayMinutes minutes at room ${schedule.ruang}")
                putExtra("user_id", currentUser.uid)
                putExtra("schedule_id", scheduleId)
                putExtra("scheduled_time", notifyTime.timeInMillis)
                putExtra("actual_schedule_time", startTime.timeInMillis)
                putExtra("room", schedule.ruang)
                putExtra("course_name", schedule.nama_mata_kuliah)
                putExtra("class_name", schedule.kelas)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId.hashCode(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notifyTime.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notifyTime.timeInMillis,
                    pendingIntent
                )
            }

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d("NotificationUtils", "✅ Alarm dijadwalkan: ${formatter.format(notifyTime.time)} untuk ${schedule.nama_mata_kuliah}")

        } catch (e: Exception) {
            Log.e("NotificationUtils", "❌ Gagal menjadwalkan notifikasi: ${e.message}")
        }
    }
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

    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("navigate_to", "notification")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
        putExtra("notification_id", notificationId)
        putExtra("title", title)
        putExtra("message", message)
        putExtra("user_id", userId)
    }

    val snoozePendingIntent = PendingIntent.getBroadcast(
        context,
        (notificationId + "_snooze").hashCode(),
        snoozeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val dismissIntent = Intent(context, DismissReceiver::class.java).apply {
        putExtra("notification_id", notificationId)
    }

    val dismissPendingIntent = PendingIntent.getBroadcast(
        context,
        (notificationId + "_dismiss").hashCode(),
        dismissIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

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

    notificationManager.notify(notificationId.hashCode(), notificationBuilder.build())

    // Cek & simpan hanya jika belum ada
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
            createdAt = System.currentTimeMillis()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("users")
                    .document(uid)
                    .collection("notifications")
                    .whereEqualTo("scheduleId", scheduleId)
                    .whereEqualTo("actualScheduleTime", actualScheduleTime)
                    .limit(1)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    Log.d("NotificationUtils", "❌ Sudah ada notifikasi untuk scheduleId=$scheduleId & time=$actualScheduleTime")
                    return@launch
                }

                repository.saveNotification(uid, notificationData)
                Log.d("NotificationUtils", "✅ Notifikasi disimpan ke Firestore: $notificationId")
            } catch (e: Exception) {
                Log.e("NotificationUtils", "❌ Gagal simpan notifikasi: ${e.message}")
            }
        }
    }

    // Tandai sebagai sudah ditampilkan (opsional)
    userId?.let {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(it)
            .collection("notifications")
            .document(notificationId)
            .update("isShown", true)
            .addOnFailureListener { e ->
                Log.w("NotificationUtils", "Gagal update flag isShown: ${e.message}")
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
