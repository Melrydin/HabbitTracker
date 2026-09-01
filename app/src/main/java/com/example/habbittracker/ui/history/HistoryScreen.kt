package com.example.habbittracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.CompletionRate
import com.example.habbittracker.domain.RateComparison
import com.example.habbittracker.domain.WeekdayRate
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.ui.components.BackTopAppBar
import com.example.habbittracker.ui.components.LabeledSection
import com.example.habbittracker.ui.components.MonthHeatmap
import com.example.habbittracker.ui.components.SettingRow
import com.example.habbittracker.ui.theme.HabbitTrackerTheme
import com.example.habbittracker.ui.theme.HabitTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Heatmap, streaks and completion rate over the stored days (F4). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopAppBar(title = stringResource(R.string.history_title), onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StreakRow(currentStreak = state.currentStreak, longestStreak = state.longestStreak)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MonthSwitcher(
                        month = state.month,
                        canGoForward = state.canGoForward,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                    )
                    Spacer(Modifier.height(16.dp))
                    MonthHeatmap(
                        month = state.month,
                        statuses = state.statuses,
                        today = state.today,
                        frozen = state.frozen,
                        onDayClick = onOpenDay,
                    )
                    Spacer(Modifier.height(16.dp))
                    Legend()
                }
            }

            CompletionRateCard(rate = state.monthRate)

            PatternsSection(state = state)

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Weekday tendencies and how the current period compares (F4). */
@Composable
private fun PatternsSection(state: HistoryUiState, modifier: Modifier = Modifier) {
    LabeledSection(label = stringResource(R.string.history_stats_label), modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.bestWeekday?.let {
                SettingRow(
                    title = weekdayValue(it),
                    subtitle = stringResource(R.string.history_best_weekday),
                )
            }
            state.weakestWeekday?.let {
                SettingRow(
                    title = weekdayValue(it),
                    subtitle = stringResource(R.string.history_weakest_weekday),
                )
            }
            state.weekComparison?.let {
                SettingRow(
                    title = comparisonValue(it),
                    subtitle = stringResource(R.string.history_this_week),
                )
            }
            state.monthComparison?.let {
                SettingRow(
                    title = comparisonValue(it),
                    subtitle = stringResource(R.string.history_this_month),
                )
            }
        }
    }
}

@Composable
private fun weekdayValue(rate: WeekdayRate): String {
    val name = rate.day.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return stringResource(R.string.history_weekday_value, name, rate.rate.percent)
}

@Composable
private fun comparisonValue(comparison: RateComparison): String {
    val difference = comparison.differenceInPercent
    val trend =
        when {
            difference > 0 -> stringResource(R.string.history_more, difference)
            difference < 0 -> stringResource(R.string.history_less, -difference)
            else -> stringResource(R.string.history_same)
        }
    return stringResource(R.string.history_against_previous, comparison.current.percent, trend)
}

@Composable
private fun StreakRow(
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.history_current_streak),
            value = pluralStringResource(R.plurals.history_streak_days, currentStreak, currentStreak),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.history_longest_streak),
            value = pluralStringResource(R.plurals.history_streak_days, longestStreak, longestStreak),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CompletionRateCard(rate: CompletionRate, modifier: Modifier = Modifier) {
    StatCard(
        label = stringResource(R.string.history_completion_rate),
        value = stringResource(R.string.history_rate_percent, rate.percent),
        detail =
            if (rate.evaluated == 0) {
                stringResource(R.string.history_rate_none)
            } else {
                stringResource(R.string.history_rate_detail, rate.passed, rate.evaluated)
            },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    month: YearMonth,
    canGoForward: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = stringResource(R.string.history_previous_month),
            )
        }
        Text(
            text = month.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNextMonth, enabled = canGoForward) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = stringResource(R.string.history_next_month),
            )
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val status = HabitTheme.status
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(status.passedContainer, stringResource(R.string.history_legend_passed))
        LegendItem(MaterialTheme.colorScheme.surfaceContainerHigh, stringResource(R.string.history_legend_failed))
        LegendItem(Color.Transparent, stringResource(R.string.history_legend_neutral))
        LegendItem(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            label = stringResource(R.string.history_legend_frozen),
            dotted = true,
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, modifier: Modifier = Modifier, dotted: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = RoundedCornerShape(4.dp),
            color = color,
        ) {
            if (dotted) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(4.dp).background(HabitTheme.status.passed, CircleShape))
                }
            }
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "History", showBackground = true)
@Composable
private fun HistoryPreview() {
    val month = YearMonth.of(2026, 8)
    HabbitTrackerTheme {
        HistoryScreen(
            state =
                HistoryUiState(
                    month = month,
                    today = month.atDay(31),
                    statuses =
                        (1..30).associate { day ->
                            month.atDay(day) to
                                when {
                                    day % 7 == 0 -> DayStatus.FAILED
                                    day % 5 == 0 -> DayStatus.NEUTRAL
                                    else -> DayStatus.PASSED
                                }
                        },
                    frozen = setOf(month.atDay(7)),
                    currentStreak = 4,
                    longestStreak = 11,
                    monthRate = CompletionRate(passed = 22, failed = 4),
                ),
            onPreviousMonth = {},
            onNextMonth = {},
            onOpenDay = {},
            onBack = {},
        )
    }
}
