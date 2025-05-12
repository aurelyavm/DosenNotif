package com.example.dosennotif.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    // StateFlow for notifications
    private val _notificationsState = MutableStateFlow<Resource<List<ScheduleNotification>>>(Resource.Loading)
    val notificationsState = _notificationsState.asStateFlow()

    // LiveData for selected notification
    private val _selectedNotification = MutableLiveData<ScheduleNotification?>()
    val selectedNotification: LiveData<ScheduleNotification?> = _selectedNotification

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

    fun getNotificationById(notificationId: String) {
        viewModelScope.launch {
            val currentState = _notificationsState.value

            if (currentState is Resource.Success) {
                val notification = currentState.data.find { it.id == notificationId }
                _selectedNotification.value = notification

                // Mark notification as read
                notification?.let { markNotificationAsRead(it.id) }
            } else {
                // Load notifications first, then get the notification
                loadNotifications()

                val newState = _notificationsState.value
                if (newState is Resource.Success) {
                    val notification = newState.data.find { it.id == notificationId }
                    _selectedNotification.value = notification

                    // Mark notification as read
                    notification?.let { markNotificationAsRead(it.id) }
                }
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            repository.markNotificationAsRead(currentUser.uid, notificationId)

            // Update the notifications state to reflect the change
            loadNotifications()
        }
    }

    fun clearSelectedNotification() {
        _selectedNotification.value = null
    }

    // Get unread notification count
    fun getUnreadCount(): Int {
        val currentState = _notificationsState.value

        return if (currentState is Resource.Success) {
            currentState.data.count { !it.isRead }
        } else {
            0
        }
    }
}