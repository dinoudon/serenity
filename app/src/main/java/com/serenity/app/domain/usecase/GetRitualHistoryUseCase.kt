package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetRitualHistoryUseCase @Inject constructor(
    private val ritualRepository: RitualRepository
) {

    operator fun invoke(start: LocalDate, end: LocalDate): Flow<List<DailyRitual>> {
        return ritualRepository.getRitualsInRange(start, end)
    }
}
