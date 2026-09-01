package com.example.habbittracker.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.habbittracker.HabbitTrackerApp
import com.example.habbittracker.R
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** What the widget needs of a habit, kept apart from the domain model. */
data class WidgetHabit(val id: Long, val name: String, val done: Boolean)

/**
 * The day on the home screen, with a one-tap check-in per habit (F9).
 *
 * Everything stays local: the widget reads from Room and writes back through the
 * same repository the app uses, so the day status is recomputed by the same rules.
 */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as HabbitTrackerApp).container
        val snapshot = container.habitRepository.observeDay(LocalDate.now()).first()
        val entries = snapshot.entries.map { WidgetHabit(it.habit.id, it.habit.name, it.fulfilled) }

        provideContent {
            GlanceTheme {
                WidgetBody(theme = snapshot.themeName, entries = entries)
            }
        }
    }
}

@Composable
private fun WidgetBody(theme: String?, entries: List<WidgetHabit>) {
    val context = LocalContext.current
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp),
    ) {
        Text(
            text = theme ?: context.getString(R.string.today_title),
            style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (entries.isEmpty()) {
            Text(
                text = context.getString(R.string.habits_empty_title),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        entries.forEach { habit -> WidgetRow(habit) }
    }
}

@Composable
private fun WidgetRow(habit: WidgetHabit) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(
                    actionRunCallback<CompleteHabitAction>(
                        actionParametersOf(CompleteHabitAction.habitIdKey to habit.id),
                    ),
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A check mark rather than a second color: the widget follows the host's
        // palette, where the app's accent is not available.
        Text(
            text = if (habit.done) "✓ ${habit.name}" else habit.name,
            style =
                TextStyle(
                    color = if (habit.done) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                ),
        )
    }
}

/** Ticks a habit off from the widget and redraws it (F9). */
class CompleteHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[habitIdKey] ?: return
        val container = (context.applicationContext as HabbitTrackerApp).container
        container.habitRepository.completeHabit(LocalDate.now(), habitId)
        TodayWidget().updateAll(context)
    }

    companion object {
        val habitIdKey = ActionParameters.Key<Long>("habitId")
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
