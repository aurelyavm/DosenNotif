package com.example.dosennotif.model

import java.io.Serializable

data class ScheduleNotification(
    val id: String = "",
    val scheduleId: String = "",
    val title: String = "",
    val message: String = "",
    val scheduledTime: Long = 0L,
    val actualScheduleTime: Long = 0L,
    val room: String = "",
    val courseName: String = "",
    val className: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", 0L, 0L, "", "", "", false, System.currentTimeMillis())
}

/*data class ScheduleNotification(
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

 */