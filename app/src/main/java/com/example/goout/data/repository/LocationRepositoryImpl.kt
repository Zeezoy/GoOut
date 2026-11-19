package com.example.goout.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.goout.domain.model.LocationFix
import com.example.goout.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationRepository {

    private val fused: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationFix? {
        if (!hasLocationPermission() || !isLocationEnabled()) return null
        return runCatching {
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        }.getOrNull()?.let {
            LocationFix(
                latitude = it.latitude,
                longitude = it.longitude,
                speedMetersPerSecond = if (it.hasSpeed()) it.speed else null,
                accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
                timestamp = it.time,
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(intervalMillis: Long): Flow<LocationFix> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Izin lokasi belum diberikan"))
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    trySend(
                        LocationFix(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            speedMetersPerSecond = if (it.hasSpeed()) it.speed else null,
                            accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
                            timestamp = it.time,
                        ),
                    )
                }
            }
        }
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fused.removeLocationUpdates(callback) }
    }
}
