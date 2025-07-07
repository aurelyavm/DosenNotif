package com.example.dosennotif.api

import android.util.Base64
import com.example.dosennotif.BuildConfig
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.upnvj.ac.id/"

    private val CUSTOM_HEADERS = mapOf(
        //"API_KEY_NAME" to "X-UPNVJ-API-KEY",
        //"API_KEY_SECRET" to "Cspwwxq5SyTOMkq8XYcwZ1PMpYrYCwrv",
        "X-UPNVJ-API-KEY" to BuildConfig.X_UPNVJ_API_KEY,
        "Accept" to "application/json",
        "Content-Type" to "multipart/form-data"
    )

    private const val USERNAME = "uakademik"
    private const val PASSWORD = "VTUzcjRrNGRlbTFrMjAyNCYh"

    private fun getAuthorizationHeader(): String {
        val credentials = "$USERNAME:$PASSWORD"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }

    fun create(): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(CustomHeadersInterceptor(CUSTOM_HEADERS, getAuthorizationHeader()))
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
