package com.example.dosennotif.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object LocationUtils {
    // UPNVJ Campus Location (can be adjusted to more accurate coordinates)
    private const val UPNVJ_LATITUDE = -6.315588522917615
    private const val UPNVJ_LONGITUDE = 106.83980697680275

    // Calculate distance between two locations in kilometers
    fun calculateDistance(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startLatitude,
            startLongitude,
            endLatitude,
            endLongitude,
            results
        )
        // Convert meters to kilometers
        return results[0] / 1000
    }

    // Calculate distance from campus
    fun calculateDistanceFromCampus(latitude: Double, longitude: Double): Float {
        return calculateDistance(
            latitude,
            longitude,
            UPNVJ_LATITUDE,
            UPNVJ_LONGITUDE
        )
    }

    // Get notification delay based on distance (in minutes)
    fun getNotificationDelay(distanceInKm: Float): Int {
        return when {
            distanceInKm < 10 -> 30     // 0-10 km -> 30 minutes
            distanceInKm < 20 -> 60     // 10-20 km -> 60 minutes
            distanceInKm < 30 -> 90     // 20-30 km -> 90 minutes
            distanceInKm < 40 -> 120    // 30-40 km -> 120 minutes
            else -> 150                // 40+ km -> 150 minutes
        }
    }

    // Get location updates as Flow
    fun getLocationUpdates(context: Context, intervalMs: Long = 60000): Flow<Location> {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Create location request
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(100f)
            .build()

        return callbackFlow {
            // Check if permission is granted
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                close()
                return@callbackFlow
            }

            // Location callback
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        trySend(location)
                    }
                }
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Clean up
            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    // Get last known location
    suspend fun getLastLocation(context: Context): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }
}

// Extension to convert Task to suspend function
/*
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? {
    return try {
        kotlinx.coroutines.tasks.await()
    } catch (e: Exception) {
        null
    }
}

 */