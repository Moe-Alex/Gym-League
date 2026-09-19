package com.gymleague.app.models

data class UserSettings(
    val userSettingsId: Int,
    val userId: Int,
    val notificationsEnabled: Boolean,
    val appearanceMode: String,
    val language: String
)