package com.gymleague.app.util

import com.gymleague.app.models.Challenge
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** What the badge on a challenge card says. */
enum class ChallengeBadge { OPEN, INVITED, PENDING, LIVE, WON, LOST, ENDED, DECLINED }

/** What the signed-in user can do with a challenge right now. */
enum class ChallengeAction { NONE, ACCEPT, ACCEPT_OR_DECLINE, LOG_PROGRESS }

/**
 * Battle Arena display rules, kept free of Android classes so they can be unit tested.
 * They mirror the rules the API enforces in ChallengeRules.cs.
 */
object ChallengeHelper {

    const val STATUS_OPEN = "Open"
    const val STATUS_PENDING = "Pending"
    const val STATUS_ACTIVE = "Active"
    const val STATUS_COMPLETED = "Completed"
    const val STATUS_DECLINED = "Declined"

    // Older challenges may not have a status, so fall back to the old flags
    fun statusOf(challenge: Challenge): String {
        if (challenge.completed) return STATUS_COMPLETED
        return challenge.status?.takeIf { it.isNotBlank() } ?: STATUS_OPEN
    }

    fun isCreator(challenge: Challenge, userId: Int) = challenge.createdByUserId == userId

    fun isOpponent(challenge: Challenge, userId: Int) = challenge.opponentUserId == userId

    fun isParticipant(challenge: Challenge, userId: Int) =
        isCreator(challenge, userId) || isOpponent(challenge, userId)

    fun myProgress(challenge: Challenge, userId: Int): Int =
        if (isCreator(challenge, userId)) challenge.currentProgress else challenge.opponentProgress

    fun rivalProgress(challenge: Challenge, userId: Int): Int =
        if (isCreator(challenge, userId)) challenge.opponentProgress else challenge.currentProgress

    // The other person in the battle (null while nobody has accepted yet)
    fun rivalName(challenge: Challenge, userId: Int): String? =
        if (isCreator(challenge, userId)) challenge.opponentUsername else challenge.createdByUsername

    fun parseDate(raw: String): LocalDate? {
        return try {
            LocalDate.parse(raw.take(10))
        } catch (e: DateTimeParseException) {
            null
        }
    }

    // A challenge is open for the whole of its end date
    fun hasEnded(endDate: String, today: LocalDate): Boolean {
        val end = parseDate(endDate) ?: return false
        return today.isAfter(end)
    }

    fun badge(challenge: Challenge, userId: Int, today: LocalDate): ChallengeBadge {
        return when (statusOf(challenge)) {
            STATUS_COMPLETED ->
                if (challenge.winnerUserId == userId) ChallengeBadge.WON else ChallengeBadge.LOST

            STATUS_DECLINED -> ChallengeBadge.DECLINED

            else -> when {
                hasEnded(challenge.endDate, today) -> ChallengeBadge.ENDED

                statusOf(challenge) == STATUS_ACTIVE -> ChallengeBadge.LIVE

                statusOf(challenge) == STATUS_PENDING ->
                    if (isOpponent(challenge, userId)) ChallengeBadge.INVITED else ChallengeBadge.PENDING

                else -> ChallengeBadge.OPEN
            }
        }
    }

    fun action(challenge: Challenge, userId: Int, today: LocalDate): ChallengeAction {
        val status = statusOf(challenge)

        if (status == STATUS_COMPLETED || status == STATUS_DECLINED) return ChallengeAction.NONE
        if (hasEnded(challenge.endDate, today)) return ChallengeAction.NONE

        return when (status) {
            STATUS_ACTIVE ->
                if (isParticipant(challenge, userId)) ChallengeAction.LOG_PROGRESS else ChallengeAction.NONE

            STATUS_PENDING ->
                if (isOpponent(challenge, userId)) ChallengeAction.ACCEPT_OR_DECLINE else ChallengeAction.NONE

            else ->
                if (isCreator(challenge, userId)) ChallengeAction.NONE else ChallengeAction.ACCEPT
        }
    }

    // The progress bars are only meaningful once the battle has started
    fun showsProgress(challenge: Challenge): Boolean {
        val status = statusOf(challenge)
        return status == STATUS_ACTIVE || status == STATUS_COMPLETED
    }

    // True when the end date typed into the "new challenge" form is acceptable.
    fun isValidEndDate(raw: String, today: LocalDate): Boolean {
        val date = try {
            LocalDate.parse(raw.trim())
        } catch (e: DateTimeParseException) {
            return false
        }

        return !date.isBefore(today)
    }
}
