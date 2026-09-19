package com.gymleague.app.network

import com.gymleague.app.models.*
import retrofit2.Response
import retrofit2.http.*

interface GymLeagueApiService {

    // --- Users ---
    @GET("api/users")
    suspend fun getUsers(): Response<List<User>>

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>

    @POST("api/users")
    suspend fun createUser(@Body user: CreateUserRequest): Response<User>

    // --- Exercises ---
    @GET("api/exercises")
    suspend fun getExercises(): Response<List<Exercise>>

    @GET("api/exercises/{id}")
    suspend fun getExercise(@Path("id") id: Int): Response<Exercise>

    @GET("api/exercises/musclegroup/{muscleGroup}")
    suspend fun getExercisesByMuscleGroup(@Path("muscleGroup") muscleGroup: String): Response<List<Exercise>>

    @POST("api/exercises")
    suspend fun createExercise(@Body exercise: Exercise): Response<Exercise>

    // --- Workouts ---
    @GET("api/workouts/user/{userId}")
    suspend fun getWorkoutsForUser(@Path("userId") userId: Int): Response<List<Workout>>

    @GET("api/workouts/{id}")
    suspend fun getWorkout(@Path("id") id: Int): Response<Workout>

    @POST("api/workouts")
    suspend fun createWorkout(@Body workout: CreateWorkoutRequest): Response<Workout>

    // --- Personal Records ---
    @GET("api/personalrecords/user/{userId}")
    suspend fun getPersonalRecords(@Path("userId") userId: Int): Response<List<PersonalRecord>>

    // --- User Settings ---
    @GET("api/usersettings/user/{userId}")
    suspend fun getSettingsForUser(@Path("userId") userId: Int): Response<UserSettings>

    @POST("api/usersettings")
    suspend fun createSettings(@Body settings: UserSettings): Response<UserSettings>
}