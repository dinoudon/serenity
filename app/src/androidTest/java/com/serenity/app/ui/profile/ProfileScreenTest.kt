package com.serenity.app.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.serenity.app.domain.model.Achievement
import com.serenity.app.domain.model.AchievementCategory
import com.serenity.app.domain.model.UserProgress
import com.serenity.app.ui.theme.SerenityTheme
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleProgress() = UserProgress(
        totalXP = 650,
        level = 3,
        levelName = "Blossom",
        levelEmoji = "🌸",
        xpIntoCurrentLevel = 150,
        xpRequiredForNextLevel = 350,
        achievements = listOf(
            Achievement("streak_7", "Week Warrior", "7-day streak", "🔥", AchievementCategory.STREAK, 20, java.time.Instant.now()),
            Achievement("streak_30", "Monthly Mover", "30-day streak", "🏅", AchievementCategory.STREAK, 20, null),
        )
    )

    @Test
    fun levelNameIsDisplayed() {
        composeTestRule.setContent {
            SerenityTheme { ProfileScreenContent(progress = sampleProgress()) }
        }
        composeTestRule.onNodeWithText("Blossom").assertIsDisplayed()
    }

    @Test
    fun levelNumberIsDisplayed() {
        composeTestRule.setContent {
            SerenityTheme { ProfileScreenContent(progress = sampleProgress()) }
        }
        composeTestRule.onNodeWithText("Level 3").assertIsDisplayed()
    }

    @Test
    fun unlockedBadgeTitleIsDisplayed() {
        composeTestRule.setContent {
            SerenityTheme { ProfileScreenContent(progress = sampleProgress()) }
        }
        composeTestRule.onNodeWithText("Week Warrior").assertIsDisplayed()
    }
}
