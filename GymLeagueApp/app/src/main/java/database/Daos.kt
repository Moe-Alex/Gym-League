package com.gymleague.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/*
 * Data access objects (DAOs). Every function is a suspend function or returns a Flow,
 * so none of them can run on the main thread and freeze the screen.
 */

@Dao
abstract class ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY exerciseId")
    abstract suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup COLLATE NOCASE ORDER BY exerciseId")
    abstract suspend fun getByMuscleGroup(muscleGroup: String): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    abstract suspend fun deleteAll()

    // Swap the old saved list for the fresh one from the server in one step
    @Transaction
    open suspend fun replaceAll(items: List<ExerciseEntity>) {
        deleteAll()
        insertAll(items)
    }
}

@Dao
abstract class WorkoutDao {

    // Everything the server sent us for this user
    @Query("SELECT * FROM workouts WHERE userId = :userId AND synced = 1 ORDER BY workoutDate DESC")
    abstract suspend fun getSyncedForUser(userId: Int): List<WorkoutEntity>

    // Workouts logged offline that the server does not have yet
    @Query("SELECT * FROM workouts WHERE userId = :userId AND synced = 0 ORDER BY localId")
    abstract suspend fun getUnsyncedForUser(userId: Int): List<WorkoutEntity>

    // The outbox: oldest first, skipping ones the server has refused too many times
    @Query("SELECT * FROM workouts WHERE synced = 0 AND syncAttempts < :maxAttempts ORDER BY localId")
    abstract suspend fun getPending(maxAttempts: Int): List<WorkoutEntity>

    // Live count, used by the offline banner ("2 workouts waiting to sync")
    @Query("SELECT COUNT(*) FROM workouts WHERE synced = 0 AND syncAttempts < :maxAttempts")
    abstract fun observePendingCount(maxAttempts: Int): Flow<Int>

    @Insert
    abstract suspend fun insert(workout: WorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<WorkoutEntity>)

    @Query("UPDATE workouts SET synced = 1, serverId = :serverId, syncAttempts = 0 WHERE localId = :localId")
    abstract suspend fun markSynced(localId: Long, serverId: Int)

    @Query("UPDATE workouts SET syncAttempts = syncAttempts + 1 WHERE localId = :localId")
    abstract suspend fun incrementAttempts(localId: Long)

    @Query("DELETE FROM workouts WHERE synced = 1 AND userId = :userId")
    abstract suspend fun deleteSyncedForUser(userId: Int)

    @Query("DELETE FROM workouts WHERE synced = 1")
    abstract suspend fun deleteAllSynced()

    // Replace the saved server copy for this user. Workouts still waiting to sync are not touched.
    @Transaction
    open suspend fun replaceSyncedForUser(userId: Int, items: List<WorkoutEntity>) {
        deleteSyncedForUser(userId)
        insertAll(items)
    }
}

@Dao
abstract class UserDao {

    @Query("SELECT * FROM users WHERE userId = :userId")
    abstract suspend fun getById(userId: Int): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(user: UserEntity)

    @Query("DELETE FROM users")
    abstract suspend fun deleteAll()
}

@Dao
abstract class SquadDao {

    @Query("SELECT * FROM squads ORDER BY squadId")
    abstract suspend fun getAll(): List<SquadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<SquadEntity>)

    @Query("DELETE FROM squads")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(items: List<SquadEntity>) {
        deleteAll()
        insertAll(items)
    }
}

@Dao
abstract class ChallengeDao {

    @Query("SELECT * FROM challenges WHERE cachedForUserId = :userId ORDER BY endDate")
    abstract suspend fun getForUser(userId: Int): List<ChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<ChallengeEntity>)

    @Query("DELETE FROM challenges WHERE cachedForUserId = :userId")
    abstract suspend fun deleteForUser(userId: Int)

    @Query("DELETE FROM challenges")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceForUser(userId: Int, items: List<ChallengeEntity>) {
        deleteForUser(userId)
        insertAll(items)
    }
}

@Dao
abstract class PersonalRecordDao {

    @Query("SELECT * FROM personal_records WHERE userId = :userId ORDER BY estimatedOneRepMax DESC")
    abstract suspend fun getForUser(userId: Int): List<PersonalRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<PersonalRecordEntity>)

    @Query("DELETE FROM personal_records WHERE userId = :userId")
    abstract suspend fun deleteForUser(userId: Int)

    @Query("DELETE FROM personal_records")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceForUser(userId: Int, items: List<PersonalRecordEntity>) {
        deleteForUser(userId)
        insertAll(items)
    }
}

@Dao
abstract class MuscleVolumeDao {

    @Query("SELECT * FROM muscle_volume WHERE userId = :userId")
    abstract suspend fun getForUser(userId: Int): List<MuscleVolumeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<MuscleVolumeEntity>)

    @Query("DELETE FROM muscle_volume WHERE userId = :userId")
    abstract suspend fun deleteForUser(userId: Int)

    @Query("DELETE FROM muscle_volume")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceForUser(userId: Int, items: List<MuscleVolumeEntity>) {
        deleteForUser(userId)
        insertAll(items)
    }
}

@Dao
abstract class CacheMetaDao {

    @Query("SELECT * FROM cache_meta WHERE cacheKey = :key")
    abstract suspend fun get(key: String): CacheMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun put(meta: CacheMetaEntity)

    @Query("DELETE FROM cache_meta")
    abstract suspend fun deleteAll()
}