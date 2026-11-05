package com.gymleague.app.repository

import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.database.toEntity
import com.gymleague.app.database.toModel
import com.gymleague.app.models.Challenge
import com.gymleague.app.models.CreateChallengeRequest
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.ApiErrorParser
import com.gymleague.app.util.ApiException
import com.gymleague.app.util.CacheKeys
import com.gymleague.app.util.OfflineLoader
import com.gymleague.app.util.toFailure
import retrofit2.Response

class ChallengeRepository {

    private val apiService = RetrofitClient.apiService

    private val db get() = GymLeagueApplication.instance.database

    // This user's challenges plus open challenges they could accept.
    // Offline the saved list is shown, but Battle Arena actions below still need internet.
    suspend fun getChallenges(userId: Int): Result<List<Challenge>> {
        return OfflineLoader.load<List<Challenge>>(
            db = db,
            cacheKey = CacheKeys.challenges(userId),
            label = "challenges",
            sizeOf = { it.size },
            fetch = {
                try {
                    toResult(apiService.getChallenges(userId), "Failed to fetch challenges")
                } catch (e: Exception) {
                    e.toFailure()
                }
            },
            save = { list ->
                db.challengeDao().replaceForUser(userId, list.map { it.toEntity(userId) })
            },
            read = { db.challengeDao().getForUser(userId).map { it.toModel() } }
        )
    }

    suspend fun createChallenge(request: CreateChallengeRequest): Result<Challenge> {
        return try {
            toResult(apiService.createChallenge(request), "Failed to create challenge")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    suspend fun acceptChallenge(challengeId: Int, userId: Int): Result<Challenge> {
        return try {
            toResult(apiService.acceptChallenge(challengeId, userId), "Failed to accept challenge")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    suspend fun declineChallenge(challengeId: Int, userId: Int): Result<Challenge> {
        return try {
            toResult(apiService.declineChallenge(challengeId, userId), "Failed to decline challenge")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    suspend fun logProgress(challengeId: Int, userId: Int): Result<Challenge> {
        return try {
            toResult(apiService.logChallengeProgress(challengeId, userId), "Failed to log progress")
        } catch (e: Exception) {
            e.toFailure()
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

        return Result.failure(ApiException(response.code(), message))
    }
}