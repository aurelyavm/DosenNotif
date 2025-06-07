package com.example.dosennotif.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.dosennotif.R
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object NotificationUtils {
    private const val CHANNEL_ID = "schedule_notification_channel"
    private const val CHANNEL_NAME = "Schedule Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for upcoming teaching schedules"

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

    fun scheduleNotification(context: Context, schedule: Schedule, delayMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val (hour, minute) = schedule.jam_mulai.split(":").map { it.toInt() }
        val dayOfWeek = schedule.getDayOfWeekNumber() // misalnya: 2 = Senin

        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
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
        if (notifyTime.timeInMillis - now < 15_000L) {
            Log.w("NotificationUtils", "⏩ Melewati notifikasi ${schedule.nama_mata_kuliah} karena sudah lewat/terlalu dekat.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val ruang = schedule.ruang ?: "unknown"
                val scheduleId = "${schedule.id_dosen}_${schedule.kode_mata_kuliah}_${schedule.kelas}_$ruang"
                val documentId = "${scheduleId}_${notifyTime.timeInMillis}"

                val docSnapshot = db.collection("users")
                    .document(currentUser.uid)
                    .collection("notifications")
                    .document(documentId)
                    .get()
                    .await()

                if (docSnapshot.exists()) {
                    Log.d("NotificationUtils", "❌ Duplikat notifikasi: $documentId")
                    return@launch
                }

                val notificationId = documentId

                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("notification_id", notificationId)
                    putExtra("title", "Upcoming Class: ${schedule.nama_mata_kuliah}")
                    putExtra("message", "You have ${schedule.nama_mata_kuliah} class in $delayMinutes minutes at room ${schedule.ruang}")
                    putExtra("user_id", currentUser.uid)
                    putExtra("schedule_id", scheduleId)
                    putExtra("scheduled_time", notifyTime.timeInMillis)
                    putExtra("actual_schedule_time", startTime.timeInMillis)
                    putExtra("room", ruang)
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
                Log.d("NotificationUtils", "✅ Notifikasi ${schedule.nama_mata_kuliah} dijadwalkan: ${formatter.format(notifyTime.time)}")

            } catch (e: Exception) {
                Log.e("NotificationUtils", "❌ Gagal menjadwalkan notifikasi: ${e.message}")
            }
        }
    }

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

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))

        notificationManager.notify(notificationId.hashCode(), notificationBuilder.build())

        userId?.let { uid ->
            val data = ScheduleNotification(
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

            val documentId = "${data.scheduleId}_${data.scheduledTime}"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("notifications")
                        .document(documentId)
                        .set(data)
                } catch (e: Exception) {
                    Log.e("NotificationUtils", "❌ Gagal simpan notifikasi: ${e.message}")
                }
            }
        }
    }

    private fun extractCourseNameFromTitle(title: String): String {
        return if (title.startsWith("Upcoming Class: ")) {
            title.substringAfter("Upcoming Class: ")
        } else title
    }
}
