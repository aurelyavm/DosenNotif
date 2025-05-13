package com.example.dosennotif.api

import com.example.dosennotif.model.ScheduleResponse
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("data/list_jadwal_pertemuan_dosen")
    suspend fun getLecturerSchedule(
        @Part("id_program_studi") idProgramStudi: RequestBody,
        @Part("id_periode") idPeriode: RequestBody
    ): ScheduleResponse
}
