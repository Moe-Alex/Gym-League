package com.gymleague.app.repository

import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.database.toEntity
import com.gymleague.app.database.toModel
import com.gymleague.app.models.CreateUserRequest
import com.gymleague.app.models.User
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.CacheKeys
import com.gymleague.app.util.OfflineLoader
import com.gymleague.app.util.toApiResult
import com.gymleague.app.util.toFailure

class UserRepository {

    private val apiService =
        RetrofitClient.apiService

    private val db get() = GymLeagueApplication.instance.database

    suspend fun getUsers(): Result<List<User>> {
        return try {
            apiService.getUsers().toApiResult("Failed to fetch users")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    // The user's XP and level. Saved to Room, and read from Room when offline.
    suspend fun getUser(
        userId: Int
    ): Result<User> {
        return OfflineLoader.load<User>(
            db = db,
            cacheKey = CacheKeys.user(userId),
            label = "user stats",
            sizeOf = { 1 },
            fetch = {
                try {
                    apiService.getUser(userId).toApiResult("Failed to fetch user")
                } catch (e: Exception) {
                    e.toFailure()
                }
            },
            save = { user -> db.userDao().insert(user.toEntity()) },
            read = { db.userDao().getById(userId)?.toModel() }
        )
    }

    suspend fun getUserByFirebaseUid(
        firebaseUid: String
    ): Result<User?> {

        return try {

            val response =
                apiService.getUserByFirebaseUid(
                    firebaseUid
                )

            val body = response.body()

            when {

                response.isSuccessful && body != null -> {
                    Result.success(body)
                }

                response.code() == 404 -> {
                    Result.success(null)
                }

                else -> {
                    Result.failure(
                        Exception(
                            "Failed to fetch Firebase user: ${response.code()}"
                        )
                    )
                }
            }

        } catch (e: Exception) {

            e.toFailure()
        }
    }

    suspend fun getUserByUsername(
        username: String
    ): Result<User?> {

        return try {

            val response =
                apiService.getUserByUsername(
                    username
                )

            val body = response.body()

            when {

                response.isSuccessful && body != null -> {
                    Result.success(body)
                }

                response.code() == 404 -> {
                    Result.success(null)
                }

                else -> {
                    Result.failure(
                        Exception(
                            "Failed to search user: ${response.code()}"
                        )
                    )
                }
            }

        } catch (e: Exception) {

            e.toFailure()
        }
    }

    suspend fun createUser(
        request: CreateUserRequest
    ): Result<User> {
        return try {
            apiService.createUser(request).toApiResult("Failed to create user")
        } catch (e: Exception) {
            e.toFailure()
        }
    }
}