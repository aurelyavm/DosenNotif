package com.example.dosennotif.utils

import android.content.Context

class AppConfig private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: AppConfig? = null

        fun getInstance(context: Context): AppConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var useMockData: Boolean
        get() = prefs.getBoolean("use_mock_data", false)
        set(value) = prefs.edit().putBoolean("use_mock_data", value).apply()

    var apiTimeout: Long
        get() = prefs.getLong("api_timeout", 15000L)
        set(value) = prefs.edit().putLong("api_timeout", value).apply()

    var enableOfflineMode: Boolean
        get() = prefs.getBoolean("enable_offline_mode", true)
        set(value) = prefs.edit().putBoolean("enable_offline_mode", value).apply()

    var lastApiSuccess: Long
        get() = prefs.getLong("last_api_success", 0L)
        set(value) = prefs.edit().putLong("last_api_success", value).apply()

    // Helper functions
    fun isApiRecentlySuccessful(thresholdMs: Long = 300000L): Boolean { // 5 minutes
        return (System.currentTimeMillis() - lastApiSuccess) < thresholdMs
    }

    fun getApiStatusText(): String {
        return when {
            useMockData -> "Mode Demo Aktif"
            isApiRecentlySuccessful() -> "Server Normal"
            enableOfflineMode -> "Mode Offline"
            else -> "Server Error"
        }
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}