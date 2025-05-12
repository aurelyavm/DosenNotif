package com.example.dosennotif.model

import java.io.Serializable

// Class for notification model
data class ScheduleNotification(
    val id: String,
    val scheduleId: String,
    val title: String,
    val message: String,
    val scheduledTime: Long,
    val actualScheduleTime: Long,
    val room: String,
    val courseName: String,
    val className: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable