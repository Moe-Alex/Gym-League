package com.gymleague.app.network

import com.gymleague.app.models.*
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Query

interface GymLeagueApiService {

    // --- Users ---

    @GET("api/users")
    suspend fun getUsers(): Response<List<User>>

    @GET("api/users/{id}")
    suspend fun getUser(
        @Path("id") id: Int
    ): Response<User>

    @GET("api/users/firebase/{firebaseUid}")
    suspend fun getUserByFirebaseUid(
        @Path("firebaseUid") firebaseUid: String
    ): Response<User>

    @GET("api/users/username/{username}")
    suspend fun getUserByUsername(
        @Path("username") username: String
    ): Response<User>

    @POST("api/users")
    suspend fun createUser(
        @Body user: CreateUserRequest
    ): Response<User>


    // --- Exercises ---

    @GET("api/exercises")
    suspend fun getExercises(): Response<List<Exercise>>

    @GET("api/exercises/{id}")
    suspend fun getExercise(
        @Path("id") id: Int
    ): Response<Exercise>

    @GET("api/exercises/musclegroup/{muscleGroup}")
    suspend fun getExercisesByMuscleGroup(
        @Path("muscleGroup") muscleGroup: String
    ): Response<List<Exercise>>

    @POST("api/exercises")
    suspend fun createExercise(
        @Body exercise: Exercise
    ): Response<Exercise>

    @DELETE("api/exercises/{id}")
    suspend fun deleteExercise(
        @Path("id") id: Int,
        @Query("userId") userId: Int
    ): Response<Unit>


    // --- Workouts ---

    @GET("api/workouts/user/{userId}")
    suspend fun getWorkoutsForUser(
        @Path("userId") userId: Int
    ): Response<List<Workout>>

    @GET("api/workouts/{id}")
    suspend fun getWorkout(
        @Path("id") id: Int
    ): Response<Workout>

    @POST("api/workouts")
    suspend fun createWorkout(
        @Body workout: CreateWorkoutRequest
    ): Response<Workout>


    // --- Personal Records ---

    @GET("api/personalrecords/user/{userId}")
    suspend fun getPersonalRecords(
        @Path("userId") userId: Int
    ): Response<List<PersonalRecord>>


    // --- User Settings ---

    @GET("api/usersettings/user/{userId}")
    suspend fun getSettingsForUser(
        @Path("userId") userId: Int
    ): Response<UserSettings>

    @POST("api/usersettings")
    suspend fun createSettings(
        @Body settings: UserSettings
    ): Response<UserSettings>

    @PUT("api/usersettings/{id}")
    suspend fun updateSettings(
        @Path("id") id: Int,
        @Body settings: UserSettings
    ): Response<Unit>

    // --- Squads ---
    @GET("api/squads")
    suspend fun getSquads(): Response<List<Squad>>

    @GET("api/squads/{id}")
    suspend fun getSquad(
        @Path("id") id: Int
    ): Response<Squad>

    @POST("api/squads")
    suspend fun createSquad(
        @Body squad: CreateSquadRequest
    ): Response<Squad>

    @POST("api/squads/{squadId}/members")
    suspend fun addSquadMember(
        @Path("squadId") squadId: Int,
        @Body member: AddSquadMemberRequest
    ): Response<SquadMember>

    @DELETE("api/squads/{squadId}/members/{userId}")
    suspend fun leaveSquad(
        @Path("squadId") squadId: Int,
        @Path("userId") userId: Int
    ): Response<Unit>


    // --- Challenges (Battle Arena) ---

    // Returns this user's challenges plus open challenges they could accept
    @GET("api/challenges")
    suspend fun getChallenges(
        @Query("userId") userId: Int
    ): Response<List<Challenge>>

    @POST("api/challenges")
    suspend fun createChallenge(
        @Body challenge: CreateChallengeRequest
    ): Response<Challenge>

    @POST("api/challenges/{id}/accept")
    suspend fun acceptChallenge(
        @Path("id") id: Int,
        @Query("userId") userId: Int
    ): Response<Challenge>

    @POST("api/challenges/{id}/decline")
    suspend fun declineChallenge(
        @Path("id") id: Int,
        @Query("userId") userId: Int
    ): Response<Challenge>

    @POST("api/challenges/{id}/progress")
    suspend fun logChallengeProgress(
        @Path("id") id: Int,
        @Query("userId") userId: Int
    ): Response<Challenge>

    @DELETE("api/challenges/{id}")
    suspend fun deleteChallenge(
        @Path("id") id: Int
    ): Response<Unit>


    // --- Muscle Group Volume ---

    // Total volume (sets x reps x weight) per muscle group for a user
    @GET("api/workouts/musclegroupvolume/{userId}")
    suspend fun getMuscleGroupVolume(
        @Path("userId") userId: Int
    ): Response<Map<String, Double>>


    // --- Push notification device tokens ---

    @POST("api/devicetokens")
    suspend fun registerDeviceToken(
        @Body request: RegisterDeviceTokenRequest
    ): Response<Unit>

    @DELETE("api/devicetokens")
    suspend fun unregisterDeviceToken(
        @Query("token") token: String
    ): Response<Unit>
}
