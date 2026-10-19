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
                    continuation.resume(evaluateLocation(location))
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            continuation.resume(evaluateLocation(lastLoc))
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    private fun evaluateLocation(location: Location): GpsLocationResult {
        val siteDescription = "Tọa độ thực tế (%.5f, %.5f • Sai số: ±%.1fm)".format(
            location.latitude,
            location.longitude,
            location.accuracy
        )

        return GpsLocationResult(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            isInsideGeofence = true, // Server evaluates geofence against assigned project coordinates & radius
            matchedSite = siteDescription
        )
    }
}
