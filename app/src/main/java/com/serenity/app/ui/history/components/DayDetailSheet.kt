package com.serenity.app.ui.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serenity.app.domain.model.DailyRitual
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    ritual: DailyRitual,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
            Text(
                text = ritual.date.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Wellness Score: ${ritual.wellnessScore}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Mood
            ritual.mood?.let { mood ->
                val moodLabel = when (mood) {
                    1 -> "😢 Awful"
                    2 -> "😕 Bad"
                    3 -> "😐 Okay"
                    4 -> "😊 Good"
                    5 -> "😄 Great"
                    else -> "—"
                }
                DetailRow(label = "Mood", value = moodLabel)
            }

            // Sleep
            ritual.sleepHours?.let { sleep ->
                DetailRow(label = "Sleep", value = "${sleep}h")
            }

            // Water
            ritual.waterGlasses?.let { water ->
                DetailRow(label = "Water", value = "$water of 8 glasses")
            }

            // Breathing
            ritual.breathingCompleted?.let { breathing ->
                DetailRow(label = "Breathing", value = if (breathing) "✓ Completed" else "✗ Skipped")
            }

            // Gratitude (only if non-blank, snippet max 80 chars)
            val note = ritual.gratitudeNote
            if (!note.isNullOrBlank()) {
                val snippet = if (note.length > 80) note.take(80) + "…" else note
                DetailRow(label = "Gratitude", value = "\"$snippet\"")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
