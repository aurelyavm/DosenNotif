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

    private const val UPNVJ_LATITUDE = -6.315588522917615 //-6.3159007105195535, 106.79496049535477
    private const val UPNVJ_LONGITUDE = 106.83980697680275 //-6.315868725694896, 106.79500341534542

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
        return results[0] / 1000
    }

    fun calculateDistanceFromCampus(latitude: Double, longitude: Double): Float {
        return calculateDistance(
            latitude,
            longitude,
            UPNVJ_LATITUDE,
            UPNVJ_LONGITUDE
        )
    }

    fun getNotificationDelay(distanceInKm: Float): Int {
        return when {
            distanceInKm < 10 -> 30
            distanceInKm < 20 -> 60
            distanceInKm < 30 -> 90
            distanceInKm < 40 -> 120
            else -> 150
        }
    }

    fun getLocationUpdates(context: Context, intervalMs: Long = 60000): Flow<Location> {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(100f)
            .build()

        return callbackFlow {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                close()
                return@callbackFlow
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        trySend(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    suspend fun getLastLocation(context: Context): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

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