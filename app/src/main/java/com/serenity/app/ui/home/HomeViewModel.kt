package com.serenity.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.WellnessQuote
import com.serenity.app.domain.repository.PreferencesRepository
import com.serenity.app.domain.usecase.GetRandomQuoteUseCase
import com.serenity.app.domain.usecase.GetStreakUseCase
import com.serenity.app.domain.usecase.GetTodayRitualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val todayRitual: DailyRitual? = null,
    val streak: Int = 0,
    val quote: WellnessQuote = WellnessQuote("", ""),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayRitualUseCase: GetTodayRitualUseCase,
    private val getStreakUseCase: GetStreakUseCase,
    private val getRandomQuoteUseCase: GetRandomQuoteUseCase,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(quote = getRandomQuoteUseCase()) }
        }

        viewModelScope.launch {
            val streak = getStreakUseCase()
            _uiState.update { it.copy(streak = streak) }
        }

        viewModelScope.launch {
            preferencesRepository.getUserPreferences().collect { prefs ->
                _uiState.update { it.copy(userName = prefs.name) }
            }
        }

        viewModelScope.launch {
            getTodayRitualUseCase().collect { ritual ->
                _uiState.update {
                    it.copy(
                        todayRitual = ritual,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
