package com.serenity.app.ui.history.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serenity.app.domain.model.WeekAverage
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bar chart for both week and month views.
 *
 * Week mode: [weekBars] = list of 7 (LocalDate, Int?) pairs; null score = placeholder.
 * Month mode: [monthBars] = list of 4 WeekAverage items.
 * Exactly one must be non-null.
 *
 * [selectedDate] highlights the selected bar in week view (null = none selected).
 * [onBarTap] called only for week bars with data.
 */
@Composable
fun WellnessBarChart(
    modifier: Modifier = Modifier,
    weekBars: List<Pair<LocalDate, Int?>>? = null,
    monthBars: List<WeekAverage>? = null,
    selectedDate: LocalDate? = null,
    onBarTap: (LocalDate) -> Unit = {}
) {
    require((weekBars == null) != (monthBars == null)) {
        "Exactly one of weekBars or monthBars must be non-null"
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val lightGreen = primary.copy(alpha = 0.25f)

    val barCount = weekBars?.size ?: monthBars!!.size
    val labels: List<String>
    val scores: List<Int?>
    val dates: List<LocalDate?>
    val isWeekMode = weekBars != null
    val today = LocalDate.now()

    if (isWeekMode) {
        labels = weekBars!!.map { (date, _) ->
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
        scores = weekBars.map { it.second }
        dates = weekBars.map { it.first }
    } else {
        val formatter = DateTimeFormatter.ofPattern("MMM d")
        labels = monthBars!!.map { it.weekStart.format(formatter) }
        scores = monthBars.map { if (it.hasData) it.averageScore else null }
        dates = monthBars.map { null } // month bars not tappable
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Chart canvas + tap overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val barWidth = (size.width - (barCount - 1) * 8.dp.toPx()) / barCount
                val maxBarHeight = size.height * 0.85f
                val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                scores.forEachIndexed { index, score ->
                    val left = index * (barWidth + 8.dp.toPx())
                    val isSelected = isWeekMode && dates[index] == selectedDate
                    val barColor = when {
                        isSelected -> tertiary
                        score == null -> surfaceVariant.copy(alpha = 0.4f)
                        else -> lerp(lightGreen, primary, score / 100f)
                    }
                    val barHeight = when {
                        score == null -> maxBarHeight * 0.05f
                        else -> maxBarHeight * (score / 100f).coerceAtLeast(0.05f)
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }

            // Tap targets (week mode only, data bars only)
            if (isWeekMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weekBars!!.forEachIndexed { index, (date, score) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                                .then(
                                    if (score != null) {
                                        Modifier.pointerInput(date) {
                                            detectTapGestures { onBarTap(date) }
                                        }
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEachIndexed { index, label ->
                val isToday = isWeekMode && dates[index] == today
                Text(
                    text = label,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
