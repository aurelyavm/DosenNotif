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

    override fun onCreate() {
        super.onCreate()
        // Buat notification channel untuk alarm notifications
        NotificationUtils.createNotificationChannel(applicationContext)

        // Buat notification channel untuk foreground service
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
                        val scheduleResult = repository.getLecturerSchedule(nidn)
                        Log.d("schedule result", "runRealtimeCheckLoop:${scheduleResult} ")
                        if (scheduleResult is Resource.Success) {
                            val distance = LocationUtils.calculateDistanceFromCampus(
                                location.latitude,
                                location.longitude
                            )
                            val delayMinutes = LocationUtils.getNotificationDelay(distance)

                            scheduleResult.data.forEach { schedule ->
                                Log.d("data schedule","schedule ${schedule}");

                                // Schedule notification via NotificationUtils
                                NotificationUtils.scheduleNotification(
                                    applicationContext,
                                    schedule,
                                    delayMinutes
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RealtimeScheduleService", "Error in realtime loop", e)
            }
            delay(CHECK_INTERVAL_MS)
        }
    }

    // Fungsi untuk mendapatkan jam saat ini + X menit dalam format HH:mm:ss
    fun getTimeXMinutesFromNow(minutes: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutes)
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(cal.time)
    }

    // Fungsi untuk mendapatkan nama hari sesuai format di Schedule
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
        private const val CHECK_INTERVAL_MS = 10_000L // cek tiap 10 detik
    }
}