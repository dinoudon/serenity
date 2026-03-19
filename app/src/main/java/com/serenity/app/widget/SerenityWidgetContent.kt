package com.serenity.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.serenity.app.ui.MainActivity

/**
 * Widget state passed to SerenityWidgetContent.
 * score == null means no data at all.
 * isYesterdayScore == true means score is from a past day (show amber dot).
 */
data class WidgetState(
    val score: Int?,
    val streak: Int,
    val isYesterdayScore: Boolean,
    val themeName: String  // "SAGE", "LAVENDER", "SAND"
)

/**
 * Resolves theme colors by theme name.
 * Returns a Pair of (primaryColor, lightColor).
 * Unknown themes default to SAGE colors.
 */
internal fun themeColors(themeName: String): Pair<Color, Color> =
    when (themeName) {
        "LAVENDER" -> Color(0xFF8b7eaa) to Color(0xFFb4a8cc)
        "SAND" -> Color(0xFFb09a7c) to Color(0xFFc8b8a4)
        else -> Color(0xFF7c9a7c) to Color(0xFFa8c4a8) // SAGE default
    }

@Composable
fun SerenityWidgetContent(state: WidgetState) {
    val (primaryColor, lightColor) = themeColors(state.themeName)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(primaryColor)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App name
            Text(
                text = "Serenity",
                style = TextStyle(
                    color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Score with optional amber dot
            Box(contentAlignment = Alignment.TopEnd) {
                if (state.score != null) {
                    Text(
                        text = "${state.score}",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    if (state.isYesterdayScore) {
                        Box(
                            modifier = GlanceModifier
                                .size(8.dp)
                                .background(Color(0xFFFFA726)) // amber
                                .cornerRadius(4.dp)
                        ) {}
                    }
                } else {
                    Text(
                        text = "–",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            // "wellness score" label
            Text(
                text = if (state.score != null) "wellness score" else "no data yet",
                style = TextStyle(
                    color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                    fontSize = 9.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Progress bar
            if (state.score != null) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                        .cornerRadius(2.dp)
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth(state.score / 100f)
                            .height(4.dp)
                            .background(Color.White)
                            .cornerRadius(2.dp)
                    ) {}
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            // Streak
            Text(
                text = if (state.score != null) "🔥 ${state.streak} days" else "Start your ritual",
                style = TextStyle(
                    color = ColorProvider(Color.White.copy(alpha = 0.85f)),
                    fontSize = 10.sp
                )
            )
        }
    }
}
