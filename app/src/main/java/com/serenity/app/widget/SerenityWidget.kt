package com.serenity.app.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition

class SerenityWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val score = prefs[intPreferencesKey("widget_score")]
            val streak = prefs[intPreferencesKey("widget_streak")] ?: 0
            val isYesterday = prefs[booleanPreferencesKey("widget_is_yesterday")] ?: false
            val theme = prefs[stringPreferencesKey("widget_theme")] ?: "SAGE"

            SerenityWidgetContent(
                state = WidgetState(
                    score = score,
                    streak = streak,
                    isYesterdayScore = isYesterday,
                    themeName = theme
                )
            )
        }
    }
}
