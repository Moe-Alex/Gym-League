package com.gymleague.app.repository

import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.database.toEntity
import com.gymleague.app.database.toModel
import com.gymleague.app.models.Exercise
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.util.CacheKeys
import com.gymleague.app.util.OfflineLoader
import com.gymleague.app.util.toApiResult
import com.gymleague.app.util.toFailure

class ExerciseRepository {

    private val apiService = RetrofitClient.apiService

    private val db get() = GymLeagueApplication.instance.database

    // Get all exercises. Saved to Room, and read from Room when offline.
    suspend fun getExercises(): Result<List<Exercise>> {
        return OfflineLoader.load<List<Exercise>>(
            db = db,
            cacheKey = CacheKeys.EXERCISES,
            label = "exercises",
            sizeOf = { it.size },
            fetch = {
                try {
                    apiService.getExercises().toApiResult("Failed to fetch exercises")
                } catch (e: Exception) {
                    e.toFailure()
                }
            },
            save = { list -> db.exerciseDao().replaceAll(list.map { it.toEntity() }) },
            read = { db.exerciseDao().getAll().map { it.toModel() } }
        )
    }

    // Get one exercise using its ID (online only)
    suspend fun getExercise(exerciseId: Int): Result<Exercise> {
        return try {
            apiService.getExercise(exerciseId).toApiResult("Failed to fetch exercise")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    // Get exercises belonging to a specific muscle group.
    // Offline, the saved list of all exercises is filtered instead.
    suspend fun getExercisesByMuscleGroup(
        muscleGroup: String
    ): Result<List<Exercise>> {
        return OfflineLoader.load<List<Exercise>>(
            db = db,
            cacheKey = CacheKeys.EXERCISES,
            label = "exercises",
            sizeOf = { it.size },
            fetch = {
                try {
                    apiService.getExercisesByMuscleGroup(muscleGroup)
                        .toApiResult("Failed to fetch exercises for $muscleGroup")
                } catch (e: Exception) {
                    e.toFailure()
                }
            },
            // Only part of the list was downloaded, so it is not saved over the full copy
            save = null,
            read = { db.exerciseDao().getByMuscleGroup(muscleGroup).map { it.toModel() } }
        )
    }

    // Create a new custom exercise (needs internet)
    suspend fun createExercise(
        exercise: Exercise
    ): Result<Exercise> {
        return try {
            apiService.createExercise(exercise).toApiResult("Failed to create exercise")
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    // Delete an exercise (needs internet)
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
            e.toFailure()
        }
    }
}