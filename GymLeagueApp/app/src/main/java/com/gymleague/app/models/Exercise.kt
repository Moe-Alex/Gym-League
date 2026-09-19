package com.gymleague.app.models

data class Exercise(
    val exerciseId: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val description: String?,
    val videoUrl: String?,
    val defaultSets: Int,
    val defaultReps: Int
)