package com.example.dosennotif.api

import com.example.dosennotif.model.ScheduleResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("data/list_jadwal_pertemuan_dosen")
    suspend fun getLecturerSchedule(
        @Body requestBody: Map<String, String>
    ): ScheduleResponse
}