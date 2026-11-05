package com.gymleague.app.util

import android.util.Log
import com.gymleague.app.R
import com.gymleague.app.database.CacheMetaEntity
import com.gymleague.app.database.GymLeagueDatabase
import kotlinx.coroutines.CancellationException

/**
 * Shared "network first, saved copy as backup" logic for every list the app shows.
 *
 * 1. Online: ask the server, save the answer to Room, return it.
 * 2. Offline (or the server is down): return the copy saved earlier in Room.
 * 3. Nothing saved yet: return a clear error.
 * The server is always the truth, so a good answer from it replaces the saved copy.
 */
object OfflineLoader {

    private const val TAG = "GymLeagueOffline"

    suspend fun <T> load(
        db: GymLeagueDatabase,
        cacheKey: String,
        label: String,
        sizeOf: (T) -> Int,
        fetch: suspend () -> Result<T>,
        save: (suspend (T) -> Unit)?,
        read: suspend () -> T?
    ): Result<T> {

        var failure: Throwable? = null

        if (NetworkMonitor.isOnline.value) {
            val fresh = fetch()

            if (fresh.isSuccess) {
                val data = fresh.getOrNull()

                if (data != null && save != null) {
                    try {
                        save(data)
                        db.cacheMetaDao().put(CacheMetaEntity(cacheKey, System.currentTimeMillis()))
                        Log.d(TAG, "Saved ${sizeOf(data)} $label to Room")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // A problem saving must never stop the user seeing fresh data
                        Log.w(TAG, "Could not save $label to Room: ${e.javaClass.simpleName}")
                    }
                }

                return fresh
            }

            failure = fresh.exceptionOrNull()

            // A real answer such as "not found" or "bad request" is shown as it is
            if (!OfflineRules.canUseCache(failure)) {
                return fresh
            }
        }

        val savedBefore = db.cacheMetaDao().get(cacheKey) != null
        val cached = if (savedBefore) read() else null

        if (cached != null) {
            Log.d(TAG, "No network, loaded ${sizeOf(cached)} $label from Room")
            return Result.success(cached)
        }

        Log.d(TAG, "No network and nothing saved for $label yet")
        return Result.failure(
            OfflineException(AppText.get(R.string.offline_not_saved, localisedLabel(label)))
        )
    }

    // The repositories name each list in English for the log. The message the user sees is translated.
    private fun localisedLabel(label: String): String {
        val id = when (label) {
            "exercises" -> R.string.offline_label_exercises
            "squads" -> R.string.offline_label_squads
            "workouts" -> R.string.offline_label_workouts
            "user stats" -> R.string.offline_label_user_stats
            "challenges" -> R.string.offline_label_challenges
            "personal records" -> R.string.offline_label_records
            "muscle volume rows" -> R.string.offline_label_muscle_volume
            else -> return label
        }
        return AppText.get(id)
    }
}
