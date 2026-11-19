package com.example.goout.domain.usecase

import com.example.goout.domain.model.DensityStatus
import javax.inject.Inject

/**
 * CORE FUNCTION 1 — Traffic density classification.
 *
 * Thresholds (functional requirement, exact):
 *  - speed >= 35 km/h          -> LANCAR
 *  - 15 <= speed < 35 km/h     -> MERAYAP
 *  - speed < 15 km/h           -> MACET
 * Negative, NaN or infinite speeds are invalid -> UNKNOWN.
 */
fun classifyTrafficDensity(speedKmh: Double): DensityStatus = when {
    speedKmh.isNaN() || speedKmh.isInfinite() || speedKmh < 0.0 -> DensityStatus.UNKNOWN
    speedKmh >= 35.0 -> DensityStatus.LANCAR
    speedKmh >= 15.0 -> DensityStatus.MERAYAP
    else -> DensityStatus.MACET
}

class ClassifyTrafficDensityUseCase @Inject constructor() {
    operator fun invoke(speedKmh: Double): DensityStatus = classifyTrafficDensity(speedKmh)
}
