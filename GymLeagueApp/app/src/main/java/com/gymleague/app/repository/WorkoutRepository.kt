package com.gymleague.app.repository

import com.gymleague.app.models.CreateWorkoutRequest
import com.gymleague.app.models.Workout
import com.gymleague.app.network.RetrofitClient

class WorkoutRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun createWorkout(
        workout: CreateWorkoutRequest
    ): Result<Workout> {

        return try {

            val response =
                apiService.createWorkout(workout)

            if (response.isSuccessful &&
                response.body() != null
            ) {

                Result.success(response.body()!!)

            } else {

                val errorBody =
                    response.errorBody()?.string()

                Result.failure(
                    Exception(
                        "Error ${response.code()}: $errorBody"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}