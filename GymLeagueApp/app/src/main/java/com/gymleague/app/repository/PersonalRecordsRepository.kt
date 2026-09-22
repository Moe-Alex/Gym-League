package com.gymleague.app.repository

import com.gymleague.app.models.PersonalRecord
import com.gymleague.app.network.RetrofitClient

class PersonalRecordsRepository {

    private val apiService =
        RetrofitClient.apiService

    suspend fun getPersonalRecords(
        userId: Int
    ): Result<List<PersonalRecord>> {

        return try {

            val response =
                apiService.getPersonalRecords(userId)

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
                        "Failed to fetch personal records: ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}