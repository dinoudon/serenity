package com.serenity.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RitualDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RitualEntity)

    @Query("SELECT * FROM daily_ritual WHERE date = :date LIMIT 1")
    fun getByDate(date: String): Flow<RitualEntity?>

    @Query("SELECT * FROM daily_ritual WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getInRange(startDate: String, endDate: String): Flow<List<RitualEntity>>

    @Query("SELECT * FROM daily_ritual ORDER BY date DESC LIMIT 1")
    fun getLatest(): Flow<RitualEntity?>

    @Query("SELECT date FROM daily_ritual ORDER BY date DESC")
    suspend fun getAllDatesDescending(): List<String>
}
