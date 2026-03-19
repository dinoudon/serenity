package com.serenity.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.usecase.GetRitualHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val weeklyRituals: List<DailyRitual> = emptyList(),
    val selectedRitual: DailyRitual? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getRitualHistoryUseCase: GetRitualHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadWeeklyData()
    }

    private fun loadWeeklyData() {
        viewModelScope.launch {
            val end = LocalDate.now()
            val start = end.minusDays(6)
            getRitualHistoryUseCase(start, end).collect { rituals ->
                _uiState.update {
                    it.copy(
                        weeklyRituals = rituals,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun selectDay(date: LocalDate) {
        val ritual = _uiState.value.weeklyRituals.find { it.date == date }
        _uiState.update { it.copy(selectedRitual = ritual) }
    }
}
