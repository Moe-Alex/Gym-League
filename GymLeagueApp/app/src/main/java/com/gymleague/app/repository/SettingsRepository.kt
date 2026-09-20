package com.gymleague.app.repository

import com.gymleague.app.models.UserSettings
import com.gymleague.app.network.RetrofitClient

class SettingsRepository {

    private val apiService =
        RetrofitClient.apiService

    suspend fun getSettingsForUser(
        userId: Int
    ): Result<UserSettings?> {

        return try {

            val response =
                apiService.getSettingsForUser(
                    userId
                )

            when {

                response.isSuccessful &&
                        response.body() != null -> {

                    Result.success(
                        response.body()
                    )
                }

                response.code() == 404 -> {

                    Result.success(null)
                }

                else -> {

                    Result.failure(
                        Exception(
                            "Failed to load settings: ${response.code()}"
                        )
                    )
                }
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun createSettings(
        settings: UserSettings
    ): Result<UserSettings> {

        return try {

            val response =
                apiService.createSettings(
                    settings
                )

            if (
                response.isSuccessful &&
                response.body() != null
            ) {

                Result.success(
                    response.body()!!
                )

            } else {

                Result.failure(
                    Exception(
                        "Failed to create settings: ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        settings: UserSettings
    ): Result<Unit> {

        return try {

            val response =
                apiService.updateSettings(
                    settings.userSettingsId,
                    settings
                )

            if (response.isSuccessful) {

                Result.success(Unit)

            } else {

                Result.failure(
                    Exception(
                        "Failed to update settings: ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}