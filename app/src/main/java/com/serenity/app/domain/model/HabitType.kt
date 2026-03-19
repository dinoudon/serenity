package com.serenity.app.domain.model

enum class HabitType(val displayLabel: String, val emoji: String) {
    MOOD("Mood", "😊"),
    SLEEP("Sleep", "💤"),
    WATER("Water", "💧"),
    BREATHING("Breathing", "🫁"),
    GRATITUDE("Gratitude", "📝")
}
