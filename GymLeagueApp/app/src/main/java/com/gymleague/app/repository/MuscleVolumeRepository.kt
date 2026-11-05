package com.gymleague.app.repository

import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.database.MuscleVolumeEntity
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.CacheKeys
import com.gymleague.app.util.OfflineLoader
import com.gymleague.app.util.toApiResult
import com.gymleague.app.util.toFailure

class MuscleVolumeRepository {

    private val apiService = RetrofitClient.apiService

    private val db get() = GymLeagueApplication.instance.database

    // Total volume (sets x reps x weight) per muscle group for this user.
    // Saved to Room, and read from Room when offline.
    suspend fun getMuscleGroupVolume(userId: Int): Result<Map<String, Double>> {
        return OfflineLoader.load<Map<String, Double>>(
            db = db,
            cacheKey = CacheKeys.muscleVolume(userId),
            label = "muscle volume rows",
            sizeOf = { it.size },
            fetch = {
                try {
                    apiService.getMuscleGroupVolume(userId).toApiResult("Failed to load muscle volume")
                } catch (e: Exception) {
                    e.toFailure()
                }
            },
            save = { map ->
                db.muscleVolumeDao().replaceForUser(
                    userId,
                    map.map { (group, volume) -> MuscleVolumeEntity(userId, group, volume) }
                )
            },
            read = {
                db.muscleVolumeDao().getForUser(userId).associate { it.muscleGroup to it.volume }
            }
        )
    }
}