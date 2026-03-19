package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.WellnessQuote
import javax.inject.Inject

class GetRandomQuoteUseCase @Inject constructor() {

    operator fun invoke(): WellnessQuote {
        return quotes.random()
    }

    companion object {
        private val quotes = listOf(
            WellnessQuote(
                "Almost everything will work again if you unplug it for a few minutes, including you.",
                "Anne Lamott"
            ),
            WellnessQuote(
                "The greatest wealth is health.",
                "Virgil"
            ),
            WellnessQuote(
                "Take care of your body. It's the only place you have to live.",
                "Jim Rohn"
            ),
            WellnessQuote(
                "Happiness is the highest form of health.",
                "Dalai Lama"
            ),
            WellnessQuote(
                "Self-care is not selfish. You cannot serve from an empty vessel.",
                "Eleanor Brownn"
            ),
            WellnessQuote(
                "Rest when you're weary. Refresh and renew yourself, your body, your mind, your spirit.",
                "Ralph Marston"
            ),
            WellnessQuote(
                "Wellness is the complete integration of body, mind, and spirit.",
                "Greg Anderson"
            ),
            WellnessQuote(
                "The mind and body are not separate. What affects one, affects the other.",
                "Anonymous"
            ),
            WellnessQuote(
                "Breathing is the greatest pleasure in life.",
                "Giovanni Papini"
            ),
            WellnessQuote(
                "Sleep is the best meditation.",
                "Dalai Lama"
            ),
            WellnessQuote(
                "Water is the driving force of all nature.",
                "Leonardo da Vinci"
            ),
            WellnessQuote(
                "Gratitude turns what we have into enough.",
                "Melody Beattie"
            ),
            WellnessQuote(
                "A calm mind brings inner strength and self-confidence.",
                "Dalai Lama"
            ),
            WellnessQuote(
                "Every day is a chance to begin again.",
                "Anonymous"
            ),
            WellnessQuote(
                "Be gentle with yourself, you're doing the best you can.",
                "Anonymous"
            )
        )
    }
}
