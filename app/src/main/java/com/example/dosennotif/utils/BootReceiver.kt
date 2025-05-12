package com.example.dosennotif.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.model.User
import com.example.dosennotif.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * BroadcastReceiver to reschedule notifications after device restart
 */
class BootReceiver : BroadcastReceiver() {

    private val repository = ScheduleRepository()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        // Get current user
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Coroutine scope for async operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get last known location
                val location = LocationUtils.getLastLocation(context)

                // Get user's NIDN from Firestore
                val userData = getUserData(currentUser.uid)
                val nidn = userData?.nidn ?: return@launch

                // Get schedule
                val scheduleResult = repository.getLecturerSchedule(nidn)

                if (scheduleResult is Resource.Success && location != null) {
                    // Calculate distance from campus
                    val distance = LocationUtils.calculateDistanceFromCampus(
                        location.latitude,
                        location.longitude
                    )

                    // Get notification delay based on distance
                    val delayMinutes = LocationUtils.getNotificationDelay(distance)

                    // Reschedule notifications
                    scheduleResult.data.forEach { schedule ->
                        NotificationUtils.scheduleNotification(
                            context,
                            schedule,
                            delayMinutes
                        )
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    private suspend fun getUserData(userId: String): User? {
        return try {
            val document = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}