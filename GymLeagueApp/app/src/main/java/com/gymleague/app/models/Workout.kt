package com.gymleague.app.models

data class Workout(
    val workoutId: Int,
    val userId: Int,
    val workoutName: String,
    val workoutDate: String,
    val durationMinutes: Int,
    val completed: Boolean,
    val xpEarned: Int,
    val workoutExercises: List<WorkoutExercise> = emptyList()
)

data class WorkoutExercise(
    val workoutExerciseId: Int = 0,
    val workoutId: Int = 0,
    val exerciseId: Int,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val completed: Boolean = true
)

data class CreateWorkoutRequest(
    val userId: Int,
    val workoutName: String,
    val workoutDate: String,
    val durationMinutes: Int,
    val completed: Boolean,
    val xpEarned: Int,
    val workoutExercises: List<WorkoutExercise>
)