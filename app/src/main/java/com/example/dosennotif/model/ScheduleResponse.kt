package com.example.dosennotif.model

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ScheduleResponse(
    val status: String = "",
    val message: String = "",
    val data: List<Schedule>? = null
)