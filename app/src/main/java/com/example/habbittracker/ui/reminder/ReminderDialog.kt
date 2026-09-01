package com.example.habbittracker.ui.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.Reminder
import com.example.habbittracker.ui.components.LabeledSection
import com.example.habbittracker.ui.components.ValueStepper
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** Sets up one reminder: a time, the weekdays it runs on and what it is about (F5). */
@Composable
fun ReminderDialog(
    reminder: Reminder,
    habits: List<Habit>,
    onConfirm: (Reminder) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(reminder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TimeFields(time = draft.time, onTimeChange = { draft = draft.copy(time = it) })
                Spacer(Modifier.height(16.dp))
                LabeledSection(label = stringResource(R.string.reminders_dialog_days)) {
                    WeekdayChips(
                        selected = draft.daysOfWeek,
                        onToggle = { draft = draft.copy(daysOfWeek = it) },
                    )
                }
                Spacer(Modifier.height(16.dp))
                LabeledSection(label = stringResource(R.string.reminders_dialog_habit)) {
                    HabitChips(
                        habits = habits,
                        selected = draft.habitId,
                        onSelect = { draft = draft.copy(habitId = it) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) {
                Text(stringResource(R.string.habit_editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_editor_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun TimeFields(time: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LabeledSection(label = stringResource(R.string.reminders_hour), modifier = Modifier.weight(1f)) {
            ValueStepper(
                value = time.hour,
                onValueChange = { onTimeChange(time.withHour(it)) },
                decreaseLabel = stringResource(R.string.settings_threshold_decrease),
                increaseLabel = stringResource(R.string.settings_threshold_increase),
                range = 0..23,
            )
        }
        LabeledSection(label = stringResource(R.string.reminders_minute), modifier = Modifier.weight(1f)) {
            ValueStepper(
                value = time.minute,
                onValueChange = { onTimeChange(time.withMinute(it)) },
                decreaseLabel = stringResource(R.string.settings_threshold_decrease),
                increaseLabel = stringResource(R.string.settings_threshold_increase),
                range = 0..59,
            )
        }
    }
}

@Composable
private fun WeekdayChips(selected: Set<Int>, onToggle: (Set<Int>) -> Unit) {
    val locale = Locale.getDefault()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.forEach { day ->
            val checked = day.value in selected
            FilterChip(
                selected = checked,
                onClick = {
                    onToggle(if (checked) selected - day.value else selected + day.value)
                },
                label = { Text(day.getDisplayName(TextStyle.NARROW, locale)) },
            )
        }
    }
}

@Composable
private fun HabitChips(habits: List<Habit>, selected: Long?, onSelect: (Long?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.reminders_general)) },
        )
        habits.forEach { habit ->
            FilterChip(
                selected = selected == habit.id,
                onClick = { onSelect(habit.id) },
                label = { Text(habit.name) },
            )
        }
    }
}
