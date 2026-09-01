package com.example.habbittracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habbittracker.R
import com.example.habbittracker.domain.DayGoalProgress
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.ui.components.EmptyState
import com.example.habbittracker.ui.icons.HabitIcons
import com.example.habbittracker.ui.theme.HabbitTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Binds the today screen to its view model. */
@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val passedMessage = stringResource(R.string.today_passed_confirmation)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TodayEvent.DayPassed -> snackbarHostState.showSnackbar(passedMessage)
            }
        }
    }

    TodayScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onThemeChange = viewModel::onThemeChange,
        onDayNoteChange = viewModel::onDayNoteChange,
        onToggleCheck = viewModel::onToggleCheck,
        onIncrement = viewModel::onIncrement,
        onDecrement = viewModel::onDecrement,
        onSetProgress = viewModel::onSetProgress,
        onSetDayGoal = viewModel::onSetDayGoal,
        onUseDefaultGoal = viewModel::onUseDefaultGoal,
        onAddHabit = onAddHabit,
        onEditHabit = onEditHabit,
        onOpenHabits = onOpenHabits,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    snackbarHostState: SnackbarHostState,
    onThemeChange: (String) -> Unit,
    onDayNoteChange: (String) -> Unit,
    onToggleCheck: (HabitItem) -> Unit,
    onIncrement: (HabitItem) -> Unit,
    onDecrement: (HabitItem) -> Unit,
    onSetProgress: (HabitItem, Int) -> Unit,
    onSetDayGoal: (GoalType, Int) -> Unit,
    onUseDefaultGoal: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingGoal by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                // Separation through color and spacing rather than shadow.
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.today_add_habit))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                TodayHeader(
                    date = state.date,
                    isToday = state.isToday,
                    currentStreak = state.currentStreak,
                    onOpenHabits = onOpenHabits,
                    onOpenHistory = onOpenHistory,
                    onOpenSettings = onOpenSettings,
                )
            }

            item(key = "theme") {
                ThemeField(theme = state.theme, onThemeChange = onThemeChange)
            }

            item(key = "goal") {
                DayGoalCard(
                    goal = state.goal,
                    overridden = state.goalOverridden,
                    onEditGoal = { editingGoal = true },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (state.habits.isEmpty()) {
                if (state.loaded) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.habits_empty_title),
                            body = stringResource(R.string.habits_empty_body),
                            actionLabel = stringResource(R.string.today_add_habit),
                            onAction = onAddHabit,
                        )
                    }
                }
            } else {
                item(key = "habits_label") {
                    Text(
                        text = stringResource(R.string.today_habits_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    )
                }
                items(items = state.habits, key = { it.id }) { item ->
                    HabitRow(
                        item = item,
                        goalType = state.goal.goalType,
                        onToggle = onToggleCheck,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        onSetProgress = onSetProgress,
                        onEdit = { onEditHabit(it.id) },
                    )
                }
            }

            item(key = "day_note") {
                DayNoteField(
                    note = state.dayNote,
                    onNoteChange = onDayNoteChange,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
    }

    if (editingGoal) {
        DayGoalDialog(
            goalType = state.goal.goalType,
            threshold = state.goal.threshold.coerceAtLeast(AppSettings.GOAL_THRESHOLD_MIN),
            overridden = state.goalOverridden,
            onConfirm = { rule, points ->
                editingGoal = false
                onSetDayGoal(rule, points)
            },
            onUseDefault = {
                editingGoal = false
                onUseDefaultGoal()
            },
            onDismiss = { editingGoal = false },
        )
    }
}

/**
 * A free note on the day (F2). It sits below the habits because a look back is
 * written at the end of the day, and the header stays calm that way.
 */
@Composable
private fun DayNoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.today_note_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            BasicTextField(
                value = note,
                onValueChange = onNoteChange,
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                decorationBox = { innerTextField ->
                    if (note.isEmpty()) {
                        Text(
                            text = stringResource(R.string.today_note_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}

@Composable
private fun TodayHeader(
    date: LocalDate,
    isToday: Boolean,
    currentStreak: Int,
    onOpenHabits: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }
    val titleFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val separator = stringResource(R.string.today_subtitle_separator)
    val subtitle =
        buildString {
            append(date.format(formatter))
            if (currentStreak > 0) {
                append(separator)
                append(pluralStringResource(R.plurals.today_streak, currentStreak, currentStreak))
            }
        }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                // A backfilled day names its date; only the running day is "Today".
                text = if (isToday) stringResource(R.string.today_title) else date.format(titleFormatter),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenHabits) {
            Icon(
                imageVector = Icons.Outlined.Checklist,
                contentDescription = stringResource(R.string.today_open_habits),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenHistory) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = stringResource(R.string.today_open_history),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.today_open_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The day theme, editable inline. A pure label with no effect on the goal (F2). */
@Composable
private fun ThemeField(
    theme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BasicTextField(
            value = theme,
            onValueChange = onThemeChange,
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(imeAction = ImeAction.Done),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            decorationBox = { innerTextField ->
                if (theme.isEmpty()) {
                    Text(
                        text = stringResource(R.string.today_theme_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            },
        )
    }
}

// --- Previews ---------------------------------------------------------------

private fun previewState(passed: Boolean = false) =
    TodayUiState(
        date = LocalDate.of(2026, 8, 31),
        theme = if (passed) "Calm focus" else "",
        goal =
            DayGoalProgress(
                goalType = GoalType.POINTS,
                current = if (passed) 6 else 3,
                threshold = 6,
                status = if (passed) DayStatus.PASSED else DayStatus.FAILED,
            ),
        habits =
            listOf(
                HabitEntry(
                    Habit(1, "Drink water", HabitType.COUNTER, 8, "glasses", points = 2, icon = "water_drop"),
                    3,
                ),
                HabitEntry(
                    habit =
                        Habit(
                            2,
                            "Exercise",
                            HabitType.CHECK,
                            1,
                            points = 3,
                            required = true,
                            icon = "directions_run",
                        ),
                    progress = if (passed) 1 else 0,
                ),
                HabitEntry(Habit(3, "Read", HabitType.COUNTER, 30, "min", points = 2, icon = "menu_book"), 30),
                HabitEntry(Habit(4, "Meditation", HabitType.CHECK, 1, points = 1, icon = "self_improvement"), 0),
            ).map(::HabitItem),
        currentStreak = 4,
        loaded = true,
    )

@Preview(name = "Today, light", showBackground = true)
@Composable
private fun TodayScreenPreview() {
    HabbitTrackerTheme(darkTheme = false) {
        TodayScreen(
            state = previewState(),
            snackbarHostState = remember { SnackbarHostState() },
            onThemeChange = {},
            onDayNoteChange = {},
            onToggleCheck = {},
            onIncrement = {},
            onDecrement = {},
            onSetProgress = { _, _ -> },
            onSetDayGoal = { _, _ -> },
            onUseDefaultGoal = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHabits = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Today, dark, passed", showBackground = true)
@Composable
private fun TodayScreenDarkPreview() {
    HabbitTrackerTheme(darkTheme = true) {
        TodayScreen(
            state = previewState(passed = true),
            snackbarHostState = remember { SnackbarHostState() },
            onThemeChange = {},
            onDayNoteChange = {},
            onToggleCheck = {},
            onIncrement = {},
            onDecrement = {},
            onSetProgress = { _, _ -> },
            onSetDayGoal = { _, _ -> },
            onUseDefaultGoal = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHabits = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Today, empty", showBackground = true)
@Composable
private fun TodayScreenEmptyPreview() {
    HabbitTrackerTheme {
        TodayScreen(
            state = TodayUiState(date = LocalDate.of(2026, 8, 31), loaded = true),
            snackbarHostState = remember { SnackbarHostState() },
            onThemeChange = {},
            onDayNoteChange = {},
            onToggleCheck = {},
            onIncrement = {},
            onDecrement = {},
            onSetProgress = { _, _ -> },
            onSetDayGoal = { _, _ -> },
            onUseDefaultGoal = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHabits = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}
