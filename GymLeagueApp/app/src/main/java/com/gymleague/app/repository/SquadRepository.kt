package com.gymleague.app.repository

import com.gymleague.app.models.AddSquadMemberRequest
import com.gymleague.app.models.CreateSquadRequest
import com.gymleague.app.models.Squad
import com.gymleague.app.models.SquadMember
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

    suspend fun createSquad(squadName: String): Result<Squad> {
        return try {
            val response = apiService.createSquad(CreateSquadRequest(squadName))

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to create squad: ${response.code()}")
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(squadId: Int, userId: Int): Result<SquadMember> {
        return try {
            val response = apiService.addSquadMember(squadId, AddSquadMemberRequest(userId))

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to join squad: ${response.code()}")
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveSquad(squadId: Int, userId: Int): Result<Unit> {
        return try {
            val response = apiService.leaveSquad(squadId, userId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("Failed to leave squad: ${response.code()}")
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}