package com.example.habbittracker.ui.pause

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.habbittracker.domain.model.Pause
import com.example.habbittracker.ui.components.BackTopAppBar
import com.example.habbittracker.ui.components.EmptyState
import com.example.habbittracker.ui.components.LabeledSection
import com.example.habbittracker.ui.components.SettingRow
import com.example.habbittracker.ui.components.ValueStepper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Breaks and holidays (F4): days nothing is asked of you. */
@Composable
fun PauseScreen(
    state: PauseListUiState,
    today: LocalDate,
    onSave: (Pause) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Pause?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopAppBar(title = stringResource(R.string.pauses_title), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = Pause(id = 0, from = today, to = today.plusDays(6)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.pauses_add))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = state.pauses, key = { it.id }) { pause ->
                SettingRow(
                    title = rangeLabel(pause),
                    subtitle =
                        state.habitName(pause.habitId)
                            ?: stringResource(R.string.pauses_everything),
                    onClick = { editing = pause },
                ) {
                    IconButton(onClick = { onDelete(pause.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.pauses_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.loaded && state.pauses.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.pauses_empty_title),
                        body = stringResource(R.string.pauses_empty_body),
                    )
                }
            }
        }
    }

    editing?.let { pause ->
        PauseDialog(
            pause = pause,
            today = today,
            habits = state.habits,
            onConfirm = {
                editing = null
                onSave(it)
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun rangeLabel(pause: Pause): String {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    return stringResource(R.string.pauses_range, pause.from.format(formatter), pause.to.format(formatter))
}

/**
 * A break is set as an offset and a length rather than two dates: the app has no
 * date picker yet, and "from in three days, for a week" is how a holiday is
 * usually described anyway.
 */
@Composable
private fun PauseDialog(
    pause: Pause,
    today: LocalDate,
    habits: List<Habit>,
    onConfirm: (Pause) -> Unit,
    onDismiss: () -> Unit,
) {
    var startsIn by remember {
        mutableStateOf(
            ChronoUnit.DAYS
                .between(today, pause.from)
                .toInt()
                .coerceIn(0, MAX_OFFSET),
        )
    }
    var length by remember {
        mutableStateOf((ChronoUnit.DAYS.between(pause.from, pause.to).toInt() + 1).coerceIn(1, MAX_LENGTH))
    }
    var habitId by remember { mutableStateOf(pause.habitId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pauses_dialog_title)) },
        text = {
            Column {
                LabeledSection(label = stringResource(R.string.pauses_dialog_from)) {
                    ValueStepper(
                        value = startsIn,
                        onValueChange = { startsIn = it },
                        decreaseLabel = stringResource(R.string.pauses_earlier),
                        increaseLabel = stringResource(R.string.pauses_later),
                        range = 0..MAX_OFFSET,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LabeledSection(label = stringResource(R.string.pauses_dialog_length)) {
                    ValueStepper(
                        value = length,
                        onValueChange = { length = it },
                        decreaseLabel = stringResource(R.string.pauses_shorter),
                        increaseLabel = stringResource(R.string.pauses_longer),
                        range = 1..MAX_LENGTH,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LabeledSection(label = stringResource(R.string.pauses_dialog_applies)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = habitId == null,
                            onClick = { habitId = null },
                            label = { Text(stringResource(R.string.pauses_everything)) },
                        )
                        habits.forEach { habit ->
                            FilterChip(
                                selected = habitId == habit.id,
                                onClick = { habitId = habit.id },
                                label = { Text(habit.name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val from = today.plusDays(startsIn.toLong())
                    onConfirm(
                        pause.copy(from = from, to = from.plusDays((length - 1).toLong()), habitId = habitId),
                    )
                },
            ) {
                Text(stringResource(R.string.habit_editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_editor_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

private const val MAX_OFFSET = 365
private const val MAX_LENGTH = 90
