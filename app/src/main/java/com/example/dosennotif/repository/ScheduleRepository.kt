package com.example.dosennotif.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.api.ApiClient
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.utils.Resource
import com.example.dosennotif.utils.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.LinkedHashSet
import java.util.Calendar
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody

class ScheduleRepository(private val context: Context? = null) {
    private val apiService = ApiClient.create()
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "ScheduleRepository"
    private val facultyDepartmentIds = listOf("3", "4", "6", "58")

    suspend fun getLecturerSchedule(lecturerNidn: String, period: String = "20242"): Resource<List<Schedule>> {
        return withContext(Dispatchers.IO) {
            try {
                val appConfig = context?.let { AppConfig.getInstance(it) }

                // 1. JIKA MODE DEMO AKTIF, LANGSUNG MOCK DATA
                if (appConfig?.useMockData == true) {
                    Log.d(TAG, "🎭 Mode Demo - menggunakan mock data")
                    val mockData = getMockScheduleData(lecturerNidn)
                    return@withContext Resource.Success(mockData)
                }

                // 2. COBA API DULU
                Log.d(TAG, "🔄 Mencoba mengambil data dari API...")
                val apiResult = tryFetchFromAPI(lecturerNidn, period, appConfig?.apiTimeout ?: 15000L)

                if (apiResult.isNotEmpty()) {
                    Log.d(TAG, "✅ API berhasil: ${apiResult.size} jadwal")
                    appConfig?.lastApiSuccess = System.currentTimeMillis()
                    return@withContext Resource.Success(apiResult)
                }

                // 3. JIKA API GAGAL DAN OFFLINE MODE AKTIF, COBA ASSETS BACKUP DULU
                if (appConfig?.enableOfflineMode != false) {
                    Log.w(TAG, "⚠️ API gagal, mencoba Assets backup...")
                    val assetsBackupResult = tryFetchFromAssetsBackup(lecturerNidn, period)

                    if (assetsBackupResult.isNotEmpty()) {
                        Log.d(TAG, "✅ Assets backup berhasil: ${assetsBackupResult.size} jadwal")
                        return@withContext Resource.Success(assetsBackupResult)
                    }

                    // 4. FALLBACK KE INDIVIDUAL BACKUP (untuk compatibility)
                    Log.w(TAG, "⚠️ Assets backup gagal, mencoba individual backup...")
                    val individualBackupResult = tryFetchFromIndividualBackup(lecturerNidn, period)

                    if (individualBackupResult.isNotEmpty()) {
                        Log.d(TAG, "✅ Individual backup berhasil: ${individualBackupResult.size} jadwal")
                        return@withContext Resource.Success(individualBackupResult)
                    }

                    // 5. JIKA SEMUA GAGAL, GUNAKAN MOCK DATA
                    Log.w(TAG, "⚠️ Semua backup gagal, menggunakan mock data...")
                    val mockData = getMockScheduleData(lecturerNidn)
                    return@withContext Resource.Success(mockData)
                } else {
                    return@withContext Resource.Error("Server tidak dapat dijangkau dan mode offline dinonaktifkan")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}")
                // Fallback terakhir: mock data jika offline mode aktif
                val appConfig = context?.let { AppConfig.getInstance(it) }
                if (appConfig?.enableOfflineMode != false) {
                    val mockData = getMockScheduleData(lecturerNidn)
                    Resource.Success(mockData)
                } else {
                    Resource.Error(e.message ?: "Terjadi kesalahan")
                }
            }
        }
    }

    // ======================================
    // FUNGSI BARU: BACA ASSETS BACKUP
    // ======================================

    /**
     * Baca dari api_responses collection (data yang di-upload dari assets)
     */
    private suspend fun tryFetchFromAssetsBackup(lecturerNidn: String, period: String): List<Schedule> {
        return try {
            Log.d(TAG, "📦 Trying to fetch from assets backup for period $period...")

            val docRef = firestore.collection("api_responses")
                .document("periode_${period}")
                .get()
                .await()

            if (docRef.exists()) {
                Log.d(TAG, "📦 Found assets backup document")

                val data = docRef.get("data") as? Map<String, Any>
                val stats = docRef.get("stats") as? Map<String, Any>
                val source = docRef.get("source") as? String

                Log.d(TAG, "📊 Assets backup stats: $stats")
                Log.d(TAG, "📄 Source: $source")

                if (data != null) {
                    // Gabungkan semua prodi
                    val allSchedules = mutableListOf<Map<String, Any>>()

                    // Extract dari setiap prodi
                    val prodi3 = data["prodi_3"] as? List<Map<String, Any>> ?: emptyList()
                    val prodi4 = data["prodi_4"] as? List<Map<String, Any>> ?: emptyList()
                    val prodi6 = data["prodi_6"] as? List<Map<String, Any>> ?: emptyList()
                    val prodi58 = data["prodi_58"] as? List<Map<String, Any>> ?: emptyList()

                    allSchedules.addAll(prodi3)
                    allSchedules.addAll(prodi4)
                    allSchedules.addAll(prodi6)
                    allSchedules.addAll(prodi58)

                    Log.d(TAG, "📦 Total schedules in backup: ${allSchedules.size}")

                    // Filter by lecturer NIDN
                    val filteredSchedules = allSchedules.mapNotNull { scheduleMap ->
                        try {
                            if (scheduleMap["nidn_dosen"] == lecturerNidn) {
                                Schedule(
                                    id_dosen = scheduleMap["id_dosen"] as? String ?: "",
                                    nama_dosen = scheduleMap["nama_dosen"] as? String ?: "",
                                    nidn_dosen = scheduleMap["nidn_dosen"] as? String ?: "",
                                    id_periode = scheduleMap["id_periode"] as? String ?: "",
                                    id_program_studi = scheduleMap["id_program_studi"] as? String ?: "",
                                    nama_program_studi = scheduleMap["nama_program_studi"] as? String ?: "",
                                    kode_mata_kuliah = scheduleMap["kode_mata_kuliah"] as? String ?: "",
                                    nama_mata_kuliah = scheduleMap["nama_mata_kuliah"] as? String ?: "",
                                    sks = scheduleMap["sks"] as? String ?: "",
                                    kelas = scheduleMap["kelas"] as? String ?: "",
                                    hari = (scheduleMap["hari"] as? String ?: "").trim(), // Trim whitespace
                                    jam_mulai = scheduleMap["jam_mulai"] as? String ?: "",
                                    jam_selesai = scheduleMap["jam_selesai"] as? String ?: "",
                                    ruang = scheduleMap["ruang"] as? String ?: ""
                                )
                            } else null
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing schedule from assets backup: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "🎯 Filtered ${filteredSchedules.size} schedules for lecturer $lecturerNidn from assets backup")

                    // Log sample schedules untuk debugging
                    if (filteredSchedules.isNotEmpty()) {
                        filteredSchedules.take(3).forEach { schedule ->
                            Log.d(TAG, "📋 Sample: ${schedule.nama_mata_kuliah} (${schedule.hari.trim()} ${schedule.jam_mulai})")
                        }
                    }

                    filteredSchedules
                } else {
                    Log.w(TAG, "❌ No data field found in assets backup")
                    emptyList()
                }
            } else {
                Log.w(TAG, "❌ No assets backup found for period $period")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Assets backup fetch error: ${e.message}")
            emptyList()
        }
    }

    // ======================================
    // FUNGSI LAMA: TETAP ADA UNTUK COMPATIBILITY
    // ======================================

    /**
     * Baca dari API real
     */
    private suspend fun tryFetchFromAPI(lecturerNidn: String, period: String, timeoutMs: Long): List<Schedule> {
        return try {
            withTimeoutOrNull(timeoutMs) {
                val uniqueSchedules = LinkedHashSet<Schedule>()

                for (departmentId in facultyDepartmentIds) {
                    try {
                        val idProdiBody = departmentId.toRequestBody(MultipartBody.FORM)
                        val idPeriodeBody = period.toRequestBody(MultipartBody.FORM)

                        val response = apiService.getLecturerSchedule(idProdiBody, idPeriodeBody)

                        response.data?.let { schedules ->
                            val filtered = schedules.filter { it.nidn_dosen == lecturerNidn }
                            filtered.forEach { uniqueSchedules.add(it) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching from department $departmentId: ${e.message}")
                    }
                }

                uniqueSchedules.toList()
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "API fetch error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Baca dari schedule_backup collection (per dosen)
     * Tetap ada untuk backward compatibility
     */
    private suspend fun tryFetchFromIndividualBackup(lecturerNidn: String, period: String): List<Schedule> {
        return try {
            Log.d(TAG, "📦 Trying individual backup for $lecturerNidn...")

            val docRef = firestore.collection("schedule_backup")
                .document("${lecturerNidn}_${period}")
                .get()
                .await()

            if (docRef.exists()) {
                val scheduleData = docRef.get("schedules") as? List<Map<String, Any>>
                scheduleData?.mapNotNull { scheduleMap ->
                    try {
                        Schedule(
                            id_dosen = scheduleMap["id_dosen"] as? String ?: "",
                            nama_dosen = scheduleMap["nama_dosen"] as? String ?: "",
                            nidn_dosen = scheduleMap["nidn_dosen"] as? String ?: "",
                            id_periode = scheduleMap["id_periode"] as? String ?: "",
                            id_program_studi = scheduleMap["id_program_studi"] as? String ?: "",
                            nama_program_studi = scheduleMap["nama_program_studi"] as? String ?: "",
                            kode_mata_kuliah = scheduleMap["kode_mata_kuliah"] as? String ?: "",
                            nama_mata_kuliah = scheduleMap["nama_mata_kuliah"] as? String ?: "",
                            sks = scheduleMap["sks"] as? String ?: "",
                            kelas = scheduleMap["kelas"] as? String ?: "",
                            hari = scheduleMap["hari"] as? String ?: "",
                            jam_mulai = scheduleMap["jam_mulai"] as? String ?: "",
                            jam_selesai = scheduleMap["jam_selesai"] as? String ?: "",
                            ruang = scheduleMap["ruang"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing schedule: ${e.message}")
                        null
                    }
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Individual backup fetch error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Mock data sebagai fallback terakhir
     */
    private fun getMockScheduleData(lecturerNidn: String): List<Schedule> {
        val currentDay = getCurrentDayInIndonesian()

        return listOf(
            Schedule(
                id_dosen = "mock_001",
                nama_dosen = "Mock Lecturer",
                nidn_dosen = lecturerNidn,
                id_periode = "20242",
                id_program_studi = "3",
                nama_program_studi = "Teknik Informatika",
                kode_mata_kuliah = "MOCK001",
                nama_mata_kuliah = "Demo - Algoritma dan Pemrograman",
                sks = "3",
                kelas = "A",
                hari = currentDay,
                jam_mulai = getTimeAfterMinutes(30),
                jam_selesai = getTimeAfterMinutes(120),
                ruang = "Lab Demo 1"
            ),
            Schedule(
                id_dosen = "mock_001",
                nama_dosen = "Mock Lecturer",
                nidn_dosen = lecturerNidn,
                id_periode = "20242",
                id_program_studi = "3",
                nama_program_studi = "Teknik Informatika",
                kode_mata_kuliah = "MOCK002",
                nama_mata_kuliah = "Demo - Basis Data",
                sks = "3",
                kelas = "B",
                hari = currentDay,
                jam_mulai = getTimeAfterMinutes(180),
                jam_selesai = getTimeAfterMinutes(270),
                ruang = "Lab Demo 2"
            )
        )
    }

    private fun getCurrentDayInIndonesian(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Senin"
        }
    }

    private fun getTimeAfterMinutes(minutes: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, minutes)
        return String.format("%02d:%02d:00",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE))
    }

    // ======================================
    // FUNGSI EXISTING LAINNYA TETAP SAMA
    // ======================================

    suspend fun saveNotification(userId: String, notification: ScheduleNotification): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
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