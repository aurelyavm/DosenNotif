package com.example.dosennotif.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.repository.ScheduleRepository
import com.example.dosennotif.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = ScheduleRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _notificationsState = MutableStateFlow<Resource<List<ScheduleNotification>>>(Resource.Loading)
    val notificationsState = _notificationsState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            _notificationsState.value = Resource.Loading

            when (val result = repository.getUserNotifications(currentUser.uid)) {
                is Resource.Success -> {
                    _notificationsState.value = result
                }
                is Resource.Error -> {
                    _notificationsState.value = result
                }
                else -> {}
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            repository.markNotificationAsRead(currentUser.uid, notificationId)

            loadNotifications()
        }
    }

    fun getUnreadCount(): Int {
        val currentState = _notificationsState.value

        return if (currentState is Resource.Success) {
            currentState.data.count { !it.isRead }
        } else {
            0
        }
    }
}