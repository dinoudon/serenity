package com.serenity.app.domain.usecase

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import com.serenity.app.widget.SerenityWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class CompleteRitualUseCase @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val calculateWellnessScoreUseCase: CalculateWellnessScoreUseCase,
    private val checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase,
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

        // Fire-and-forget: achievement failure must not block ritual save
        runCatching {
            checkAndUnlockAchievementsUseCase(wellnessScore = score)
        }

        // Trigger widget refresh
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SerenityWidgetReceiver::class.java)
            )
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        component = ComponentName(context, SerenityWidgetReceiver::class.java)
                    }
                )
            }
        }
    }
}
