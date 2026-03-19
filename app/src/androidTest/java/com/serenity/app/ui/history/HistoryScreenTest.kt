package com.serenity.app.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import com.serenity.app.domain.model.WeeklyStats
import com.serenity.app.ui.theme.SerenityTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 3, 20)

    private fun ritual(daysAgo: Long = 0, score: Int = 70) = DailyRitual(
        date = today.minusDays(daysAgo),
        mood = 4,
        sleepHours = 7.5f,
        waterGlasses = 6,
        breathingCompleted = true,
        gratitudeNote = "Grateful for tests",
        wellnessScore = score,
        createdAt = Instant.EPOCH
    )

    @Test
    fun emptyState_showsNoCheckInsMessage() {
        val emptyState = HistoryUiState(
            isLoading = false,
            weekBars = (0..6).map { today.minusDays(it.toLong()) to null },
            monthBars = emptyList(),
            weeklyStats = null,
            monthlyStats = null
        )
        composeRule.setContent {
            SerenityTheme {
                HistoryScreenContent(
                    uiState = emptyState,
                    onNavigateBack = {},
                    onViewModeChange = {},
                    onBarTapped = {},
                    onDismissDetail = {}
                )
            }
        }
        composeRule.onNodeWithText("No check-ins yet").assertIsDisplayed()
    }

    @Test
    fun weekViewWithData_showsChart() {
        val weekBars = (0..6).map { i ->
            today.minusDays(i.toLong()) to if (i < 3) 70 + i * 5 else null
        }
        val state = HistoryUiState(
            isLoading = false,
            viewMode = HistoryViewMode.WEEK,
            weekBars = weekBars,
            weeklyStats = WeeklyStats(
                averageScore = 75,
                bestDay = today to 80,
                topHabit = HabitType.MOOD
            )
        )
        composeRule.setContent {
            SerenityTheme {
                HistoryScreenContent(
                    uiState = state,
                    onNavigateBack = {},
                    onViewModeChange = {},
                    onBarTapped = {},
                    onDismissDetail = {}
                )
            }
        }
        // Stats row visible
        composeRule.onNodeWithText("75 avg").assertIsDisplayed()
    }

    @Test
    fun toggleToMonth_showsMonthMode() {
        var currentMode = HistoryViewMode.WEEK
        val state = HistoryUiState(
            isLoading = false,
            viewMode = HistoryViewMode.WEEK,
            weekBars = (0..6).map { today.minusDays(it.toLong()) to 70 },
            weeklyStats = WeeklyStats(70, today to 70, HabitType.SLEEP)
        )
        composeRule.setContent {
            SerenityTheme {
                HistoryScreenContent(
                    uiState = state,
                    onNavigateBack = {},
                    onViewModeChange = { currentMode = it },
                    onBarTapped = {},
                    onDismissDetail = {}
                )
            }
        }
        composeRule.onNodeWithText("Month").performClick()
        assert(currentMode == HistoryViewMode.MONTH)
    }

    @Test
    fun dayDetailSheet_visibleWhenRitualSelected() {
        val state = HistoryUiState(
            isLoading = false,
            weekBars = (0..6).map { today.minusDays(it.toLong()) to 70 },
            weeklyStats = WeeklyStats(70, today to 70, HabitType.WATER),
            selectedDayRitual = ritual(0, 70)
        )
        composeRule.setContent {
            SerenityTheme {
                HistoryScreenContent(
                    uiState = state,
                    onNavigateBack = {},
                    onViewModeChange = {},
                    onBarTapped = {},
                    onDismissDetail = {}
                )
            }
        }
        // Bottom sheet should show wellness score
        composeRule.onNodeWithText("Wellness Score: 70").assertIsDisplayed()
    }
}
