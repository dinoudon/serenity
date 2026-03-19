package com.serenity.app.domain.usecase

import javax.inject.Inject

class CalculateWellnessScoreUseCase @Inject constructor() {

    operator fun invoke(
        mood: Int?,
        sleepHours: Float?,
        waterGlasses: Int?,
        breathingCompleted: Boolean?,
        gratitudeNote: String?
    ): Int {
        val components = mutableListOf<Pair<Float, Float>>()

        if (mood != null) {
            val moodScore = (mood.coerceIn(1, 5) * 25 - 25).toFloat()
            components.add(moodScore to MOOD_WEIGHT)
        }

        if (sleepHours != null) {
            val sleepScore = calculateSleepScore(sleepHours.coerceIn(0f, 12f))
            components.add(sleepScore to SLEEP_WEIGHT)
        }

        if (waterGlasses != null) {
            val waterScore = (waterGlasses.coerceIn(0, 8).toFloat() / 8f * 100f)
            components.add(waterScore to WATER_WEIGHT)
        }

        if (breathingCompleted != null) {
            val breathingScore = if (breathingCompleted) 100f else 0f
            components.add(breathingScore to BREATHING_WEIGHT)
        }

        if (gratitudeNote != null) {
            val gratitudeScore = if (gratitudeNote.isNotBlank()) 100f else 0f
            components.add(gratitudeScore to GRATITUDE_WEIGHT)
        }

        if (components.isEmpty()) return 0

        val totalWeight = components.sumOf { it.second.toDouble() }.toFloat()
        val weightedSum = components.sumOf { (score, weight) ->
            (score * weight / totalWeight).toDouble()
        }.toFloat()

        return weightedSum.toInt().coerceIn(0, 100)
    }

    private fun calculateSleepScore(hours: Float): Float {
        return when {
            hours < 5f -> 0f
            hours < 7f -> (hours - 5f) / 2f * 100f
            hours <= 9f -> 100f
            else -> ((12f - hours) / 3f * 100f).coerceAtLeast(0f)
        }
    }

    companion object {
        private const val MOOD_WEIGHT = 0.30f
        private const val SLEEP_WEIGHT = 0.30f
        private const val WATER_WEIGHT = 0.20f
        private const val BREATHING_WEIGHT = 0.10f
        private const val GRATITUDE_WEIGHT = 0.10f
    }
}
