package com.serenity.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.app.ui.history.components.DayDetailSheet
import com.serenity.app.ui.history.components.SummaryStatsRow
import com.serenity.app.ui.history.components.WellnessBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Your History") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Week / Month toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.viewMode == HistoryViewMode.WEEK,
                    onClick = { viewModel.setViewMode(HistoryViewMode.WEEK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Week") }
                SegmentedButton(
                    selected = uiState.viewMode == HistoryViewMode.MONTH,
                    onClick = { viewModel.setViewMode(HistoryViewMode.MONTH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Month") }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isLoading) {
                // Loading placeholder — do nothing, chart area is blank
            } else {
                val hasData = if (uiState.viewMode == HistoryViewMode.WEEK) {
                    uiState.weekBars.any { it.second != null }
                } else {
                    uiState.monthBars.any { it.hasData }
                }

                if (!hasData) {
                    // Empty state
                    Text(
                        text = "No check-ins yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    )
                } else {
                    // Chart
                    if (uiState.viewMode == HistoryViewMode.WEEK) {
                        WellnessBarChart(
                            weekBars = uiState.weekBars,
                            selectedDate = uiState.selectedDayRitual?.date,
                            onBarTap = viewModel::onBarTapped,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        WellnessBarChart(
                            monthBars = uiState.monthBars,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary stats — extracted per view mode to handle different model shapes
                    if (uiState.viewMode == HistoryViewMode.WEEK) {
                        uiState.weeklyStats?.let { stats ->
                            SummaryStatsRow(
                                averageScore = stats.averageScore,
                                bestDay = stats.bestDay,
                                topHabit = stats.topHabit,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        uiState.monthlyStats?.let { stats ->
                            val averageScore = uiState.monthBars
                                .mapNotNull { it.averageScore }
                                .let { scores -> if (scores.isEmpty()) 0 else scores.average().toInt() }
                            SummaryStatsRow(
                                averageScore = averageScore,
                                bestDay = stats.bestDay,
                                topHabit = stats.topHabit,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Day detail sheet
    uiState.selectedDayRitual?.let { ritual ->
        DayDetailSheet(
            ritual = ritual,
            onDismiss = viewModel::dismissDayDetail
        )
    }
}
