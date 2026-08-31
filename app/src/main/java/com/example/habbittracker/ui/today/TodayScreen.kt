package com.example.habbittracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habbittracker.R
import com.example.habbittracker.domain.DayGoalProgress
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.ui.icons.HabitIcons
import com.example.habbittracker.ui.theme.HabbitTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Bindet den Heute-Screen an sein ViewModel. */
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
        onToggleCheck = viewModel::onToggleCheck,
        onIncrement = viewModel::onIncrement,
        onDecrement = viewModel::onDecrement,
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
    onToggleCheck: (HabitItem) -> Unit,
    onIncrement: (HabitItem) -> Unit,
    onDecrement: (HabitItem) -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                // Trennung ueber Farbe und Abstand statt ueber Schatten.
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.today_add_habit))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
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
                DayGoalCard(goal = state.goal, modifier = Modifier.padding(top = 4.dp))
            }

            if (state.habits.isEmpty()) {
                if (state.loaded) {
                    item(key = "empty") { EmptyHabits(onAddHabit = onAddHabit) }
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
                        onEdit = { onEditHabit(it.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(
    date: LocalDate,
    currentStreak: Int,
    onOpenHabits: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d. MMMM", locale) }
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
                text = stringResource(R.string.today_title),
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

/** Tagesthema, direkt inline editierbar. Reine Kennzeichnung, ohne Einfluss auf das Ziel (F2). */
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
                androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
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

@Composable
private fun EmptyHabits(onAddHabit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = HabitIcons[HabitIcons.FALLBACK],
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.today_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.today_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onAddHabit) {
            Text(stringResource(R.string.today_add_habit))
        }
    }
}

// --- Previews ---------------------------------------------------------------

private fun previewState(passed: Boolean = false) =
    TodayUiState(
        date = LocalDate.of(2026, 8, 31),
        theme = if (passed) "Ruhiger Fokus" else "",
        goal = DayGoalProgress(GoalType.POINTS, current = if (passed) 6 else 3, threshold = 6, passed = passed),
        habits =
            listOf(
                HabitEntry(
                    Habit(1, "Wasser trinken", HabitType.COUNTER, 8, "Glaeser", points = 2, icon = "water_drop"),
                    3,
                ),
                HabitEntry(
                    habit = Habit(2, "Sport", HabitType.CHECK, 1, points = 3, required = true, icon = "directions_run"),
                    progress = if (passed) 1 else 0,
                ),
                HabitEntry(Habit(3, "Lesen", HabitType.AMOUNT, 30, "min", points = 2, icon = "menu_book"), 30),
                HabitEntry(Habit(4, "Meditation", HabitType.CHECK, 1, points = 1, icon = "self_improvement"), 0),
            ).map(::HabitItem),
        currentStreak = 4,
        loaded = true,
    )

@Preview(name = "Heute, hell", showBackground = true)
@Composable
private fun TodayScreenPreview() {
    HabbitTrackerTheme(darkTheme = false) {
        TodayScreen(
            state = previewState(),
            snackbarHostState = remember { SnackbarHostState() },
            onThemeChange = {},
            onToggleCheck = {},
            onIncrement = {},
            onDecrement = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHabits = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Heute, dunkel, bestanden", showBackground = true)
@Composable
private fun TodayScreenDarkPreview() {
    HabbitTrackerTheme(darkTheme = true) {
        TodayScreen(
            state = previewState(passed = true),
            snackbarHostState = remember { SnackbarHostState() },
            onThemeChange = {},
            onToggleCheck = {},
            onIncrement = {},
            onDecrement = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHabits = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Heute, leer", showBackground = true)
@Composable
private fun TodayScreenEmptyPreview() {
    HabbitTrackerTheme {
        TodayScreen(
            state = TodayUiState(date = LocalDate.of(2026, 8, 31), loaded = true),
            snackbarHostState = remember { SnackbarHostState() },
            onThemeChange = {},
            onToggleCheck = {},
            onIncrement = {},
            onDecrement = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHabits = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}
