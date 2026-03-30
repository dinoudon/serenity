package com.serenity.app.domain.repository

import com.serenity.app.domain.model.DailyRitual
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RitualRepository {
    suspend fun saveRitual(ritual: DailyRitual)
    fun getRitualByDate(date: LocalDate): Flow<DailyRitual?>
    fun getRitualsInRange(start: LocalDate, end: LocalDate): Flow<List<DailyRitual>>
    fun getLatestRitual(): Flow<DailyRitual?>
    suspend fun getCurrentStreak(): Int
    suspend fun countPerfectScores(): Int
    suspend fun countNonNullSleep(): Int
    suspend fun countNonNullWater(): Int
    suspend fun countNonNullGratitude(): Int
    suspend fun countBreathingCompleted(): Int
}
