package com.gymleague.app.models

/**
 * A Battle Arena challenge: a head-to-head race between its creator and an opponent.
 * The first person to reach [target] wins and earns [xpReward] XP.
 *
 * [currentProgress] is the creator's progress and [opponentProgress] is the opponent's.
 * [status] is one of Open, Pending, Active, Completed or Declined.
 */
data class Challenge(
    val challengeId: Int,
    val title: String,
    val description: String?,
    val type: String,
    val target: Int,
    val xpReward: Int,
    val currentProgress: Int,
    val opponentProgress: Int,
    val completed: Boolean,
    val endDate: String,
    val createdByUserId: Int,
    val createdByUsername: String?,
    val opponentUserId: Int?,
    val opponentUsername: String?,
    val status: String?,
    val winnerUserId: Int?,
    val createdAt: String?
)

data class CreateChallengeRequest(
    val title: String,
    val description: String,
    val type: String,
    val target: Int,
    val xpReward: Int,
    val endDate: String,
    val createdByUserId: Int,
    // Optional: challenge one specific person (for example from the Squad screen)
    val opponentUserId: Int? = null
)
