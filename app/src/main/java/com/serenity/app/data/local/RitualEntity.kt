package com.serenity.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.serenity.app.domain.model.DailyRitual
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "daily_ritual")
data class RitualEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val mood: Int?,
    val sleepHours: Float?,
    val waterGlasses: Int?,
    val breathingCompleted: Boolean?,
    val gratitudeNote: String?,
    val wellnessScore: Int,
    val createdAt: Long
) {
    fun toDomain(): DailyRitual = DailyRitual(
        id = id,
        date = LocalDate.parse(date),
        mood = mood,
        sleepHours = sleepHours,
        waterGlasses = waterGlasses,
        breathingCompleted = breathingCompleted,
        gratitudeNote = gratitudeNote,
        wellnessScore = wellnessScore,
        createdAt = Instant.ofEpochMilli(createdAt)
    )

    companion object {
        fun fromDomain(ritual: DailyRitual): RitualEntity = RitualEntity(
            id = ritual.id,
            date = ritual.date.toString(),
            mood = ritual.mood,
            sleepHours = ritual.sleepHours,
            waterGlasses = ritual.waterGlasses,
            breathingCompleted = ritual.breathingCompleted,
            gratitudeNote = ritual.gratitudeNote,
            wellnessScore = ritual.wellnessScore,
            createdAt = ritual.createdAt.toEpochMilli()
        )
    }
}
