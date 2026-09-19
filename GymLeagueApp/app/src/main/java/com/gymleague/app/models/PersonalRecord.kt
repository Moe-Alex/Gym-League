package com.gymleague.app.models

data class PersonalRecord(
    val exerciseId: Int,
    val exerciseName: String,
    val heaviestWeight: Double,
    val repsAtHeaviestWeight: Int,
    val estimatedOneRepMax: Double,
    val dateAchieved: String
)