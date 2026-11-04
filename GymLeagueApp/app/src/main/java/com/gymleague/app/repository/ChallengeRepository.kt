package com.gymleague.app.repository

import com.gymleague.app.models.Challenge
import com.gymleague.app.models.CreateChallengeRequest
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.ApiErrorParser
import retrofit2.Response

class ChallengeRepository {

    private val apiService = RetrofitClient.apiService

    // This user's challenges plus open challenges they could accept
    suspend fun getChallenges(userId: Int): Result<List<Challenge>> {
        return try {
            toResult(apiService.getChallenges(userId), "Failed to fetch challenges")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChallenge(request: CreateChallengeRequest): Result<Challenge> {
        return try {
            toResult(apiService.createChallenge(request), "Failed to create challenge")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptChallenge(challengeId: Int, userId: Int): Result<Challenge> {
        return try {
            toResult(apiService.acceptChallenge(challengeId, userId), "Failed to accept challenge")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineChallenge(challengeId: Int, userId: Int): Result<Challenge> {
        return try {
            toResult(apiService.declineChallenge(challengeId, userId), "Failed to decline challenge")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logProgress(challengeId: Int, userId: Int): Result<Challenge> {
        return try {
            toResult(apiService.logChallengeProgress(challengeId, userId), "Failed to log progress")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Success with a body, or a failure carrying the API's own explanation
    // (for example "You cannot accept your own challenge.")
    private fun <T> toResult(response: Response<T>, fallback: String): Result<T> {
        val body = response.body()

        if (response.isSuccessful && body != null) {
            return Result.success(body)
        }

        val message = ApiErrorParser.message(
            response.errorBody()?.string(),
            "$fallback (${response.code()})"
        )

        return Result.failure(Exception(message))
    }
}
