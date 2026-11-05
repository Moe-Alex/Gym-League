package com.gymleague.app.sync

import android.util.Log
import com.gymleague.app.database.GymLeagueDatabase
import com.gymleague.app.database.toCreateRequest
import com.gymleague.app.models.Workout
import com.gymleague.app.network.GymLeagueApiService
import com.gymleague.app.util.OfflineRules
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** What happened in one sync run. [retryLater] is true when the connection or server failed halfway. */
data class SyncResult(val synced: Int, val refused: Int, val retryLater: Boolean)

/**
 * Sends workouts logged offline to the server.
 *  - A workout keeps its "pending" flag until the server confirms it.
 *  - If the connection drops halfway, the rest stay pending and are tried again later.
 *  - Before sending, the server's list is checked, so nothing is sent twice.
 */
class WorkoutSync(
    private val db: GymLeagueDatabase,
    private val api: GymLeagueApiService
) {

    companion object {
        private const val TAG = "GymLeagueOffline"

        // Only one sync may run at a time, which also prevents duplicate sends
        private val lock = Mutex()
    }

    suspend fun syncPending(): SyncResult = lock.withLock {

        val pending = db.workoutDao().getPending(OfflineRules.MAX_SYNC_ATTEMPTS)

        if (pending.isEmpty()) {
            return@withLock SyncResult(0, 0, false)
        }

        Log.d(TAG, "Found ${pending.size} pending workouts to sync")

        var synced = 0
        var refused = 0

        // The server's workouts for each user, loaded once per run
        val serverLists = mutableMapOf<Int, List<Workout>>()

        for (item in pending) {
            try {
                val serverWorkouts = serverLists[item.userId] ?: loadServerList(item.userId)

                if (serverWorkouts == null) {
                    // Could not reach the server, leave everything pending
                    return@withLock SyncResult(synced, refused, true)
                }

                serverLists[item.userId] = serverWorkouts

                val request = item.toCreateRequest()
                val alreadyThere = OfflineRules.findOnServer(request, serverWorkouts)

                if (alreadyThere != null) {
                    db.workoutDao().markSynced(item.localId, alreadyThere.workoutId)
                    Log.d(TAG, "Workout ${item.localId} was already on the server, marked as synced")
                    synced++
                    continue
                }

                val response = api.createWorkout(request)
                val body = response.body()

                when {
                    response.isSuccessful && body != null -> {
                        db.workoutDao().markSynced(item.localId, body.workoutId)
                        synced++
                    }

                    OfflineRules.isServerUnavailable(response.code()) -> {
                        // Server asleep or down: not the workout's fault, so try again later
                        Log.w(TAG, "Server unavailable (${response.code()}), will retry")
                        return@withLock SyncResult(synced, refused, true)
                    }

                    else -> {
                        // The server refused this workout, count the attempt and carry on
                        db.workoutDao().incrementAttempts(item.localId)
                        refused++
                        Log.w(TAG, "Server refused workout ${item.localId} (${response.code()})")
                    }
                }

            } catch (e: IOException) {
                Log.w(TAG, "Connection lost during sync, kept ${pending.size - synced} pending")
                return@withLock SyncResult(synced, refused, true)
            }
        }

        if (synced > 0) {
            Log.d(TAG, "Synced $synced pending workouts")
        }

        SyncResult(synced, refused, false)
    }

    // Returns null when the server could not be reached
    private suspend fun loadServerList(userId: Int): List<Workout>? {
        val response = api.getWorkoutsForUser(userId)
        val body = response.body()

        return if (response.isSuccessful && body != null) body else null
    }
}