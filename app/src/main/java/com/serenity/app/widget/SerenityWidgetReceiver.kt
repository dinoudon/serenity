package com.serenity.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.room.Room
import com.serenity.app.data.local.SerenityDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

// Separate DataStore instance for widget access (same file as app DataStore)
private val Context.widgetUserPrefsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_preferences")

class SerenityWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SerenityWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            refreshWidgetData(context)
        }
    }

    private suspend fun refreshWidgetData(context: Context) {
        try {
            // Read theme from user_preferences DataStore
            val prefs = context.widgetUserPrefsDataStore.data.first()
            val themeKey = stringPreferencesKey("theme")
            val theme = prefs[themeKey] ?: "SAGE"

            // Access Room DB directly (no Hilt in widget context)
            val db = Room.databaseBuilder(
                context.applicationContext,
                SerenityDatabase::class.java,
                "serenity_database"
            ).build()
            val dao = db.ritualDao()

            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val todayRitual = dao.getByDate(today.toString()).first()
            val yesterdayRitual = if (todayRitual == null) {
                dao.getByDate(yesterday.toString()).first()
            } else {
                null
            }

            val displayRitual = todayRitual ?: yesterdayRitual
            val score = displayRitual?.wellnessScore
            val isYesterday = todayRitual == null && yesterdayRitual != null

            // Compute streak from sorted descending dates
            val streak = computeStreak(dao.getAllDatesDescending(), today)

            // Update all widget instances
            val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(SerenityWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { widgetPrefs ->
                    if (score != null) {
                        widgetPrefs[intPreferencesKey("widget_score")] = score
                    }
                    widgetPrefs[intPreferencesKey("widget_streak")] = streak
                    widgetPrefs[booleanPreferencesKey("widget_is_yesterday")] = isYesterday
                    widgetPrefs[stringPreferencesKey("widget_theme")] = theme
                }
            }
            SerenityWidget().updateAll(context)

            db.close()
        } catch (e: Exception) {
            // Silent fail — widget will show last known state
        }
    }

    /**
     * Counts consecutive days ending on [today] (or yesterday if today is missing)
     * from the list of date strings sorted descending.
     */
    private fun computeStreak(datesDesc: List<String>, today: LocalDate): Int {
        if (datesDesc.isEmpty()) return 0
        val dates = datesDesc.mapNotNull {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }.sortedDescending()

        // Streak may start from today or yesterday
        var expected = if (dates.firstOrNull() == today) today else today.minusDays(1)
        if (dates.firstOrNull() != expected) return 0

        var count = 0
        for (date in dates) {
            if (date == expected) {
                count++
                expected = expected.minusDays(1)
            } else {
                break
            }
        }
        return count
    }
}
