package com.example.dosennotif.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.model.User
import com.example.dosennotif.repository.ScheduleRepository
import com.example.dosennotif.utils.LocationUtils
import com.example.dosennotif.utils.NotificationUtils
import com.example.dosennotif.utils.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RealtimeScheduleService : Service() {

    private val repository = ScheduleRepository()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ✅ ADD: Tracking untuk prevent duplicate scheduling
    private val scheduledNotifications = mutableSetOf<String>()
    private var lastScheduleTime = 0L
    private var lastUserNidn: String? = null

    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createNotificationChannel(applicationContext)

        createForegroundNotificationChannel()

        startForeground(
            NOTIF_ID,
            buildForegroundNotification("Realtime schedule checking running...")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            runRealtimeCheckLoop()
        }
        return START_STICKY
    }

    // ✅ FIXED: Prevent duplicate scheduling
    private suspend fun runRealtimeCheckLoop() {
        Log.d("AuthCheck", "User: ${FirebaseAuth.getInstance().currentUser}")

        while (serviceScope.isActive) {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                Log.d("currentUser", "${currentUser}")

                if (currentUser != null) {
                    val location: Location? = LocationUtils.getLastLocation(applicationContext)
                    val userData: User? = getUserData(currentUser.uid)
                    val nidn = userData?.nidn

                    if (nidn != null && location != null) {
                        // ✅ CHECK: Skip jika baru saja di-schedule untuk user yang sama
                        val now = System.currentTimeMillis()
                        if (nidn == lastUserNidn && (now - lastScheduleTime) < SCHEDULE_COOLDOWN_MS) {
                            Log.d("RealtimeScheduleService", "⏳ Cooldown active for user $nidn, skipping...")
                            delay(CHECK_INTERVAL_MS)
                            continue
                        }

                        val scheduleResult = repository.getLecturerSchedule(nidn)
                        Log.d("schedule result", "runRealtimeCheckLoop:${scheduleResult}")

                        if (scheduleResult is Resource.Success) {
                            val distance = LocationUtils.calculateDistanceFromCampus(
                                location.latitude,
                                location.longitude
                            )
                            val delayMinutes = LocationUtils.getNotificationDelay(distance)

                            var newScheduleCount = 0
                            var skipCount = 0

                            scheduleResult.data.forEach { schedule ->
                                Log.d("data schedule", "schedule ${schedule}")

                                // ✅ CREATE UNIQUE KEY untuk setiap schedule
                                val scheduleKey = "${schedule.id_dosen}_${schedule.kode_mata_kuliah}_${schedule.kelas}_${schedule.ruang}_${schedule.hari}_${schedule.jam_mulai}"

                                // ✅ ONLY SCHEDULE IF NOT ALREADY SCHEDULED
                                if (!scheduledNotifications.contains(scheduleKey)) {
                                    NotificationUtils.scheduleNotification(
                                        applicationContext,
                                        schedule,
                                        delayMinutes
                                    )
                                    scheduledNotifications.add(scheduleKey)
                                    newScheduleCount++
                                    Log.d("RealtimeScheduleService", "✅ Scheduled: ${schedule.nama_mata_kuliah}")
                                } else {
                                    skipCount++
                                    Log.d("RealtimeScheduleService", "⏭️ Skipped (already scheduled): ${schedule.nama_mata_kuliah}")
                                }
                            }

                            // ✅ UPDATE tracking variables
                            lastScheduleTime = now
                            lastUserNidn = nidn

                            Log.d("RealtimeScheduleService", "📊 Summary: ${newScheduleCount} new, ${skipCount} skipped for user $nidn")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RealtimeScheduleService", "Error in realtime loop", e)
            }

            // ✅ INCREASED INTERVAL untuk reduce frequency
            delay(CHECK_INTERVAL_MS)
        }
    }

    fun getTimeXMinutesFromNow(minutes: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutes)
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(cal.time)
    }

    fun getTodayName(): String {
        val format = SimpleDateFormat("EEEE", Locale("id", "ID"))
        return format.format(Calendar.getInstance().time)
    }

    private suspend fun getUserData(userId: String): User? {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        // ✅ CLEAR tracking saat service destroyed
        scheduledNotifications.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Realtime Schedule Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(content: String): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Dosennotif Service")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "realtime_schedule_service_channel"
        private const val NOTIF_ID = 1
        // ✅ INCREASED from 10 seconds to 60 seconds
        private const val CHECK_INTERVAL_MS = 60_000L
        // ✅ ADD: Cooldown untuk prevent rapid re-scheduling
        private const val SCHEDULE_COOLDOWN_MS = 300_000L // 5 minutes
    }
}