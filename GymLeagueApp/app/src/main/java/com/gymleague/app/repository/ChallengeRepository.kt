package com.gymleague.app.repository

import com.gymleague.app.models.Challenge
import com.gymleague.app.models.CreateChallengeRequest
import com.gymleague.app.network.RetrofitClient

class ChallengeRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getChallenges(): Result<List<Challenge>> {
        return try {
            val response = apiService.getChallenges()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to fetch challenges: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChallenge(
        request: CreateChallengeRequest
    ): Result<Challenge> {
        return try {
            val response = apiService.createChallenge(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to create challenge: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logProgress(
        challengeId: Int
    ): Result<Challenge> {
        return try {
            val response = apiService.logChallengeProgress(challengeId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to log progress: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}