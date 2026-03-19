package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetTodayRitualUseCase @Inject constructor(
    private val ritualRepository: RitualRepository
) {

    operator fun invoke(): Flow<DailyRitual?> {
        return ritualRepository.getRitualByDate(LocalDate.now())
    }
}
