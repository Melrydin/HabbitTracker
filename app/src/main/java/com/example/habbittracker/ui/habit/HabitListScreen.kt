package com.example.habbittracker.ui.habit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.ui.components.BackTopAppBar
import com.example.habbittracker.ui.components.EmptyState
import com.example.habbittracker.ui.icons.HabitIcons
import com.example.habbittracker.ui.theme.HabbitTrackerTheme
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Overview of every habit (F1). The archive lives here too, so that archiving stays
 * reversible instead of being a deletion in disguise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    state: HabitListUiState,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopAppBar(title = stringResource(R.string.habit_list_title), onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
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
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = state.active, key = { it.id }) { habit ->
                HabitSummaryRow(habit = habit, onClick = { onEditHabit(habit.id) })
            }

            if (state.archived.isNotEmpty()) {
                item(key = "archived_label") {
                    Text(
                        text = stringResource(R.string.habit_list_archived),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp),
                    )
                }
                items(items = state.archived, key = { it.id }) { habit ->
                    HabitSummaryRow(
                        habit = habit,
                        onClick = { onEditHabit(habit.id) },
                        dimmed = true,
                    )
                }
            }

            if (state.loaded && state.isEmpty) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.habits_empty_title),
                        body = stringResource(R.string.habits_empty_body),
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitSummaryRow(
    habit: Habit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val contentColor =
        if (dimmed) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = HabitIcons[habit.icon],
                contentDescription = null,
                tint =
                    if (dimmed) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = habitSummary(habit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Second line: rhythm, type, target, points and required, exactly as set in the editor. */
@Composable
private fun habitSummary(habit: Habit): String {
    val parts =
        buildList {
            rhythmOf(habit)?.let(::add)
            add(stringResource(habit.type.labelRes()))
            if (habit.type != HabitType.CHECK) {
                val unit = habit.unit
                add(
                    if (unit.isNullOrBlank()) {
                        stringResource(R.string.habit_summary_target_plain, habit.target)
                    } else {
                        stringResource(R.string.habit_summary_target, habit.target, unit)
                    },
                )
            }
            add(pluralStringResource(R.plurals.habit_points, habit.points, habit.points))
            if (habit.required) add(stringResource(R.string.habit_required))
        }
    return parts.joinToString(stringResource(R.string.today_subtitle_separator))
}

/**
 * Which week a habit lives in (F8). A habit that runs every day says nothing here,
 * because that is the normal case and would only add noise to every row.
 */
@Composable
private fun rhythmOf(habit: Habit): String? {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    return when (habit.kind) {
        HabitKind.SIMPLE -> {
            null
        }

        HabitKind.WEEKLY -> {
            habit.weekStart?.let { stringResource(R.string.habit_summary_week, formatter.format(it)) }
        }

        HabitKind.SUB -> {
            habit.assignedDows
                .sorted()
                .joinToString(" ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, locale) }
        }
    }
}

@Preview(name = "Habits", showBackground = true)
@Composable
private fun HabitListPreview() {
    HabbitTrackerTheme {
        HabitListScreen(
            state =
                HabitListUiState(
                    active =
                        listOf(
                            Habit(
                                1,
                                "Drink water",
                                HabitType.COUNTER,
                                8,
                                "glasses",
                                points = 2,
                                icon = "water_drop",
                            ),
                            Habit(
                                2,
                                "Exercise",
                                HabitType.CHECK,
                                1,
                                points = 3,
                                required = true,
                                icon = "directions_run",
                            ),
                        ),
                    archived =
                        listOf(
                            Habit(3, "Vocabulary", HabitType.COUNTER, 20, "min", icon = "menu_book", archived = true),
                        ),
                    loaded = true,
                ),
            onAddHabit = {},
            onEditHabit = {},
            onBack = {},
        )
    }
}
