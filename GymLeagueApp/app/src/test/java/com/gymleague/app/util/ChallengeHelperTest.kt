package com.gymleague.app.util

import com.gymleague.app.models.Challenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChallengeHelperTest {

    private val today = LocalDate.of(2026, 11, 4)
    private val me = 1
    private val rival = 2
    private val stranger = 3

    private fun challenge(
        status: String? = "Open",
        creator: Int = me,
        opponent: Int? = null,
        endDate: String = "2026-12-01",
        progress: Int = 0,
        opponentProgress: Int = 0,
        completed: Boolean = false,
        winner: Int? = null
    ) = Challenge(
        challengeId = 10,
        title = "Squat war",
        description = null,
        type = "Workouts",
        target = 5,
        xpReward = 100,
        currentProgress = progress,
        opponentProgress = opponentProgress,
        completed = completed,
        endDate = endDate,
        createdByUserId = creator,
        createdByUsername = "creator",
        opponentUserId = opponent,
        opponentUsername = if (opponent == null) null else "opponent",
        status = status,
        winnerUserId = winner,
        createdAt = null
    )

    // ---- status ----

    @Test
    fun statusOf_missingStatus_fallsBackToOpen() {
        assertEquals("Open", ChallengeHelper.statusOf(challenge(status = null)))
        assertEquals("Open", ChallengeHelper.statusOf(challenge(status = "  ")))
    }

    @Test
    fun statusOf_completedFlagWins() {
        assertEquals("Completed", ChallengeHelper.statusOf(challenge(status = "Active", completed = true)))
    }

    // ---- progress ----

    @Test
    fun progress_isSwappedForTheOpponent() {
        val c = challenge(opponent = rival, progress = 3, opponentProgress = 1)

        assertEquals(3, ChallengeHelper.myProgress(c, me))
        assertEquals(1, ChallengeHelper.rivalProgress(c, me))
        assertEquals(1, ChallengeHelper.myProgress(c, rival))
        assertEquals(3, ChallengeHelper.rivalProgress(c, rival))
    }

    @Test
    fun rivalName_dependsOnWhoIsLooking() {
        val c = challenge(opponent = rival)

        assertEquals("opponent", ChallengeHelper.rivalName(c, me))
        assertEquals("creator", ChallengeHelper.rivalName(c, rival))
        assertNull(ChallengeHelper.rivalName(challenge(), me))
    }

    // ---- dates ----

    @Test
    fun hasEnded_endDateItselfIsStillOpen() {
        assertFalse(ChallengeHelper.hasEnded("2026-11-04", today))
        assertTrue(ChallengeHelper.hasEnded("2026-11-03", today))
    }

    @Test
    fun hasEnded_acceptsFullTimestamps() {
        assertFalse(ChallengeHelper.hasEnded("2026-11-04T00:00:00", today))
        assertTrue(ChallengeHelper.hasEnded("2026-11-01T23:59:59", today))
    }

    @Test
    fun hasEnded_unreadableDate_isNotEnded() {
        assertFalse(ChallengeHelper.hasEnded("soon", today))
    }

    @Test
    fun isValidEndDate_rules() {
        assertTrue(ChallengeHelper.isValidEndDate("2026-11-04", today))
        assertTrue(ChallengeHelper.isValidEndDate(" 2027-01-01 ", today))
        assertFalse(ChallengeHelper.isValidEndDate("2026-11-03", today))
        assertFalse(ChallengeHelper.isValidEndDate("04/11/2026", today))
        assertFalse(ChallengeHelper.isValidEndDate("", today))
    }

    // ---- badges ----

    @Test
    fun badge_openChallenge() {
        assertEquals(ChallengeBadge.OPEN, ChallengeHelper.badge(challenge(), stranger, today))
    }

    @Test
    fun badge_pendingIsInvitedForTheOpponentAndPendingForOthers() {
        val c = challenge(status = "Pending", opponent = rival)

        assertEquals(ChallengeBadge.INVITED, ChallengeHelper.badge(c, rival, today))
        assertEquals(ChallengeBadge.PENDING, ChallengeHelper.badge(c, me, today))
    }

    @Test
    fun badge_activeIsLive() {
        val c = challenge(status = "Active", opponent = rival)
        assertEquals(ChallengeBadge.LIVE, ChallengeHelper.badge(c, me, today))
    }

    @Test
    fun badge_completedShowsWonOrLost() {
        val c = challenge(status = "Completed", opponent = rival, completed = true, winner = me)

        assertEquals(ChallengeBadge.WON, ChallengeHelper.badge(c, me, today))
        assertEquals(ChallengeBadge.LOST, ChallengeHelper.badge(c, rival, today))
    }

    @Test
    fun badge_declined() {
        val c = challenge(status = "Declined", opponent = rival)
        assertEquals(ChallengeBadge.DECLINED, ChallengeHelper.badge(c, me, today))
    }

    @Test
    fun badge_expiredUnfinishedChallengeIsEnded() {
        val c = challenge(status = "Active", opponent = rival, endDate = "2026-10-01")
        assertEquals(ChallengeBadge.ENDED, ChallengeHelper.badge(c, me, today))
    }

    // ---- actions ----

    @Test
    fun action_creatorCannotAcceptTheirOwnOpenChallenge() {
        assertEquals(ChallengeAction.NONE, ChallengeHelper.action(challenge(), me, today))
    }

    @Test
    fun action_anyoneElseCanAcceptAnOpenChallenge() {
        assertEquals(ChallengeAction.ACCEPT, ChallengeHelper.action(challenge(), stranger, today))
    }

    @Test
    fun action_invitedOpponentCanAcceptOrDecline() {
        val c = challenge(status = "Pending", opponent = rival)

        assertEquals(ChallengeAction.ACCEPT_OR_DECLINE, ChallengeHelper.action(c, rival, today))
        assertEquals(ChallengeAction.NONE, ChallengeHelper.action(c, me, today))
        assertEquals(ChallengeAction.NONE, ChallengeHelper.action(c, stranger, today))
    }

    @Test
    fun action_onlyParticipantsCanLogProgressOnALiveBattle() {
        val c = challenge(status = "Active", opponent = rival)

        assertEquals(ChallengeAction.LOG_PROGRESS, ChallengeHelper.action(c, me, today))
        assertEquals(ChallengeAction.LOG_PROGRESS, ChallengeHelper.action(c, rival, today))
        assertEquals(ChallengeAction.NONE, ChallengeHelper.action(c, stranger, today))
    }

    @Test
    fun action_nothingToDoOnFinishedOrExpiredBattles() {
        val done = challenge(status = "Completed", opponent = rival, completed = true, winner = me)
        val declined = challenge(status = "Declined", opponent = rival)
        val expired = challenge(status = "Active", opponent = rival, endDate = "2026-10-01")

        assertEquals(ChallengeAction.NONE, ChallengeHelper.action(done, me, today))
        assertEquals(ChallengeAction.NONE, ChallengeHelper.action(declined, rival, today))
        assertEquals(ChallengeAction.NONE, ChallengeHelper.action(expired, me, today))
    }

    @Test
    fun showsProgress_onlyOnceTheBattleHasStarted() {
        assertFalse(ChallengeHelper.showsProgress(challenge()))
        assertFalse(ChallengeHelper.showsProgress(challenge(status = "Pending", opponent = rival)))
        assertTrue(ChallengeHelper.showsProgress(challenge(status = "Active", opponent = rival)))
        assertTrue(ChallengeHelper.showsProgress(challenge(status = "Completed", completed = true)))
    }
}
