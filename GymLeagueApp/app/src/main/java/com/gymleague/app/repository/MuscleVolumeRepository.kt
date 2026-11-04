package com.gymleague.app.repository

import com.gymleague.app.network.RetrofitClient

class MuscleVolumeRepository {

    private val apiService = RetrofitClient.apiService

    // Total volume (sets x reps x weight) per muscle group for this user
    suspend fun getMuscleGroupVolume(userId: Int): Result<Map<String, Double>> {
        return try {
            val response = apiService.getMuscleGroupVolume(userId)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(
                    Exception("Failed to load muscle volume: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
