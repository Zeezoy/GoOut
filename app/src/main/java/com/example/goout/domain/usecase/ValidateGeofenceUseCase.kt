package com.example.goout.domain.usecase

import com.example.goout.core.utils.GeoUtils
import com.example.goout.domain.model.Corridor
import com.example.goout.domain.model.GeofenceReason
import com.example.goout.domain.model.GeofenceResult
import com.example.goout.domain.model.GeoPoint
import javax.inject.Inject

/**
 * CORE FUNCTION 2 — Corridor geofence validation.
 *
 * A submission (telemetry or report) is valid only when the device is within
 * [MAX_DISTANCE_METERS] of the nearest monitored corridor polyline.
 * Distance is the shortest point-to-segment distance across every polyline
 * segment of every corridor, not merely distance to endpoints.
 */
class GeofenceValidator @Inject constructor() {

    companion object {
        const val MAX_DISTANCE_METERS = 100.0

        /** Fixes with worse horizontal accuracy than this are rejected. */
        const val MAX_ACCURACY_METERS = 50.0
    }

    fun validate(
        location: GeoPoint,
        corridors: List<Corridor>,
        accuracyMeters: Float? = null,
    ): GeofenceResult {
        if (!location.isValid) {
            return GeofenceResult(false, null, null, GeofenceReason.INVALID_COORDINATES)
        }
        if (accuracyMeters != null && accuracyMeters > MAX_ACCURACY_METERS) {
            return GeofenceResult(false, null, null, GeofenceReason.POOR_ACCURACY)
        }
        val usable = corridors.filter { it.polyline.isNotEmpty() }
        if (usable.isEmpty()) {
            return GeofenceResult(false, null, null, GeofenceReason.NO_CORRIDOR_DATA)
        }

        var nearestId: String? = null
        var nearestDistance = Double.MAX_VALUE
        for (corridor in usable) {
            val d = GeoUtils.distanceToPolylineMeters(location, corridor.polyline)
            if (d < nearestDistance) {
                nearestDistance = d
                nearestId = corridor.corridorId
            }
        }

        // Distance exactly at the threshold is VALID (spec: <= 100 m).
        // A tiny epsilon absorbs floating-point error at the boundary.
        return if (nearestDistance <= MAX_DISTANCE_METERS + 1e-6) {
            GeofenceResult(true, nearestId, nearestDistance, GeofenceReason.WITHIN_CORRIDOR)
        } else {
            GeofenceResult(false, nearestId, nearestDistance, GeofenceReason.OUTSIDE_CORRIDOR)
        }
    }
}

class ValidateGeofenceUseCase @Inject constructor(private val validator: GeofenceValidator) {
    operator fun invoke(
        location: GeoPoint,
        corridors: List<Corridor>,
        accuracyMeters: Float? = null,
    ): GeofenceResult = validator.validate(location, corridors, accuracyMeters)
}
