package com.example.dosennotif.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class CustomHeadersInterceptor(
    private val headers: Map<String, String>,
    private val authorizationHeader: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // Tambahkan header kustom
        headers.forEach { (key, value) ->
            Log.d("ApiClient", "Adding header: $key = $value")
            requestBuilder.addHeader(key, value)
        }

        // Tambahkan header Authorization untuk Basic Auth
        Log.d("ApiClient", "Adding header: Authorization = $authorizationHeader")
        requestBuilder.addHeader("Authorization", authorizationHeader)

        // Membangun request dengan semua header yang telah ditambahkan
        val request = requestBuilder.build()

        // Log request headers untuk debugging
        Log.d("ApiClient", "Request Headers: ${request.headers}")

        return chain.proceed(request)
    }
}
