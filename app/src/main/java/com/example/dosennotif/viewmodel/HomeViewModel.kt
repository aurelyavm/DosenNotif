package com.example.dosennotif.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.User
import com.example.dosennotif.repository.ScheduleRepository
import com.example.dosennotif.utils.LocationUtils
import com.example.dosennotif.utils.NotificationUtils
import com.example.dosennotif.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScheduleRepository(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _distanceFromCampus = MutableLiveData<Float>()
    val distanceFromCampus: LiveData<Float> = _distanceFromCampus

    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _scheduleState = MutableStateFlow<Resource<List<Schedule>>>(Resource.Loading)
    val scheduleState = _scheduleState.asStateFlow()

    private val _todaySchedules = MutableLiveData<List<Schedule>>()
    val todaySchedules: LiveData<List<Schedule>> = _todaySchedules

    private val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    // ✅ ADD: Tracking untuk prevent duplicate scheduling
    private var lastNotificationSchedule = 0L
    private var lastScheduledNidn: String? = null

    init {
        loadUserData()
    }

    fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val user = document.toObject(User::class.java)
                        _userData.value = user

                        user?.nidn?.let { loadSchedules(it) }
                    }
                }
        }
    }

    fun loadSchedules(nidn: String, period: String = "20242") {
        viewModelScope.launch {
            _scheduleState.value = Resource.Loading

            when (val result = repository.getLecturerSchedule(nidn, period)) {
                is Resource.Success -> {
                    _scheduleState.value = result

                    filterTodaySchedules(result.data)

                    // ✅ ONLY schedule notifications on initial load, not on every location update
                    scheduleNotificationsOnLoad(result.data, nidn)
                }
                is Resource.Error -> {
                    _scheduleState.value = result
                }
                else -> {}
            }
        }
    }

    private fun filterTodaySchedules(schedules: List<Schedule>) {
        val dayMapping = mapOf(
            Calendar.SUNDAY to "Minggu",
            Calendar.MONDAY to "Senin",
            Calendar.TUESDAY to "Selasa",
            Calendar.WEDNESDAY to "Rabu",
            Calendar.THURSDAY to "Kamis",
            Calendar.FRIDAY to "Jumat",
            Calendar.SATURDAY to "Sabtu"
        )

        val todayDayName = dayMapping[currentDayOfWeek]?.lowercase(Locale.getDefault())

        if (schedules.isNullOrEmpty()) {
            _todaySchedules.postValue(emptyList())
            return
        }
        val filteredSchedules = schedules.filter { schedule ->
            schedule.getFormattedDay().lowercase(Locale.getDefault()).contains(todayDayName ?: "")
        }.sortedBy { schedule ->
            schedule.getStartTime()
        }
        Log.d("filterToday", "${filteredSchedules}")
        _todaySchedules.postValue(filteredSchedules)
    }

    // ✅ NEW: Schedule notifications only on initial load
    private fun scheduleNotificationsOnLoad(schedules: List<Schedule>, nidn: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // ✅ CHECK: Skip jika baru saja scheduled untuk user yang sama
            if (nidn == lastScheduledNidn && (now - lastNotificationSchedule) < SCHEDULE_COOLDOWN) {
                Log.d("HomeViewModel", "⏳ Cooldown active for user $nidn, skipping notification scheduling")
                return@launch
            }

            _currentLocation.value?.let { location ->
                val distance = LocationUtils.calculateDistanceFromCampus(
                    location.latitude,
                    location.longitude
                )

                _distanceFromCampus.postValue(distance)
                val delayMinutes = LocationUtils.getNotificationDelay(distance)

                schedules.forEach { schedule ->
                    NotificationUtils.scheduleNotification(
                        getApplication(),
                        schedule,
                        delayMinutes
                    )
                }

                // ✅ UPDATE tracking
                lastNotificationSchedule = now
                lastScheduledNidn = nidn

                Log.d("HomeViewModel", "✅ Notifications scheduled for user $nidn with ${delayMinutes}min delay")
            } ?: run {
                Log.d("HomeViewModel", "⚠️ No location available, notifications will be scheduled when location is available")
            }
        }
    }

    // ✅ MODIFIED: Only update distance, don't reschedule notifications
    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location

        val distance = LocationUtils.calculateDistanceFromCampus(
            location.latitude,
            location.longitude
        )

        _distanceFromCampus.postValue(distance)

        // ✅ REMOVED: Don't reschedule notifications on every location update
        // scheduleNotifications(state.data) <- This was causing spam!

        // ✅ ONLY schedule if we haven't scheduled yet and have schedule data
        _scheduleState.value.let { state ->
            if (state is Resource.Success && lastNotificationSchedule == 0L) {
                _userData.value?.nidn?.let { nidn ->
                    scheduleNotificationsOnLoad(state.data, nidn)
                }
            }
        }

        Log.d("HomeViewModel", "📍 Location updated: ${String.format("%.2f km", distance)} from campus")
    }

    fun getFormattedDistance(): String {
        val distance = _distanceFromCampus.value ?: 0f
        return String.format("%.2f km", distance)
    }

    companion object {
        // ✅ ADD: Cooldown untuk prevent rapid scheduling
        private const val SCHEDULE_COOLDOWN = 300_000L // 5 minutes
    }
}