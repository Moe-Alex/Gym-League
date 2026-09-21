package com.gymleague.app.repository

import com.gymleague.app.models.Squad
import com.gymleague.app.network.RetrofitClient

class SquadRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getSquads(): Result<List<Squad>> {
        return try {

            val response = apiService.getSquads()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to load squads: ${response.code()}")
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSquad(squadId: Int): Result<Squad> {
        return try {

            val response = apiService.getSquad(squadId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to load squad: ${response.code()}")
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}