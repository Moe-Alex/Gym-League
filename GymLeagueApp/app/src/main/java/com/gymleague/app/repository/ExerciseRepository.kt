package com.gymleague.app.repository

import com.gymleague.app.models.Exercise
import com.gymleague.app.network.RetrofitClient

class ExerciseRepository {

    private val apiService = RetrofitClient.apiService

    // Get all exercises from the REST API
    suspend fun getExercises(): Result<List<Exercise>> {
        return try {
            val response = apiService.getExercises()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to fetch exercises: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get one exercise using its ID
    suspend fun getExercise(exerciseId: Int): Result<Exercise> {
        return try {
            val response = apiService.getExercise(exerciseId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Failed to fetch exercise: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get exercises belonging to a specific muscle group
    suspend fun getExercisesByMuscleGroup(
        muscleGroup: String
    ): Result<List<Exercise>> {
        return try {
            val response =
                apiService.getExercisesByMuscleGroup(muscleGroup)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception(
                        "Failed to fetch exercises for $muscleGroup: ${response.code()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Create a new custom exercise
    suspend fun createExercise(
        exercise: Exercise
    ): Result<Exercise> {
        return try {

            val response =
                apiService.createExercise(exercise)

            if (response.isSuccessful && response.body() != null) {

                Result.success(response.body()!!)

            } else {

                Result.failure(
                    Exception(
                        "Failed to create exercise: ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // Delete an exercise
    suspend fun deleteExercise(
        exerciseId: Int,
        userId: Int
    ): Result<Unit> {

        return try {

            val response = apiService.deleteExercise(exerciseId, userId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else if (response.code() == 403) {
                Result.failure(Exception("You can only delete exercises you created."))
            } else {
                Result.failure(Exception("Failed to delete exercise: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}