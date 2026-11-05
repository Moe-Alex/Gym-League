package com.gymleague.app.repository

import android.util.Log
import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.database.toCreateRequest
import com.gymleague.app.database.toModel
import com.gymleague.app.database.toPendingEntity
import com.gymleague.app.database.toSyncedEntity
import com.gymleague.app.models.CreateWorkoutRequest
import com.gymleague.app.models.Workout
import com.gymleague.app.network.RetrofitClient
import com.gymleague.app.sync.SyncManager
import com.gymleague.app.util.CacheKeys
import com.gymleague.app.util.NetworkMonitor
import com.gymleague.app.util.OfflineLoader
import com.gymleague.app.util.OfflineRules
import com.gymleague.app.util.toApiResult
import java.io.IOException

class WorkoutRepository {

    private companion object {
        const val TAG = "GymLeagueOffline"
    }

    private val apiService = RetrofitClient.apiService

    private val db get() = GymLeagueApplication.instance.database

    /**
     * Saves a workout. Online it goes straight to the server. Offline (or if the connection
     * drops) it is saved on the phone with a "pending sync" flag and sent later. The caller gets
     * a success either way. A workout that is still pending has a negative workoutId.
     */
    suspend fun createWorkout(
        workout: CreateWorkoutRequest
    ): Result<Workout> {

        if (!NetworkMonitor.isOnline.value) {
            return saveForLater(workout)
        }

        return try {

            val response = apiService.createWorkout(workout)
            val body = response.body()

            if (response.isSuccessful && body != null) {

                // Keep a copy so it still shows offline
                db.workoutDao().insertAll(listOf(body.toSyncedEntity()))
                Result.success(body)

            } else if (OfflineRules.isServerUnavailable(response.code())) {

                // Server asleep or down: keep the workout and send it later
                Log.w(TAG, "Server unavailable (${response.code()}), saving workout for later")
                saveForLater(workout)

            } else {

                val errorBody = response.errorBody()?.string()

                Result.failure(
                    Exception("Error ${response.code()}: $errorBody")
                )
            }

        } catch (e: IOException) {

            // The connection dropped: keep the workout and send it later
            Log.w(TAG, "Connection failed while saving, saving workout for later")
            saveForLater(workout)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // Stores the workout in Room with the pending flag and asks WorkManager to sync it
    private suspend fun saveForLater(workout: CreateWorkoutRequest): Result<Workout> {
        return try {
            val localId = db.workoutDao().insert(workout.toPendingEntity())
            val saved = workout.toPendingEntity().copy(localId = localId).toModel()

            Log.d(TAG, "Saved workout offline (pending sync), local id $localId")
            SyncManager.schedule(GymLeagueApplication.instance)

            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * The user's workouts: the server's list plus any still waiting to sync.
     * Offline, the copy saved in Room is shown.
     */
    suspend fun getWorkoutsForUser(userId: Int): Result<List<Workout>> {

        val loaded = OfflineLoader.load<List<Workout>>(
            db = db,
            cacheKey = CacheKeys.workouts(userId),
            label = "workouts",
            sizeOf = { it.size },
            fetch = {
                try {
                    apiService.getWorkoutsForUser(userId).toApiResult("Failed to fetch workouts")
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            save = { list ->
                db.workoutDao().replaceSyncedForUser(userId, list.map { it.toSyncedEntity() })
            },
            read = { db.workoutDao().getSyncedForUser(userId).map { it.toModel() } }
        )

        val waiting = db.workoutDao().getUnsyncedForUser(userId)

        if (loaded.isFailure) {
            // Nothing saved from the server yet, but the user's offline workouts can still be shown
            return if (waiting.isNotEmpty() && OfflineRules.isNetworkFailure(loaded.exceptionOrNull())) {
                Result.success(waiting.map { it.toModel() }.reversed())
            } else {
                loaded
            }
        }

        val fromServer = loaded.getOrNull() ?: emptyList()

        // A pending workout the server already has (answer lost on the way) is not shown twice
        val stillPending = waiting
            .filter { !OfflineRules.isAlreadyOnServer(it.toCreateRequest(), fromServer) }
            .map { it.toModel() }
            .reversed()

        return Result.success(stillPending + fromServer)
    }
}