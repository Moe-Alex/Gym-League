package com.gymleague.app.repository

import com.gymleague.app.models.UserSettings
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.toApiResult
import com.gymleague.app.util.toFailure

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
                            "Failed to load settings: ${response.code()}"
                        )
                    )
                }
            }

        } catch (e: Exception) {

            e.toFailure()
        }
    }

    suspend fun createSettings(
        settings: UserSettings
    ): Result<UserSettings> {
        return try {
            apiService.createSettings(settings).toApiResult("Failed to create settings")
        } catch (e: Exception) {
            e.toFailure()
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

            e.toFailure()
        }
    }
}