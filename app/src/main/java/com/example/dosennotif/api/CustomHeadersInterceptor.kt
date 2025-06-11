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

        headers.forEach { (key, value) ->
            Log.d("ApiClient", "Adding header: $key = $value")
            requestBuilder.addHeader(key, value)
        }

        Log.d("ApiClient", "Adding header: Authorization = $authorizationHeader")
        requestBuilder.addHeader("Authorization", authorizationHeader)

        val request = requestBuilder.build()

        Log.d("ApiClient", "Request Headers: ${request.headers}")

        return chain.proceed(request)
    }
}
