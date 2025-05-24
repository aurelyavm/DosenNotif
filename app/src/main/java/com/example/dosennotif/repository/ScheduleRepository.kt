package com.example.dosennotif.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.api.ApiClient
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.LinkedHashSet
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody

class ScheduleRepository {
    private val apiService = ApiClient.create()
    private val firestore = FirebaseFirestore.getInstance()

    private val TAG = "ScheduleRepository"

    private val facultyDepartmentIds = listOf("3", "4", "6", "58")

    suspend fun getLecturerSchedule(lecturerNidn: String, period: String = "20242"): Resource<List<Schedule>> {
        return withContext(Dispatchers.IO) {
            try {
                val uniqueSchedules = LinkedHashSet<Schedule>()
                Log.d(TAG, "Fetching schedule for lecturer $lecturerNidn in period $period")

                for (departmentId in facultyDepartmentIds) {
                    try {
                        val idProdiBody = departmentId.toRequestBody(MultipartBody.FORM)
                        val idPeriodeBody = period.toRequestBody(MultipartBody.FORM)

                        Log.d(TAG, "Requesting schedule for departmentId=$departmentId")

                        val response = apiService.getLecturerSchedule(idProdiBody, idPeriodeBody)

                        if (response.data == null) {
                            Log.w(TAG, "No data received for departmentId=$departmentId")
                            continue
                        }

                        val filtered = response.data.filter { it.nidn_dosen == lecturerNidn }
                        Log.d(
                            TAG,
                            "Found ${filtered.size} schedules for lecturer in departmentId=$departmentId"
                        )

                        filtered.forEach { uniqueSchedules.add(it) }

                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error fetching schedule for departmentId=$departmentId: ${e.message}",
                            e
                        )
                    }
                }

                Log.d(TAG, "Total unique schedules found: ${uniqueSchedules.size}")
                Resource.Success(uniqueSchedules.toList())
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching schedules: ${e.message}", e)
                Resource.Error(e.message ?: "An error occurred fetching schedules")
            }
        }
    }

    // Di fungsi saveNotification(), tambahkan check duplikat:
    suspend fun saveNotification(userId: String, notification: ScheduleNotification): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if notification already exists
                val existingNotification = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(notification.id)
                    .get()
                    .await()

                if (existingNotification.exists()) {
                    Log.d(TAG, "Notification already exists, skipping save: ${notification.id}")
                    return@withContext Resource.Success(notification.id)
                }

                Log.d(TAG, "Saving notification for userId=$userId: $notification")

                val notificationRef = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(notification.id)

                notificationRef.set(notification).await()

                Log.d(TAG, "Notification saved with ID: ${notification.id}")
                Resource.Success(notification.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save notification: ${e.message}", e)
                Resource.Error(e.message ?: "Failed to save notification")
            }
        }
    }

    suspend fun getUserNotifications(userId: String): Resource<List<ScheduleNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching notifications for userId=$userId")

                val notifications = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(ScheduleNotification::class.java)
                Log.d(TAG,"data : ${notifications}")
                Log.d(TAG, "Fetched ${notifications.size} notifications")
                Resource.Success(notifications)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get notifications: ${e.message}", e)
                Resource.Error(e.message ?: "Failed to get notifications")
            }
        }
    }

    suspend fun markNotificationAsRead(userId: String, notificationId: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Marking notification as read: userId=$userId, notificationId=$notificationId")

                firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(notificationId)
                    .update("isRead", true)
                    .await()

                Log.d(TAG, "Notification marked as read")
                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark notification as read: ${e.message}", e)
                Resource.Error(e.message ?: "Failed to mark notification as read")
            }
        }
    }
}
