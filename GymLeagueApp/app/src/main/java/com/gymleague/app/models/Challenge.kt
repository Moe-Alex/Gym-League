package com.gymleague.app.models

data class Challenge(
    val challengeId: Int,
    val title: String,
    val description: String,
    val type: String,
    val target: Int,
    val xpReward: Int,
    val currentProgress: Int,
    val completed: Boolean,
    val endDate: String,
    val createdByUserId: Int,
    val createdAt: String
)

data class CreateChallengeRequest(
    val title: String,
    val description: String,
    val type: String,
    val target: Int,
    val xpReward: Int,
    val endDate: String,
    val createdByUserId: Int
)