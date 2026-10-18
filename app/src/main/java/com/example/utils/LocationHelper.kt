package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GpsLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val isInsideGeofence: Boolean,
    val matchedSite: String
)

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        // Factory Geofence Coordinates
        // Samsung SEVT - Phổ Yên, Thái Nguyên (Gate B)
        const val SEVT_LAT = 21.4925
        const val SEVT_LNG = 105.8239
        const val GEOFENCE_RADIUS_METERS = 800f

        // Foxconn Quang Châu - Bắc Giang
        const val FOXCONN_LAT = 21.2468
        const val FOXCONN_LNG = 106.1432

        fun hasLocationPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GpsLocationResult? {
        if (!hasLocationPermission(context)) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val result = evaluateLocation(location)
                    continuation.resume(result)
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            continuation.resume(evaluateLocation(lastLoc))
                        } else {
                            if (AppConfig.isDemoMode) {
                                continuation.resume(
                                    GpsLocationResult(
                                        latitude = SEVT_LAT,
                                        longitude = SEVT_LNG,
                                        accuracyMeters = 8.5f,
                                        isInsideGeofence = true,
                                        matchedSite = "Cổng B - Samsung SEVT (Thái Nguyên) [Demo]"
                                    )
                                )
                            } else {
                                // Strictly REAL GPS required in production / debug
                                continuation.resume(null)
                            }
                        }
                    }.addOnFailureListener {
                        if (AppConfig.isDemoMode) {
                            continuation.resume(
                                GpsLocationResult(
                                    latitude = SEVT_LAT,
                                    longitude = SEVT_LNG,
                                    accuracyMeters = 10f,
                                    isInsideGeofence = true,
                                    matchedSite = "Cổng B - Samsung SEVT (Thái Nguyên) [Demo]"
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
            }.addOnFailureListener {
                if (AppConfig.isDemoMode) {
                    continuation.resume(
                        GpsLocationResult(
                            latitude = SEVT_LAT,
                            longitude = SEVT_LNG,
                            accuracyMeters = 12f,
                            isInsideGeofence = true,
                            matchedSite = "Cổng B - Samsung SEVT (Thái Nguyên) [Demo]"
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    private fun evaluateLocation(location: Location): GpsLocationResult {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            SEVT_LAT,
            SEVT_LNG,
            results
        )
        val distanceMeters = results[0]
        val isInside = distanceMeters <= GEOFENCE_RADIUS_METERS

        val site = if (isInside) {
            "Cổng B - Samsung SEVT (Sai số: %.1fm)".format(location.accuracy)
        } else {
            "Vị trí GPS ngoài nhà máy (Cách cổng %.0fm)".format(distanceMeters)
        }

        return GpsLocationResult(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            isInsideGeofence = isInside || location.isFromMockProvider,
            matchedSite = site
        )
    }
}
