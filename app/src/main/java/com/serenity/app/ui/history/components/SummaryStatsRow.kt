package com.serenity.app.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serenity.app.domain.model.HabitType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SummaryStatsRow(
    averageScore: Int,
    bestDay: Pair<LocalDate, Int>,
    topHabit: HabitType,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            label = "AVG SCORE",
            value = "$averageScore avg",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "BEST DAY",
            value = "${bestDay.first.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).take(3).replaceFirstChar { it.uppercase() }} • ${bestDay.second}",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "TOP HABIT",
            value = "${topHabit.emoji} ${topHabit.displayLabel}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
