package com.gymleague.app.models

data class User(
    val userId: Int,
    val firebaseUid: String,
    val username: String,
    val email: String,
    val profileImageUrl: String?,
    val xp: Int,
    val level: Int,
    val createdAt: String
)

data class CreateUserRequest(
    val firebaseUid: String,
    val username: String,
    val email: String,
    val profileImageUrl: String? = null,
    val xp: Int = 0,
    val level: Int = 1
)