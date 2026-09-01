package com.example.habbittracker.ui.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.WeekSpan
import com.example.habbittracker.domain.model.covers
import com.example.habbittracker.ui.components.LabeledSection
import com.example.habbittracker.ui.components.SegmentedChoice
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Everything the editor needs for the rhythm of a habit (F8). */
data class RhythmCallbacks(
    val onKindChange: (HabitKind) -> Unit,
    val onWeekChange: (LocalDate) -> Unit,
    val onWeekSpanChange: (WeekSpan) -> Unit,
    val onRecurrenceChange: (Recurrence) -> Unit,
    val onParentChange: (Long) -> Unit,
    val onToggleDow: (Int) -> Unit,
)

/**
 * Where the habit sits in the week (F8): every day, bound to one week, or a part
 * of such a week that shows up on chosen weekdays.
 */
@Composable
fun RhythmSection(
    form: HabitFormState,
    weeks: List<Habit>,
    callbacks: RhythmCallbacks,
    modifier: Modifier = Modifier,
) {
    LabeledSection(
        label = stringResource(R.string.habit_editor_kind_label),
        hint = stringResource(R.string.habit_editor_kind_hint),
        modifier = modifier,
    ) {
        SegmentedChoice(
            options = HabitKind.entries,
            selected = form.kind,
            onSelect = callbacks.onKindChange,
        ) { stringResource(it.labelRes()) }
        if (form.showsWeek) WeekFields(form, callbacks)
        if (form.showsParent) SubFields(form, weeks, callbacks)
    }
}

/** The week a week habit is bound to, its span and how it shows up in it. */
@Composable
private fun WeekFields(form: HabitFormState, callbacks: RhythmCallbacks) {
    val week = form.weekStart ?: return
    Spacer(Modifier.height(12.dp))
    WeekStepper(weekStart = week, span = form.weekSpan, onWeekChange = callbacks.onWeekChange)
    Spacer(Modifier.height(12.dp))
    SegmentedChoice(
        options = WeekSpan.entries,
        selected = form.weekSpan,
        onSelect = callbacks.onWeekSpanChange,
    ) { stringResource(it.labelRes()) }
    Spacer(Modifier.height(8.dp))
    SegmentedChoice(
        options = Recurrence.entries,
        selected = form.recurrence,
        onSelect = callbacks.onRecurrenceChange,
    ) { stringResource(it.labelRes()) }
}

/** The week the habit joins and the days it appears on inside it. */
@Composable
private fun SubFields(form: HabitFormState, weeks: List<Habit>, callbacks: RhythmCallbacks) {
    Spacer(Modifier.height(12.dp))
    if (weeks.isEmpty()) {
        Text(
            text = stringResource(R.string.habit_editor_parent_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    ParentChips(weeks = weeks, selected = form.parentId, onSelect = callbacks.onParentChange)
    Spacer(Modifier.height(12.dp))
    // A sub habit cannot appear on a day its week does not have.
    val span = weeks.firstOrNull { it.id == form.parentId }?.weekSpan ?: WeekSpan.FULL
    WeekdayChips(selected = form.assignedDows, span = span, onToggle = callbacks.onToggleDow)
}

@Composable
private fun WeekStepper(weekStart: LocalDate, span: WeekSpan, onWeekChange: (LocalDate) -> Unit) {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val last = weekStart.plusDays(if (span == WeekSpan.WORKWEEK) WORKWEEK_LAST else FULL_WEEK_LAST)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onWeekChange(weekStart.minusWeeks(1)) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.habit_editor_week_previous),
            )
        }
        Text(
            text = "${formatter.format(weekStart)} – ${formatter.format(last)}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onWeekChange(weekStart.plusWeeks(1)) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.habit_editor_week_next),
            )
        }
    }
}

@Composable
private fun ParentChips(weeks: List<Habit>, selected: Long?, onSelect: (Long) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.habit_editor_parent_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            weeks.forEach { week ->
                FilterChip(
                    selected = week.id == selected,
                    onClick = { onSelect(week.id) },
                    label = { Text(week.name) },
                )
            }
        }
    }
}

@Composable
private fun WeekdayChips(selected: Set<Int>, span: WeekSpan, onToggle: (Int) -> Unit) {
    val locale = Locale.getDefault()
    Column {
        Text(
            text = stringResource(R.string.habit_editor_dows_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.filter { span.covers(it.value) }.forEach { day ->
                FilterChip(
                    selected = day.value in selected,
                    onClick = { onToggle(day.value) },
                    label = { Text(day.getDisplayName(TextStyle.NARROW, locale)) },
                )
            }
        }
    }
}

private const val WORKWEEK_LAST = 4L
private const val FULL_WEEK_LAST = 6L
