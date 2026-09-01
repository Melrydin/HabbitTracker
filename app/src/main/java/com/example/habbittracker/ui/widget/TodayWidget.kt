package com.example.habbittracker.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.glance.appwidget.cornerRadius
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
import com.example.habbittracker.data.DaySnapshot
import com.example.habbittracker.domain.model.HabitType
import java.time.LocalDate

/** What the widget needs of a habit, kept apart from the domain model. */
data class WidgetHabit(
    val id: Long,
    val name: String,
    val done: Boolean,
    val progress: Int,
    val target: Int,
    val counter: Boolean,
)

/**
 * The day on the home screen, with a one-tap check-in per habit (F9).
 *
 * Everything stays local: the widget reads from Room and writes back through the
 * same repository the app uses, so the day status is recomputed by the same rules.
 */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as HabbitTrackerApp).container
        val day = container.habitRepository.observeDay(LocalDate.now())

        provideContent {
            // Collected rather than read once: a habit ticked off in the app has to
            // reach the home screen too, not only one ticked off here.
            val snapshot by day.collectAsState(initial = null)
            GlanceTheme {
                WidgetBody(theme = snapshot?.themeName, entries = snapshot.toWidgetHabits())
            }
        }
    }
}

private fun DaySnapshot?.toWidgetHabits(): List<WidgetHabit> =
    this?.entries.orEmpty().map { entry ->
        WidgetHabit(
            id = entry.habit.id,
            name = entry.habit.name,
            done = entry.fulfilled,
            progress = entry.progress,
            target = entry.habit.target,
            counter = entry.habit.type == HabitType.COUNTER,
        )
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
        // Glance adds its padding modifiers up rather than nesting them, so the gap
        // between two rows has to be a spacer of its own.
        entries.forEachIndexed { index, habit ->
            if (index > 0) Spacer(GlanceModifier.height(4.dp))
            WidgetRow(habit)
        }
    }
}

/**
 * One habit. A finished one is laid on a filled surface of the host's palette
 * rather than in the app's accent, which is not available out here. The rounded
 * corner only exists from Android 12 on; below that the fill is square, which
 * still reads as done.
 */
@Composable
private fun WidgetRow(habit: WidgetHabit) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .then(
                    if (habit.done) GlanceModifier.background(GlanceTheme.colors.primaryContainer) else GlanceModifier,
                ).padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable(
                    actionRunCallback<StepHabitAction>(
                        actionParametersOf(StepHabitAction.habitIdKey to habit.id),
                    ),
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (habit.done) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurface
        Text(
            text = if (habit.done) "✓ ${habit.name}" else habit.name,
            style = TextStyle(color = color),
            modifier = GlanceModifier.defaultWeight(),
        )
        // Only a counter has a state a check mark cannot express.
        if (habit.counter) {
            Text(
                text = LocalContext.current.getString(R.string.widget_counter, habit.progress, habit.target),
                style = TextStyle(color = color),
            )
        }
    }
}

/** Adds one step to a habit from the widget and redraws it (F9). */
class StepHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[habitIdKey] ?: return
        val container = (context.applicationContext as HabbitTrackerApp).container
        container.habitRepository.incrementHabit(LocalDate.now(), habitId)
        TodayWidget().updateAll(context)
    }

    companion object {
        val habitIdKey = ActionParameters.Key<Long>("habitId")
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
