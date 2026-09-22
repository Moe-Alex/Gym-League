package com.gymleague.app.models

data class Squad(
    val squadId: Int,
    val squadName: String,
    val sharedStreak: Int,
    val createdAt: String?,
    val members: List<SquadMember> = emptyList()
)

data class SquadMember(
    val squadMemberId: Int,
    val squadId: Int,
    val userId: Int,
    val currentStreak: Int,
    val joinedAt: String?,
    val user: User?
)

data class CreateSquadRequest(
    val squadName: String,
    val sharedStreak: Int = 0
)

data class AddSquadMemberRequest(
    val userId: Int
)