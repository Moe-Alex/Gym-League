package com.gymleague.app.repository

import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.database.toEntity
import com.gymleague.app.database.toModel
import com.gymleague.app.models.AddSquadMemberRequest
import com.gymleague.app.models.CreateSquadRequest
import com.gymleague.app.models.Squad
import com.gymleague.app.models.SquadMember
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.CacheKeys
import com.gymleague.app.util.OfflineLoader
import com.gymleague.app.util.toApiResult
import com.gymleague.app.util.toFailure

class SquadRepository {

    private val apiService = RetrofitClient.apiService

    private val db get() = GymLeagueApplication.instance.database

    // Saved to Room, and read from Room when offline
    suspend fun getSquads(): Result<List<Squad>> {
        return OfflineLoader.load<List<Squad>>(
            db = db,
            cacheKey = CacheKeys.SQUADS,
            label = "squads",
            sizeOf = { it.size },
            fetch = {
                try {
                    apiService.getSquads().toApiResult("Failed to load squads")
                } catch (e: Exception) {
                    e.toFailure()
                }
            },
            save = { list -> db.squadDao().replaceAll(list.map { it.toEntity() }) },
            read = { db.squadDao().getAll().map { it.toModel() } }
        )
    }

    suspend fun getSquad(squadId: Int): Result<Squad> {
        return try {
            apiService.getSquad(squadId).toApiResult("Failed to load squad")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    suspend fun createSquad(squadName: String): Result<Squad> {
        return try {
            apiService.createSquad(CreateSquadRequest(squadName)).toApiResult("Failed to create squad")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    suspend fun addMember(squadId: Int, userId: Int): Result<SquadMember> {
        return try {
            apiService.addSquadMember(squadId, AddSquadMemberRequest(userId))
                .toApiResult("Failed to join squad")
        } catch (e: Exception) {
            e.toFailure()
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
            e.toFailure()
        }
    }
}