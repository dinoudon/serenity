package com.serenity.app.data.repository

import com.serenity.app.data.local.RitualDao
import com.serenity.app.data.local.RitualEntity
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RitualRepositoryImpl @Inject constructor(
    private val ritualDao: RitualDao
) : RitualRepository {

    override suspend fun saveRitual(ritual: DailyRitual) {
        ritualDao.upsert(RitualEntity.fromDomain(ritual))
    }

    override fun getRitualByDate(date: LocalDate): Flow<DailyRitual?> {
        return ritualDao.getByDate(date.toString()).map { it?.toDomain() }
    }

    override fun getRitualsInRange(start: LocalDate, end: LocalDate): Flow<List<DailyRitual>> {
        return ritualDao.getInRange(start.toString(), end.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLatestRitual(): Flow<DailyRitual?> {
        return ritualDao.getLatest().map { it?.toDomain() }
    }

    override suspend fun getCurrentStreak(): Int {
        val dates = ritualDao.getAllDatesDescending()
            .map { LocalDate.parse(it) }

        if (dates.isEmpty()) return 0

        var streak = 0
        var expectedDate = LocalDate.now()

        for (date in dates) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (date.isBefore(expectedDate)) {
                // If the first date isn't today, check if it's yesterday (streak still valid)
                if (streak == 0 && date == LocalDate.now().minusDays(1)) {
                    streak = 1
                    expectedDate = date.minusDays(1)
                } else {
                    break
                }
            }
        }

        return streak
    }

    override suspend fun countPerfectScores(): Int = ritualDao.countPerfectScores()
    override suspend fun countNonNullSleep(): Int = ritualDao.countNonNullSleep()
    override suspend fun countNonNullWater(): Int = ritualDao.countNonNullWater()
    override suspend fun countNonNullGratitude(): Int = ritualDao.countNonNullGratitude()
    override suspend fun countBreathingCompleted(): Int = ritualDao.countBreathingCompleted()
}
