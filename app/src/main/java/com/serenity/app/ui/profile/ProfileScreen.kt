package com.serenity.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.app.domain.model.Achievement
import com.serenity.app.domain.model.AchievementCategory
import com.serenity.app.domain.model.UserProgress

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message)
        }
        is ProfileUiState.Success -> ProfileScreenContent(progress = state.progress)
    }
}

@Composable
internal fun ProfileScreenContent(progress: UserProgress) {
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LevelHeader(progress = progress)
        QuickStatsRow(progress = progress)
        Spacer(Modifier.height(16.dp))
        AchievementCategory.entries.forEach { category ->
            BadgeSection(
                category = category,
                achievements = progress.achievements.filter { it.category == category },
                onBadgeTapped = { selectedAchievement = it }
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    selectedAchievement?.let { achievement ->
        BadgeDetailSheet(
            achievement = achievement,
            onDismiss = { selectedAchievement = null }
        )
    }
}

@Composable
private fun LevelHeader(progress: UserProgress) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(primary, secondary)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(progress.levelEmoji, fontSize = 48.sp)
            Text(
                text = progress.levelName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Level ${progress.level}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (progress.xpRequiredForNextLevel == null) 1f
                    else progress.xpIntoCurrentLevel.toFloat() / (progress.xpIntoCurrentLevel + progress.xpRequiredForNextLevel)
                },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(4.dp))
            val xpText = if (progress.xpRequiredForNextLevel == null) {
                "${progress.totalXP} XP · Max Level"
            } else {
                "${progress.xpIntoCurrentLevel} / ${progress.xpIntoCurrentLevel + progress.xpRequiredForNextLevel} XP"
            }
            Text(
                text = xpText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun QuickStatsRow(progress: UserProgress) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCell(label = "TOTAL XP", value = "${progress.totalXP}")
        StatCell(label = "LEVEL", value = "${progress.level}")
        StatCell(
            label = "BADGES",
            value = "${progress.achievements.count { it.unlockedAt != null }} / ${progress.achievements.size}"
        )
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BadgeSection(
    category: AchievementCategory,
    achievements: List<Achievement>,
    onBadgeTapped: (Achievement) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements) { achievement ->
                BadgeItem(achievement = achievement, onTap = { onBadgeTapped(achievement) })
            }
        }
    }
}

@Composable
private fun BadgeItem(achievement: Achievement, onTap: () -> Unit) {
    val isUnlocked = achievement.unlockedAt != null
    Column(
        modifier = Modifier
            .width(72.dp)
            .alpha(if (isUnlocked) 1f else 0.3f)
            .clickable(onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(achievement.emoji, fontSize = 28.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeDetailSheet(achievement: Achievement, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(achievement.emoji, fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(achievement.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(achievement.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("+${achievement.xpReward} XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (achievement.unlockedAt != null) {
                Text("Unlocked!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Keep going to unlock this", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
