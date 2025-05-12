package com.example.dosennotif.model

data class NotificationPreferences(
    val enabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    // Default distance thresholds (in minutes)
    val distanceThresholds: Map<String, Int> = mapOf(
        "0-10" to 30,   // 0-10 km, 30 minutes before
        "10-20" to 60,  // 10-20 km, 60 minutes before
        "20-30" to 90,  // 20-30 km, 90 minutes before
        "30-40" to 120, // 30-40 km, 120 minutes before
        "40-50" to 150  // 40-50 km, 150 minutes before
    )
)