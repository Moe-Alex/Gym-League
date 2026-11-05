package com.gymleague.app.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database that lives on the phone. It keeps a copy of the data the app has seen
 * (so the app works without internet) and the workouts logged offline that are waiting to sync.
 *
 * Whenever a table changes, raise [version]. While developing, uninstalling the app is the
 * quickest way to reset the database.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        UserEntity::class,
        SquadEntity::class,
        ChallengeEntity::class,
        PersonalRecordEntity::class,
        MuscleVolumeEntity::class,
        CacheMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GymLeagueDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userDao(): UserDao
    abstract fun squadDao(): SquadDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun muscleVolumeDao(): MuscleVolumeDao
    abstract fun cacheMetaDao(): CacheMetaDao

    /**
     * Called when the user logs out, so the next person to sign in cannot see this user's data.
     * Workouts that are still waiting to sync are kept, so nothing the user logged is lost.
     * They are sent under the user id they were saved with, and other users never see them.
     */
    suspend fun clearUserCache() {
        exerciseDao().deleteAll()
        userDao().deleteAll()
        squadDao().deleteAll()
        challengeDao().deleteAll()
        personalRecordDao().deleteAll()
        muscleVolumeDao().deleteAll()
        workoutDao().deleteAllSynced()
        cacheMetaDao().deleteAll()

        Log.d("GymLeagueOffline", "Cleared the saved copy of the user's data")
    }

    companion object {

        private const val DATABASE_NAME = "gymleague.db"

        @Volatile
        private var instance: GymLeagueDatabase? = null

        // One database for the whole app
        fun getInstance(context: Context): GymLeagueDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymLeagueDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}