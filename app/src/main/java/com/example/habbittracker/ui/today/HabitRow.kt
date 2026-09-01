package com.example.habbittracker.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.ui.components.ProgressTrack
import com.example.habbittracker.ui.icons.HabitIcons
import com.example.habbittracker.ui.theme.HabitTheme

/**
 * A row of the daily list (F3). The stepper moves the count one at a time;
 * tapping the row or holding it opens the editor (F1).
 */
@Composable
fun HabitRow(
    item: HabitItem,
    goalType: GoalType,
    onIncrement: (HabitItem) -> Unit,
    onDecrement: (HabitItem) -> Unit,
    onEdit: (HabitItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = item.entry.habit
    val status = HabitTheme.status

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .clickable(role = Role.Button) { onEdit(item) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = HabitIcons[habit.icon],
                contentDescription = null,
                tint = if (item.fulfilled) status.passed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = habitSubtitle(item, goalType)
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                ProgressTrack(
                    fraction = item.entry.progress.toFloat() / habit.target,
                    color = if (item.fulfilled) status.passed else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    height = 4.dp,
                )
            }

            Stepper(
                habitName = habit.name,
                canDecrease = item.entry.progress > 0,
                onDecrement = { onDecrement(item) },
                onIncrement = { onIncrement(item) },
            )
        }
    }
}

@Composable
private fun Stepper(
    habitName: String,
    canDecrease: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDecrement()
            },
            enabled = canDecrease,
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.habit_decrease, habitName),
            )
        }
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onIncrement()
            },
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.habit_increase, habitName),
            )
        }
    }
}

/**
 * Second line: the current value for counters, and for CHECK whatever moves the day
 * forward (points or required), so the rule of the day stays readable.
 */
@Composable
private fun habitSubtitle(item: HabitItem, goalType: GoalType): String {
    val habit = item.entry.habit
    val unit = habit.unit
    val progress =
        if (unit.isNullOrBlank()) {
            stringResource(R.string.habit_progress_plain, item.entry.progress, habit.target)
        } else {
            stringResource(R.string.habit_progress_with_unit, item.entry.progress, habit.target, unit)
        }
    // What the habit is worth only matters where the rule of the day counts it.
    val worth =
        when {
            goalType == GoalType.ALL_REQUIRED && habit.required -> stringResource(R.string.habit_required)
            goalType == GoalType.POINTS -> pluralStringResource(R.plurals.habit_points, habit.points, habit.points)
            else -> ""
        }
    val separator = stringResource(R.string.today_subtitle_separator)
    return listOf(progress, worth).filter { it.isNotEmpty() }.joinToString(separator)
}
