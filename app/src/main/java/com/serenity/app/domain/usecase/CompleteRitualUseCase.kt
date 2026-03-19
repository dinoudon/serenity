package com.serenity.app.domain.usecase

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class CompleteRitualUseCase @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val calculateWellnessScoreUseCase: CalculateWellnessScoreUseCase,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(
        mood: Int?,
        sleepHours: Float?,
        waterGlasses: Int?,
        breathingCompleted: Boolean?,
        gratitudeNote: String?
    ) {
        val score = calculateWellnessScoreUseCase(
            mood = mood,
            sleepHours = sleepHours,
            waterGlasses = waterGlasses,
            breathingCompleted = breathingCompleted,
            gratitudeNote = gratitudeNote
        )

        val ritual = DailyRitual(
            date = LocalDate.now(),
            mood = mood,
            sleepHours = sleepHours,
            waterGlasses = waterGlasses,
            breathingCompleted = breathingCompleted,
            gratitudeNote = gratitudeNote,
            wellnessScore = score,
            createdAt = Instant.now()
        )

        ritualRepository.saveRitual(ritual)

        runCatching {
            GlanceAppWidgetManager(context).updateAll(context)
        }
    }
}
