package com.example.dosennotif.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.api.ApiClient
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.LinkedHashSet

class ScheduleRepository {
    private val apiService = ApiClient.create()
    private val firestore = FirebaseFirestore.getInstance()

    // Department IDs for Faculty of Computer Science
    private val facultyDepartmentIds = listOf(
        "3",  // S1 Informatika
        "4",  // S1 Sistem Informasi
        "6",  // DIII Sistem Informasi
        "58"  // S1 Sains Data
    )

    // Fetch lecturer schedule from API
    suspend fun getLecturerSchedule(lecturerNidn: String, period: String = "20242"): Resource<List<Schedule>> {
        return withContext(Dispatchers.IO) {
            try {
                // Create result set to handle duplicate schedules
                val uniqueSchedules = LinkedHashSet<Schedule>()

                // Fetch schedules for each department
                for (departmentId in facultyDepartmentIds) {
                    try {
                        val requestBody = mapOf(
                            "id_periode" to period,
                            "id_program_studi" to departmentId
                        )

                        val response = apiService.getLecturerSchedule(requestBody)

                        // Check if response data is null
                        if (response.data == null) {
                            continue
                        }
                        // Filter schedules for this lecturer and add to result set
                        response.data
                        .filter { it.nidn_dosen == lecturerNidn }
                        .forEach { uniqueSchedules.add(it) }
                    } catch (e: Exception) {
                        // Log error but continue with other departments
                        e.printStackTrace()
                    }
                }

                // Convert to list and return
                // If no schedules found, return empty list instead of null
                Resource.Success(uniqueSchedules.toList())
            } catch (e: Exception) {
                Resource.Error(e.message ?: "An error occurred fetching schedules")
            }
        }
    }

    // Save schedule notification to Firestore
    suspend fun saveNotification(userId: String, notification: ScheduleNotification): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
                val notificationRef = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(notification.id)

                notificationRef.set(notification).await()
                Resource.Success(notification.id)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to save notification")
            }
        }
    }

    // Get all notifications for a user
    suspend fun getUserNotifications(userId: String): Resource<List<ScheduleNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(ScheduleNotification::class.java)

                Resource.Success(notifications)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to get notifications")
            }
        }
    }

    // Mark notification as read
    suspend fun markNotificationAsRead(userId: String, notificationId: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(notificationId)
                    .update("isRead", true)
                    .await()

                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to mark notification as read")
            }
        }
    }
}