package com.serenity.app.domain.usecase

import com.serenity.app.domain.repository.RitualRepository
import javax.inject.Inject

class GetStreakUseCase @Inject constructor(
    private val ritualRepository: RitualRepository
) {

    suspend operator fun invoke(): Int {
        return ritualRepository.getCurrentStreak()
    }
}
