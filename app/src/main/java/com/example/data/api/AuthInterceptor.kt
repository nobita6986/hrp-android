package com.example.data.api

import com.example.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that automatically attaches Authorization: Bearer <token>
 * and standard JSON headers to every outbound HTTP request.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            requestBuilder.addHeader("Cookie", "hrp_session=$token")
        }

        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")

        val response = chain.proceed(requestBuilder.build())

        // Auto-extract hrp_session cookie if returned from web backend
        val setCookieHeaders = response.headers("Set-Cookie")
        for (header in setCookieHeaders) {
            if (header.contains("hrp_session=")) {
                val extractedToken = header.substringAfter("hrp_session=").substringBefore(";").trim()
                if (extractedToken.isNotEmpty() && extractedToken != "deleted") {
                    tokenManager.saveTokenSync(extractedToken)
                }
            }
        }

        return response
    }
}
