package com.gymleague.app.repository

import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.database.toEntity
import com.gymleague.app.database.toModel
import com.gymleague.app.models.PersonalRecord
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.CacheKeys
import com.gymleague.app.util.OfflineLoader
import com.gymleague.app.util.toApiResult
import com.gymleague.app.util.toFailure

class PersonalRecordsRepository {

    private val apiService = RetrofitClient.apiService

    private val db get() = GymLeagueApplication.instance.database

    // Saved to Room, and read from Room when offline
    suspend fun getPersonalRecords(
        userId: Int
    ): Result<List<PersonalRecord>> {
        return OfflineLoader.load<List<PersonalRecord>>(
            db = db,
            cacheKey = CacheKeys.records(userId),
            label = "personal records",
            sizeOf = { it.size },
            fetch = {
                try {
                    apiService.getPersonalRecords(userId)
                        .toApiResult("Failed to fetch personal records")
                } catch (e: Exception) {
                    e.toFailure()
                }
            },
            save = { list ->
                db.personalRecordDao().replaceForUser(userId, list.map { it.toEntity(userId) })
            },
            read = { db.personalRecordDao().getForUser(userId).map { it.toModel() } }
        )
    }
}