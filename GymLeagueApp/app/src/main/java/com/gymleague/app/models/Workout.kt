package com.gymleague.app.models

data class Workout(
    val workoutId: Int,
    val userId: Int,
    val workoutName: String,
    val workoutDate: String,
    val durationMinutes: Int,
    val completed: Boolean,
    val xpEarned: Int
)

data class CreateWorkoutRequest(
    val userId: Int,
    val workoutName: String,
    val workoutDate: String,
    val durationMinutes: Int,
    val completed: Boolean,
    val xpEarned: Int
)