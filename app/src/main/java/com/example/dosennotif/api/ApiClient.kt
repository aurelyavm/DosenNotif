package com.example.dosennotif.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://api.upnvj.ac.id/"

    // API credentials (should be stored securely in production)
    private const val USERNAME = "uakademik"
    private const val PASSWORD = "VTUzcjRrNGRlbTFrMjAyNCYh"

    // Custom headers for the API
    private val CUSTOM_HEADERS = mapOf(
        "API_KEY_NAME" to "X-UPNVJ-API-KEY",
        "API_KEY_SECRET" to "Cspwwxq5SyTOMkq8XYcwZ1PMpYrYCwrv"
    )

    // Create API service
    fun create(): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(BasicAuthInterceptor(USERNAME, PASSWORD))
            .addInterceptor(CustomHeadersInterceptor(CUSTOM_HEADERS))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}