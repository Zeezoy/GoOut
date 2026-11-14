package com.example.goout.core.utils

import com.example.goout.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Geodesic helpers: Haversine distance and shortest distance to a polyline. */
object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle distance between two coordinates in meters (Haversine formula). */
    fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Shortest distance in meters from [point] to the segment [start]-[end].
     * Uses a local equirectangular projection (accurate for segments of a few km,
     * which is the scale of the monitored corridors) and clamps the projection
     * onto the segment, so distance is measured to the segment itself, never
     * only to its endpoints.
     */
    fun distanceToSegmentMeters(point: GeoPoint, start: GeoPoint, end: GeoPoint): Double {
        val refLat = Math.toRadians((start.latitude + end.latitude) / 2.0)
        // Project to a local planar frame (meters).
        fun project(p: GeoPoint): Pair<Double, Double> {
            val x = Math.toRadians(p.longitude) * EARTH_RADIUS_METERS * cos(refLat)
            val y = Math.toRadians(p.latitude) * EARTH_RADIUS_METERS
            return x to y
        }

        val (px, py) = project(point)
        val (ax, ay) = project(start)
        val (bx, by) = project(end)

        val abx = bx - ax
        val aby = by - ay
        val lenSq = abx * abx + aby * aby
        val t = if (lenSq == 0.0) 0.0 else (((px - ax) * abx + (py - ay) * aby) / lenSq).coerceIn(0.0, 1.0)
        val cx = ax + t * abx
        val cy = ay + t * aby
        return sqrt((px - cx).pow(2) + (py - cy).pow(2))
    }

    /** Minimum distance in meters from [point] to a polyline (all its segments). */
    fun distanceToPolylineMeters(point: GeoPoint, polyline: List<GeoPoint>): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return haversineMeters(point, polyline.first())
        var best = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            best = min(best, distanceToSegmentMeters(point, polyline[i], polyline[i + 1]))
        }
        return best
    }
}
