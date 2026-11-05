package com.gymleague.app.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gymleague.app.database.GymLeagueDatabase
import com.gymleague.app.network.RetrofitClient
import java.util.concurrent.TimeUnit

/**
 * Runs in the background when the phone has internet and sends the workouts logged offline.
 * WorkManager keeps the job even if the app is closed.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = GymLeagueDatabase.getInstance(applicationContext)
        val outcome = WorkoutSync(db, RetrofitClient.apiService).syncPending()

        return if (outcome.retryLater && runAttemptCount < 10) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}

object SyncManager {

    private const val TAG = "GymLeagueOffline"
    private const val WORK_NAME = "sync-pending-workouts"

    /** Asks WorkManager to sync as soon as the phone is connected. Safe to call many times. */
    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)

        Log.d(TAG, "Sync scheduled, it will run when the phone is connected")
    }
}