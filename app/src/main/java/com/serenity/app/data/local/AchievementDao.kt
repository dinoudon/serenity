package com.serenity.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(entity: AchievementUnlockEntity)

    @Query("SELECT * FROM achievement_unlock")
    suspend fun getAllUnlocks(): List<AchievementUnlockEntity>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgress(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProgress(entity: UserProgressEntity)

    @Query("UPDATE user_progress SET totalXp = totalXp + :amount WHERE id = 1")
    suspend fun incrementXp(amount: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressIfAbsent(entity: UserProgressEntity)
}
