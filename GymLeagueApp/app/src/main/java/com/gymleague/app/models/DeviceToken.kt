package com.gymleague.app.models

/** Sent to the API so it knows which phone to push notifications to for this user. */
data class RegisterDeviceTokenRequest(
    val userId: Int,
    val token: String
)
