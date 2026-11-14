package com.example.goout.domain.usecase

import com.example.goout.core.common.CorridorConfig
import com.example.goout.domain.model.Corridor
import com.example.goout.domain.model.GeoPoint
import com.example.goout.domain.model.LocationFix
import com.example.goout.domain.model.TelemetryEvent
import java.util.UUID
import javax.inject.Inject

/** Why a telemetry fix was not turned into an event. */
enum class TelemetryRejection {
    NOT_AUTHENTICATED, NO_SPEED, INVALID_SPEED, OUTSIDE_CORRIDOR, TOO_FREQUENT, POOR_ACCURACY,
}

sealed interface TelemetryOutcome {
    data class Accepted(val event: TelemetryEvent) : TelemetryOutcome
    data class Rejected(val reason: TelemetryRejection) : TelemetryOutcome
}

const val MPS_TO_KMH = 3.6

/**
 * CORE FUNCTION 3 — Passive telemetry processing (pure logic, fully testable).
 * Validates a GPS fix, converts speed (km/h = m/s * 3.6), geofences it and
 * enforces the minimum submission interval.
 */
class ProcessTelemetryFixUseCase @Inject constructor(
    private val validateGeofence: ValidateGeofenceUseCase,
) {
    /** Timestamp of the last accepted event, per process lifetime. */
    @Volatile
    private var lastAcceptedAt: Long = 0L

    fun reset() { lastAcceptedAt = 0L }

    operator fun invoke(
        userId: String?,
        fix: LocationFix,
        corridors: List<Corridor>,
        now: Long = System.currentTimeMillis(),
        minIntervalMillis: Long = CorridorConfig.TELEMETRY_MIN_INTERVAL_MILLIS,
        source: String = "gps",
    ): TelemetryOutcome {
        if (userId.isNullOrBlank()) return TelemetryOutcome.Rejected(TelemetryRejection.NOT_AUTHENTICATED)

        val speedMps = fix.speedMetersPerSecond
            ?: return TelemetryOutcome.Rejected(TelemetryRejection.NO_SPEED)
        if (speedMps.isNaN() || speedMps < 0f) {
            return TelemetryOutcome.Rejected(TelemetryRejection.INVALID_SPEED)
        }
        val speedKmh = speedMps * MPS_TO_KMH

        val geofence = validateGeofence(
            GeoPoint(fix.latitude, fix.longitude),
            corridors,
            fix.accuracyMeters,
        )
        if (!geofence.isValid || geofence.corridorId == null) {
            return TelemetryOutcome.Rejected(
                if (geofence.reason == com.example.goout.domain.model.GeofenceReason.POOR_ACCURACY) {
                    TelemetryRejection.POOR_ACCURACY
                } else {
                    TelemetryRejection.OUTSIDE_CORRIDOR
                },
            )
        }

        if (now - lastAcceptedAt < minIntervalMillis) {
            return TelemetryOutcome.Rejected(TelemetryRejection.TOO_FREQUENT)
        }
        lastAcceptedAt = now

        return TelemetryOutcome.Accepted(
            TelemetryEvent(
                telemetryId = UUID.randomUUID().toString(),
                userId = userId,
                corridorId = geofence.corridorId,
                latitude = fix.latitude,
                longitude = fix.longitude,
                speedKmh = speedKmh,
                timestamp = now,
                source = source,
            ),
        )
    }
}
