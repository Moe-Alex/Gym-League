package com.gymleague.app.util

import com.gymleague.app.models.CreateWorkoutRequest
import com.gymleague.app.models.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OfflineRulesTest {

    private fun pending(
        userId: Int = 1,
        name: String = "Bench Press",
        date: String = "2026-11-05T10:20:30Z"
    ) = CreateWorkoutRequest(
        userId = userId,
        workoutName = name,
        workoutDate = date,
        durationMinutes = 30,
        completed = true,
        xpEarned = 30,
        workoutExercises = emptyList()
    )

    private fun onServer(
        id: Int = 10,
        userId: Int = 1,
        name: String = "Bench Press",
        date: String = "2026-11-05T10:20:30Z"
    ) = Workout(
        workoutId = id,
        userId = userId,
        workoutName = name,
        workoutDate = date,
        durationMinutes = 30,
        completed = true,
        xpEarned = 30,
        workoutExercises = emptyList()
    )

    // ---- Which errors count as "offline" ----

    @Test
    fun networkErrors_areNetworkFailures() {
        assertTrue(OfflineRules.isNetworkFailure(UnknownHostException("no host")))
        assertTrue(OfflineRules.isNetworkFailure(SocketTimeoutException("timeout")))
        assertTrue(OfflineRules.isNetworkFailure(IOException("lost")))
        assertTrue(OfflineRules.isNetworkFailure(OfflineException("offline")))
    }

    @Test
    fun otherErrors_areNotNetworkFailures() {
        assertFalse(OfflineRules.isNetworkFailure(IllegalStateException("bug")))
        assertFalse(OfflineRules.isNetworkFailure(ApiException(400, "bad request")))
        assertFalse(OfflineRules.isNetworkFailure(null))
    }

    @Test
    fun cacheIsUsedForOfflineAndServerDown() {
        assertTrue(OfflineRules.canUseCache(UnknownHostException()))
        assertTrue(OfflineRules.canUseCache(ApiException(503, "waking up")))
        assertTrue(OfflineRules.canUseCache(ApiException(500, "server error")))
    }

    @Test
    fun cacheIsNotUsedForRealAnswers() {
        assertFalse(OfflineRules.canUseCache(ApiException(404, "not found")))
        assertFalse(OfflineRules.canUseCache(ApiException(400, "bad request")))
        assertFalse(OfflineRules.canUseCache(IllegalArgumentException("bug")))
        assertFalse(OfflineRules.canUseCache(null))
    }

    // ---- The offline banner ----

    @Test
    fun banner_offlineWithNothingWaiting() {
        assertEquals(BannerState.OFFLINE, OfflineRules.bannerState(false, 0))
    }

    @Test
    fun banner_offlineWithWorkoutsWaiting() {
        assertEquals(BannerState.OFFLINE_PENDING, OfflineRules.bannerState(false, 2))
    }

    @Test
    fun banner_onlineWithWorkoutsWaitingIsSyncing() {
        assertEquals(BannerState.SYNCING, OfflineRules.bannerState(true, 1))
    }

    @Test
    fun banner_onlineAndNothingWaitingIsHidden() {
        assertEquals(BannerState.HIDDEN, OfflineRules.bannerState(true, 0))
    }

    // ---- Retrying ----

    @Test
    fun syncIsRetriedUntilTheLimit() {
        assertTrue(OfflineRules.shouldRetrySync(0))
        assertTrue(OfflineRules.shouldRetrySync(OfflineRules.MAX_SYNC_ATTEMPTS - 1))
        assertFalse(OfflineRules.shouldRetrySync(OfflineRules.MAX_SYNC_ATTEMPTS))
    }

    // ---- Ids for workouts not on the server yet ----

    @Test
    fun pendingWorkoutIds_areNegative() {
        assertEquals(-7, OfflineRules.pendingWorkoutId(7L))
        assertTrue(OfflineRules.isPendingWorkoutId(OfflineRules.pendingWorkoutId(1L)))
        assertFalse(OfflineRules.isPendingWorkoutId(12))
    }

    // ---- Not sending the same workout twice ----

    @Test
    fun dates_ignoreFractionsAndTimeZoneSuffix() {
        assertEquals("2026-11-05T10:20:30", OfflineRules.normaliseDate("2026-11-05T10:20:30Z"))
        assertEquals("2026-11-05T10:20:30", OfflineRules.normaliseDate("2026-11-05T10:20:30.123+00:00"))
    }

    @Test
    fun matchingWorkoutOnServer_isFound() {
        val server = listOf(onServer(id = 42, date = "2026-11-05T10:20:30.000Z"))

        assertTrue(OfflineRules.isAlreadyOnServer(pending(), server))
        assertEquals(42, OfflineRules.findOnServer(pending(), server)?.workoutId)
    }

    @Test
    fun differentWorkout_isNotMatched() {
        val server = listOf(
            onServer(name = "Squat"),
            onServer(userId = 2),
            onServer(date = "2026-11-05T10:20:31Z")
        )

        assertFalse(OfflineRules.isAlreadyOnServer(pending(), server))
        assertNull(OfflineRules.findOnServer(pending(), server))
    }

    @Test
    fun emptyServerList_matchesNothing() {
        assertFalse(OfflineRules.isAlreadyOnServer(pending(), emptyList()))
        assertNotNull(OfflineRules.findOnServer(pending(), listOf(onServer())))
    }

    // ---- Cache keys ----

    @Test
    fun cacheKeys_areDifferentForEachUser() {
        assertEquals("workouts:1", CacheKeys.workouts(1))
        assertTrue(CacheKeys.workouts(1) != CacheKeys.workouts(2))
        assertTrue(CacheKeys.challenges(1) != CacheKeys.records(1))
    }
}