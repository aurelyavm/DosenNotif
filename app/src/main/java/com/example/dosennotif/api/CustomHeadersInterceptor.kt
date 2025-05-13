package com.example.dosennotif.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class CustomHeadersInterceptor(private val headers: Map<String, String>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // Add custom headers
        headers.forEach { (key, value) ->
            Log.d("ApiClient", "Adding header: $key = $value")
            requestBuilder.header(key, value)
        }

        // Log full request for debugging
        val request = requestBuilder.build()
        Log.d("ApiClient", "Request Headers: ${request.headers}")

        return chain.proceed(request)
    }
}