package com.example.dosennotif.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.model.User
import com.example.dosennotif.repository.ScheduleRepository
import com.example.dosennotif.utils.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class ProfileViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = ScheduleRepository()

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _userSchedules = MutableLiveData<List<Schedule>>()
    val userSchedules: LiveData<List<Schedule>> = _userSchedules

    private val _nextClass = MutableLiveData<Schedule?>()
    val nextClass: LiveData<Schedule?> = _nextClass

    init {
        loadUserData()
    }

    fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val document = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    _userData.value = user

                    user?.nidn?.let { loadSchedules(it) }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun loadSchedules(nidn: String, period: String = "20242") {
        viewModelScope.launch {
            when (val result = repository.getLecturerSchedule(nidn, period)) {
                is Resource.Success -> {
                    _userSchedules.value = result.data
                    findNextClass(result.data)
                }
                is Resource.Error -> {
                    _userSchedules.value = emptyList()
                    _nextClass.value = null
                }
                else -> {}
            }
        }
    }

    private fun findNextClass(schedules: List<Schedule>) {
        val now = Calendar.getInstance()
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val currentTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        var nextClass: Schedule? = null
        var minTimeDiff = Int.MAX_VALUE

        schedules.forEach { schedule ->
            val scheduleDayOfWeek = schedule.getDayOfWeekNumber()
            val (hour, minute) = schedule.jam_mulai.split(":").map { it.toInt() }
            val scheduleTimeMinutes = hour * 60 + minute

            val daysUntil = when {
                scheduleDayOfWeek > currentDayOfWeek -> scheduleDayOfWeek - currentDayOfWeek
                scheduleDayOfWeek < currentDayOfWeek -> (7 - currentDayOfWeek) + scheduleDayOfWeek
                else -> {
                    if (scheduleTimeMinutes > currentTime) 0 else 7
                }
            }

            val totalMinutesUntil = if (daysUntil == 0) {
                scheduleTimeMinutes - currentTime
            } else {
                (daysUntil * 24 * 60) + scheduleTimeMinutes - currentTime
            }

            if (totalMinutesUntil > 0 && totalMinutesUntil < minTimeDiff) {
                minTimeDiff = totalMinutesUntil
                nextClass = schedule
            }
        }

        _nextClass.value = nextClass
    }

    fun refreshSchedule() {
        val user = _userData.value
        user?.nidn?.let { loadSchedules(it) }
    }

    fun signOut() {
        auth.signOut()
    }
}