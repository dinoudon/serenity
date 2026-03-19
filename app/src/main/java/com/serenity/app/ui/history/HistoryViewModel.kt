package com.serenity.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.MonthlyStats
import com.serenity.app.domain.model.WeeklyStats
import com.serenity.app.domain.repository.RitualRepository
import com.serenity.app.domain.usecase.GetMonthlyStatsUseCase
import com.serenity.app.domain.usecase.GetWeeklyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class HistoryViewMode { WEEK, MONTH }

data class HistoryUiState(
    val viewMode: HistoryViewMode = HistoryViewMode.WEEK,
    val weekBars: List<Pair<LocalDate, Int?>> = emptyList(),   // 7 items, null = no ritual that day
    val monthBars: List<com.serenity.app.domain.model.WeekAverage> = emptyList(), // 4 items
    val weeklyStats: WeeklyStats? = null,
    val monthlyStats: MonthlyStats? = null,
    val selectedDayRitual: DailyRitual? = null,   // non-null = show DayDetailSheet
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val getWeeklyStatsUseCase: GetWeeklyStatsUseCase,
    private val getMonthlyStatsUseCase: GetMonthlyStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val weekStart = today.minusDays(6)
            val monthStart = today.minusDays(27)

            ritualRepository.getRitualsInRange(monthStart, today).collect { rituals ->
                val ritualsByDate = rituals.associateBy { it.date }

                // Week bars: rolling 7 days
                val weekBars = (6 downTo 0).map { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    date to ritualsByDate[date]?.wellnessScore
                }

                // Month bars via use case
                val weekRituals = rituals.filter { !it.date.isBefore(weekStart) }
                val weeklyStats = getWeeklyStatsUseCase(weekRituals)
                val monthlyStats = getMonthlyStatsUseCase(rituals, today)

                _uiState.update {
                    it.copy(
                        weekBars = weekBars,
                        monthBars = monthlyStats?.weeklyAverages ?: emptyList(),
                        weeklyStats = weeklyStats,
                        monthlyStats = monthlyStats,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setViewMode(mode: HistoryViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onBarTapped(date: LocalDate) {
        viewModelScope.launch {
            ritualRepository.getRitualByDate(date).collect { ritual ->
                _uiState.update { it.copy(selectedDayRitual = ritual) }
            }
        }
    }

    fun dismissDayDetail() {
        _uiState.update { it.copy(selectedDayRitual = null) }
    }
}
