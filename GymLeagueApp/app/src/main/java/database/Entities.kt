package com.gymleague.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gymleague.app.models.Challenge
import com.gymleague.app.models.CreateWorkoutRequest
import com.gymleague.app.models.Exercise
import com.gymleague.app.models.PersonalRecord
import com.gymleague.app.models.Squad
import com.gymleague.app.models.SquadMember
import com.gymleague.app.models.User
import com.gymleague.app.models.Workout
import com.gymleague.app.models.WorkoutExercise
import com.gymleague.app.util.JsonLists
import com.gymleague.app.util.OfflineRules

/*
 * Room entities: one table for each kind of data the app keeps on the phone.
 * The field names match the models in the models folder, which keeps the converting simple.
 * The server is always the truth. These tables are only a copy so the app works offline.
 */

// ---------------- Exercises ----------------

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val exerciseId: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val description: String?,
    val videoUrl: String?,
    val defaultSets: Int,
    val defaultReps: Int,
    val createdByUserId: Int?
)

fun Exercise.toEntity() = ExerciseEntity(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    muscleGroup = muscleGroup,
    description = description,
    videoUrl = videoUrl,
    defaultSets = defaultSets,
    defaultReps = defaultReps,
    createdByUserId = createdByUserId
)

fun ExerciseEntity.toModel() = Exercise(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    muscleGroup = muscleGroup,
    description = description,
    videoUrl = videoUrl,
    defaultSets = defaultSets,
    defaultReps = defaultReps,
    createdByUserId = createdByUserId
)

// ---------------- Workouts (also the offline "outbox") ----------------

/**
 * A workout. Rows copied from the server have synced = true and a serverId.
 * A workout logged while offline has synced = false and no serverId yet, so it is easy to find
 * later and send. [syncAttempts] counts how many times the server refused it.
 * The exercises inside the workout are saved as JSON text in [exercisesJson].
 */
@Entity(
    tableName = "workouts",
    indices = [Index("userId"), Index("synced")]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Int?,
    val userId: Int,
    val workoutName: String,
    val workoutDate: String,
    val durationMinutes: Int,
    val completed: Boolean,
    val xpEarned: Int,
    val exercisesJson: String,
    val synced: Boolean,
    val syncAttempts: Int = 0
)

// A workout that came from the server
fun Workout.toSyncedEntity() = WorkoutEntity(
    serverId = workoutId,
    userId = userId,
    workoutName = workoutName,
    workoutDate = workoutDate,
    durationMinutes = durationMinutes,
    completed = completed,
    xpEarned = xpEarned,
    exercisesJson = JsonLists.encode(workoutExercises),
    synced = true
)

// A workout logged offline, waiting to be sent
fun CreateWorkoutRequest.toPendingEntity() = WorkoutEntity(
    serverId = null,
    userId = userId,
    workoutName = workoutName,
    workoutDate = workoutDate,
    durationMinutes = durationMinutes,
    completed = completed,
    xpEarned = xpEarned,
    exercisesJson = JsonLists.encode(workoutExercises),
    synced = false
)

// Pending workouts get a negative id (see OfflineRules.pendingWorkoutId)
fun WorkoutEntity.toModel() = Workout(
    workoutId = serverId ?: OfflineRules.pendingWorkoutId(localId),
    userId = userId,
    workoutName = workoutName,
    workoutDate = workoutDate,
    durationMinutes = durationMinutes,
    completed = completed,
    xpEarned = xpEarned,
    workoutExercises = JsonLists.decode<WorkoutExercise>(exercisesJson)
)

// Turns a saved pending workout back into the request the API expects
fun WorkoutEntity.toCreateRequest() = CreateWorkoutRequest(
    userId = userId,
    workoutName = workoutName,
    workoutDate = workoutDate,
    durationMinutes = durationMinutes,
    completed = completed,
    xpEarned = xpEarned,
    workoutExercises = JsonLists.decode<WorkoutExercise>(exercisesJson)
)

// ---------------- User stats (XP and level) ----------------

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: Int,
    val firebaseUid: String,
    val username: String,
    val email: String,
    val profileImageUrl: String?,
    val xp: Int,
    val level: Int,
    val createdAt: String
)

fun User.toEntity() = UserEntity(
    userId = userId,
    firebaseUid = firebaseUid,
    username = username,
    email = email,
    profileImageUrl = profileImageUrl,
    xp = xp,
    level = level,
    createdAt = createdAt
)

fun UserEntity.toModel() = User(
    userId = userId,
    firebaseUid = firebaseUid,
    username = username,
    email = email,
    profileImageUrl = profileImageUrl,
    xp = xp,
    level = level,
    createdAt = createdAt
)

// ---------------- Squads (the member list is saved as JSON text) ----------------

@Entity(tableName = "squads")
data class SquadEntity(
    @PrimaryKey val squadId: Int,
    val squadName: String,
    val sharedStreak: Int,
    val createdAt: String?,
    val membersJson: String
)

fun Squad.toEntity() = SquadEntity(
    squadId = squadId,
    squadName = squadName,
    sharedStreak = sharedStreak,
    createdAt = createdAt,
    membersJson = JsonLists.encode(members)
)

fun SquadEntity.toModel() = Squad(
    squadId = squadId,
    squadName = squadName,
    sharedStreak = sharedStreak,
    createdAt = createdAt,
    members = JsonLists.decode<SquadMember>(membersJson)
)

// ---------------- Battle Arena challenges ----------------

/** The API returns a different list for each user, so the saved copy is kept per user. */
@Entity(
    tableName = "challenges",
    primaryKeys = ["cachedForUserId", "challengeId"]
)
data class ChallengeEntity(
    val cachedForUserId: Int,
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

fun Challenge.toEntity(forUserId: Int) = ChallengeEntity(
    cachedForUserId = forUserId,
    challengeId = challengeId,
    title = title,
    description = description,
    type = type,
    target = target,
    xpReward = xpReward,
    currentProgress = currentProgress,
    opponentProgress = opponentProgress,
    completed = completed,
    endDate = endDate,
    createdByUserId = createdByUserId,
    createdByUsername = createdByUsername,
    opponentUserId = opponentUserId,
    opponentUsername = opponentUsername,
    status = status,
    winnerUserId = winnerUserId,
    createdAt = createdAt
)

fun ChallengeEntity.toModel() = Challenge(
    challengeId = challengeId,
    title = title,
    description = description,
    type = type,
    target = target,
    xpReward = xpReward,
    currentProgress = currentProgress,
    opponentProgress = opponentProgress,
    completed = completed,
    endDate = endDate,
    createdByUserId = createdByUserId,
    createdByUsername = createdByUsername,
    opponentUserId = opponentUserId,
    opponentUsername = opponentUsername,
    status = status,
    winnerUserId = winnerUserId,
    createdAt = createdAt
)

// ---------------- Personal records (1RM) ----------------

@Entity(
    tableName = "personal_records",
    primaryKeys = ["userId", "exerciseId"]
)
data class PersonalRecordEntity(
    val userId: Int,
    val exerciseId: Int,
    val exerciseName: String,
    val heaviestWeight: Double,
    val repsAtHeaviestWeight: Int,
    val estimatedOneRepMax: Double,
    val dateAchieved: String
)

fun PersonalRecord.toEntity(forUserId: Int) = PersonalRecordEntity(
    userId = forUserId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    heaviestWeight = heaviestWeight,
    repsAtHeaviestWeight = repsAtHeaviestWeight,
    estimatedOneRepMax = estimatedOneRepMax,
    dateAchieved = dateAchieved
)

fun PersonalRecordEntity.toModel() = PersonalRecord(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    heaviestWeight = heaviestWeight,
    repsAtHeaviestWeight = repsAtHeaviestWeight,
    estimatedOneRepMax = estimatedOneRepMax,
    dateAchieved = dateAchieved
)

// ---------------- Muscle group volume ----------------

@Entity(
    tableName = "muscle_volume",
    primaryKeys = ["userId", "muscleGroup"]
)
data class MuscleVolumeEntity(
    val userId: Int,
    val muscleGroup: String,
    val volume: Double
)

// ---------------- When was each saved list last refreshed? ----------------

/**
 * One row per saved list. If there is no row, nothing has been saved for that list yet,
 * which is different from "saved, and the list was empty".
 */
@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val cacheKey: String,
    val updatedAt: Long
)