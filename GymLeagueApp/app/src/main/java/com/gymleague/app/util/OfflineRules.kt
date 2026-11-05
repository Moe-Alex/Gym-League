package com.gymleague.app.util

import com.gymleague.app.models.CreateWorkoutRequest
import com.gymleague.app.models.Workout
import java.io.IOException

/** The API answered, but with an error (for example 400 or 503). [statusCode] is the HTTP code. */
class ApiException(val statusCode: Int, message: String) : Exception(message)

/** The phone has no internet, so the action cannot be done right now. */
class OfflineException(message: String) : Exception(message)

/** What the offline banner at the bottom of the screen should say. */
enum class BannerState { HIDDEN, OFFLINE, OFFLINE_PENDING, SYNCING }

/**
 * The rules behind offline mode. They are kept free of Android classes so they can be
 * unit tested on the computer (and by GitHub Actions) without a phone.
 */
object OfflineRules {

    // A workout the server keeps refusing is tried this many times, then left alone
    const val MAX_SYNC_ATTEMPTS = 5

    /** True when the error means "could not reach the server": no signal, airplane mode, timeout. */
    fun isNetworkFailure(error: Throwable?): Boolean =
        error is IOException || error is OfflineException

    /** HTTP 5xx means the server is down or still waking up (Render's free plan sleeps). */
    fun isServerUnavailable(httpCode: Int): Boolean = httpCode >= 500

    /** Should we show the saved copy after this failure instead of an error? */
    fun canUseCache(error: Throwable?): Boolean {
        if (isNetworkFailure(error)) return true
        return error is ApiException && isServerUnavailable(error.statusCode)
    }

    /** Decides which banner to show from the connection and the number of unsynced workouts. */
    fun bannerState(isOnline: Boolean, pendingCount: Int): BannerState {
        return when {
            !isOnline && pendingCount > 0 -> BannerState.OFFLINE_PENDING
            !isOnline -> BannerState.OFFLINE
            pendingCount > 0 -> BannerState.SYNCING
            else -> BannerState.HIDDEN
        }
    }

    /** A workout that keeps being refused stops being retried after [MAX_SYNC_ATTEMPTS] tries. */
    fun shouldRetrySync(attempts: Int): Boolean = attempts < MAX_SYNC_ATTEMPTS

    /**
     * A workout saved offline has no server id yet, so it gets a negative one made from its
     * local row number. A negative id can never clash with a real server id.
     */
    fun pendingWorkoutId(localId: Long): Int = -localId.toInt()

    fun isPendingWorkoutId(workoutId: Int): Boolean = workoutId < 0

    // "2026-11-05T10:20:30Z", "2026-11-05T10:20:30.123+00:00" and "2026-11-05T10:20:30" all match
    fun normaliseDate(raw: String): String = raw.take(19)

    /**
     * True when the server already holds this workout. It stops the same workout being sent twice
     * if a request reached the server but the phone never got the answer.
     */
    fun isAlreadyOnServer(pending: CreateWorkoutRequest, serverWorkouts: List<Workout>): Boolean {
        val pendingDate = normaliseDate(pending.workoutDate)

        return serverWorkouts.any { server ->
            server.userId == pending.userId &&
                    server.workoutName == pending.workoutName &&
                    normaliseDate(server.workoutDate) == pendingDate
        }
    }

    /** Finds the matching workout on the server, or null. */
    fun findOnServer(pending: CreateWorkoutRequest, serverWorkouts: List<Workout>): Workout? {
        val pendingDate = normaliseDate(pending.workoutDate)

        return serverWorkouts.firstOrNull { server ->
            server.userId == pending.userId &&
                    server.workoutName == pending.workoutName &&
                    normaliseDate(server.workoutDate) == pendingDate
        }
    }
}

/** The keys used to remember when each saved list was last refreshed from the server. */
object CacheKeys {
    const val EXERCISES = "exercises"
    const val SQUADS = "squads"

    fun workouts(userId: Int) = "workouts:$userId"
    fun user(userId: Int) = "user:$userId"
    fun challenges(userId: Int) = "challenges:$userId"
    fun records(userId: Int) = "records:$userId"
    fun muscleVolume(userId: Int) = "volume:$userId"
}