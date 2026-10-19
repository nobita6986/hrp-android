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
            requestBuilder.header("Authorization", "Bearer $token")
            requestBuilder.header("Cookie", "hrp_session=$token; hrp_token=$token")
        }

        requestBuilder.header("X-HRP-Client", "android")
        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("Accept", "application/json")

        val response = chain.proceed(requestBuilder.build())

        // Auto-extract hrp_session or hrp_token cookie if returned from web backend
        val setCookieHeaders = response.headers("Set-Cookie")
        for (header in setCookieHeaders) {
            val cookieKey = when {
                header.contains("hrp_session=") -> "hrp_session="
                header.contains("hrp_token=") -> "hrp_token="
                else -> null
            }
            if (cookieKey != null) {
                val extractedToken = header.substringAfter(cookieKey).substringBefore(";").trim()
                if (extractedToken.isNotEmpty() && extractedToken != "deleted") {
                    tokenManager.saveTokenSync(extractedToken)
                }
            }
        }

        return response
    }
}

