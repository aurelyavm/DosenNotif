package com.example.dosennotif.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.model.NotificationPreferences
import com.example.dosennotif.model.User
import com.example.dosennotif.utils.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData for user data
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    // LiveData for saving preferences state
    private val _savingState = MutableLiveData<Resource<Unit>>()
    val savingState: LiveData<Resource<Unit>> = _savingState

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
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        val currentUser = auth.currentUser ?: return

        _savingState.value = Resource.Loading

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("notificationPreferences", preferences)
                    .await()

                // Update local user data
                val updatedUser = _userData.value?.copy(notificationPreferences = preferences)
                _userData.value = updatedUser

                _savingState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _savingState.value = Resource.Error(e.message ?: "Failed to update preferences")
            }
        }
    }

    fun updateDistanceThreshold(range: String, minutes: Int) {
        val currentUser = auth.currentUser ?: return
        val currentPreferences = _userData.value?.notificationPreferences ?: return

        // Create a new map with the updated threshold
        val updatedThresholds = currentPreferences.distanceThresholds.toMutableMap().apply {
            this[range] = minutes
        }

        // Create new preferences with updated thresholds
        val updatedPreferences = currentPreferences.copy(
            distanceThresholds = updatedThresholds
        )

        // Update Firestore
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("notificationPreferences.distanceThresholds.$range", minutes)
                    .await()

                // Update local user data
                val updatedUser = _userData.value?.copy(notificationPreferences = updatedPreferences)
                _userData.value = updatedUser
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}