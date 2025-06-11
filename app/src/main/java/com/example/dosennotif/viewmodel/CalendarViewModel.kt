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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class CalendarViewModel : ViewModel() {
    private val repository = ScheduleRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val availablePeriods = listOf(
        Pair("20241", "2024 Semester 1 (Ganjil)"),
        Pair("20242", "2024 Semester 2 (Genap)")
    )

    private val _selectedPeriod = MutableLiveData("20242")
    val selectedPeriod: LiveData<String> = _selectedPeriod

    private val _scheduleState = MutableStateFlow<Resource<List<Schedule>>>(Resource.Loading)
    val scheduleState = _scheduleState.asStateFlow()

    private val _userData = MutableLiveData<User?>()

    private val _schedulesByDay = MutableLiveData<Map<String, List<Schedule>>>()
    val schedulesByDay: LiveData<Map<String, List<Schedule>>> = _schedulesByDay

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

                        user?.nidn?.let { loadSchedules(it, _selectedPeriod.value ?: "20242") }
                    }
                }
        }
    }

    fun loadSchedules(nidn: String, period: String) {
        viewModelScope.launch {
            _scheduleState.value = Resource.Loading
            _selectedPeriod.value = period

            when (val result = repository.getLecturerSchedule(nidn, period)) {
                is Resource.Success -> {
                    _scheduleState.value = result

                    groupSchedulesByDay(result.data)
                }
                is Resource.Error -> {
                    _scheduleState.value = result
                }
                else -> {}
            }
        }
    }

    fun selectPeriod(period: String) {
        _userData.value?.nidn?.let {
            loadSchedules(it, period)
        }
    }

    private fun groupSchedulesByDay(schedules: List<Schedule>?) {
        if (schedules.isNullOrEmpty()) {
            _schedulesByDay.postValue(emptyMap())
            return
        }
        val groupedSchedules = schedules.groupBy {
            it.getFormattedDay()
        }.mapValues { entry ->
            entry.value.sortedBy { it.getStartTime() }
        }

        val dayOrder = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

        val sortedGroupedSchedules = LinkedHashMap<String, List<Schedule>>()

        dayOrder.forEach { day ->
            groupedSchedules[day]?.let {
                sortedGroupedSchedules[day] = it
            }
        }

        _schedulesByDay.postValue(sortedGroupedSchedules)
    }

    fun getSchedulesForDay(day: String): List<Schedule> {
        return _schedulesByDay.value?.get(day) ?: emptyList()
    }

    fun getAllSchedules(): List<Schedule> {
        val currentState = _scheduleState.value

        return if (currentState is Resource.Success) {
            currentState.data
        } else {
            emptyList()
        }
    }
}