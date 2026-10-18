package com.example.data.api

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val DEFAULT_BASE_URL = "https://vieclammienbac.com.vn/api/"
    const val LOCAL_EMULATOR_URL = "http://10.0.2.2:3000/api/"

    @Volatile
    private var apiServiceInstance: HrpApiService? = null

    @Volatile
    private var customBaseUrl: String? = null

    /**
     * Resolves the current base URL in priority order:
     * 1. Explicitly configured runtime URL (if set)
     * 2. BuildConfig.BASE_URL (from .env via secrets plugin)
     * 3. DEFAULT_BASE_URL fallback
     */
    fun getBaseUrl(): String {
        customBaseUrl?.let { return it }

        return try {
            val buildConfigUrl = BuildConfig::class.java.getField("BASE_URL").get(null) as? String
            if (!buildConfigUrl.isNullOrBlank()) {
                if (buildConfigUrl.endsWith("/")) buildConfigUrl else "$buildConfigUrl/"
            } else {
                DEFAULT_BASE_URL
            }
        } catch (_: Throwable) {
            DEFAULT_BASE_URL
        }
    }

    fun setCustomBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        customBaseUrl = normalized
        apiServiceInstance = null // Invalidate existing client
    }

    fun getApiService(context: Context): HrpApiService {
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: buildRetrofit(context.applicationContext).create(HrpApiService::class.java).also {
                apiServiceInstance = it
            }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val tokenManager = TokenManager.getInstance(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            if (com.example.utils.AppConfig.isHttpLoggingEnabled) {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
            } else {
                level = HttpLoggingInterceptor.Level.NONE
            }
        }

        val authInterceptor = AuthInterceptor(tokenManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
