package com.example.goout.data.repository

import com.example.goout.data.FirestoreSchema
import com.example.goout.data.local.dao.TelemetryDao
import com.example.goout.data.remote.firebase.FirebaseProvider
import com.example.goout.data.toEntity
import com.example.goout.domain.model.SyncStatus
import com.example.goout.domain.model.TelemetryEvent
import com.example.goout.domain.repository.TelemetryRepository
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val telemetryDao: TelemetryDao,
    private val firebase: FirebaseProvider,
) : TelemetryRepository {

    override suspend fun saveLocal(event: TelemetryEvent) {
        telemetryDao.insert(event.toEntity())
    }

    /**
     * Pushes PENDING telemetry to Firestore. Document IDs are the stable
     * telemetryId, so retries are idempotent (a duplicate write simply
     * overwrites the identical document).
     */
    override suspend fun syncPending(): Result<Int> = runCatching {
        val firestore = firebase.firestore ?: error("Firebase tidak tersedia")
        if (firebase.auth?.currentUser == null) error("Tidak terautentikasi")
        val pending = telemetryDao.pending()
        var synced = 0
        for (row in pending) {
            runCatching {
                firestore.collection(FirestoreSchema.TELEMETRY).document(row.telemetryId)
                    .set(
                        mapOf(
                            "telemetryId" to row.telemetryId,
                            "userId" to row.userId,
                            "corridorId" to row.corridorId,
                            "latitude" to row.latitude,
                            "longitude" to row.longitude,
                            "speedKmh" to row.speedKmh,
                            "clientTimestamp" to row.timestamp,
                            "source" to row.source,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                telemetryDao.updateStatus(row.telemetryId, SyncStatus.SYNCED.name)
                synced++
            }.onFailure {
                telemetryDao.updateStatus(row.telemetryId, SyncStatus.FAILED.name)
                throw it
            }
        }
        telemetryDao.pruneSynced(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        synced
    }

    override fun observePendingCount(): Flow<Int> = telemetryDao.observePendingCount()
}
