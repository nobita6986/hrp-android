package com.example.utils

import com.example.BuildConfig

/**
 * Global application runtime configuration flags.
 * Controlled strictly by Gradle build variants (debug, release, demo).
 */
object AppConfig {

    /**
     * Demo mode is strictly restricted to the `demo` build variant.
     * When false (debug & release):
     * - API calls return REAL errors from the backend (no mock fallback).
     * - Check-in requires REAL GPS hardware location from the device.
     * - Quick demo credentials are hidden from the login screen.
     */
    val isDemoMode: Boolean
        get() = try {
            val field = BuildConfig::class.java.getField("ENABLE_DEMO_MODE")
            field.getBoolean(null)
        } catch (_: Throwable) {
            false
        }

    /**
     * Controls HTTP wire logging.
     * In release builds, this is strictly false to prevent sensitive token/cookie leaks.
     */
    val isHttpLoggingEnabled: Boolean
        get() = try {
            val field = BuildConfig::class.java.getField("LOG_HTTP")
            field.getBoolean(null)
        } catch (_: Throwable) {
            false
        }
}
