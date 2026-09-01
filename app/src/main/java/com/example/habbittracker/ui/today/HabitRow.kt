package com.example.habbittracker.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Polarity
import com.example.habbittracker.ui.components.ProgressTrack
import com.example.habbittracker.ui.icons.HabitIcons
import com.example.habbittracker.ui.theme.HabitTheme

/**
 * A row of the daily list (F3). CHECK toggles when the whole row is tapped;
 * A counter gets a stepper and opens the editor on tap instead.
 * A long press always leads to the editor (F1).
 */
@Composable
fun HabitRow(
    item: HabitItem,
    goalType: GoalType,
    onToggle: (HabitItem) -> Unit,
    onIncrement: (HabitItem) -> Unit,
    onDecrement: (HabitItem) -> Unit,
    onSetProgress: (HabitItem, Int) -> Unit,
    onEdit: (HabitItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entering by remember { mutableStateOf(false) }
    val habit = item.entry.habit
    val status = HabitTheme.status
    val haptics = LocalHapticFeedback.current
    val isCheck = habit.type == HabitType.CHECK
    val editLabel = stringResource(R.string.habit_edit_action)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .combinedClickable(
                        role = if (isCheck) Role.Checkbox else Role.Button,
                        onClick = {
                            if (isCheck) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggle(item)
                            } else {
                                onEdit(item)
                            }
                        },
                        onLongClick = { onEdit(item) },
                    ).then(
                        // For CHECK a tap ticks the habit off, so TalkBack needs its
                        // own route into the editor.
                        if (isCheck) {
                            Modifier.semantics {
                                toggleableState = ToggleableState(item.fulfilled)
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction(editLabel) {
                                            onEdit(item)
                                            true
                                        },
                                    )
                            }
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = 16.dp, vertical = 14.dp),
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
                if (!isCheck) {
                    Spacer(Modifier.height(10.dp))
                    ProgressTrack(
                        fraction = item.entry.progress.toFloat() / habit.target,
                        color = if (item.fulfilled) status.passed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        height = 4.dp,
                    )
                }
            }

            if (isCheck) {
                CheckMarker(checked = item.fulfilled)
            } else {
                Stepper(
                    habitName = habit.name,
                    progress = item.entry.progress,
                    onDecrement = { onDecrement(item) },
                    onIncrement = { onIncrement(item) },
                    onEnterValue = { entering = true },
                )
            }
        }
    }

    if (entering) {
        ProgressEntryDialog(
            habit = habit,
            progress = item.entry.progress,
            onConfirm = {
                entering = false
                onSetProgress(item, it)
            },
            onDismiss = { entering = false },
        )
    }
}

@Composable
private fun CheckMarker(checked: Boolean, modifier: Modifier = Modifier) {
    val status = HabitTheme.status
    Box(
        modifier =
            modifier
                .size(28.dp)
                .then(
                    if (checked) {
                        Modifier.background(status.passedContainer, CircleShape)
                    } else {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = status.passed,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun Stepper(
    habitName: String,
    progress: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onEnterValue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDecrement()
            },
            enabled = progress > 0,
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
        // The count doubles as the way into typing one in, for the days a few
        // taps would not carry it far enough (F3).
        Text(
            text = progress.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClickLabel = stringResource(R.string.habit_enter_value, habitName)) {
                        onEnterValue()
                    }.padding(horizontal = 8.dp, vertical = 4.dp),
        )
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

/** What the counter aims at, or for a habit to avoid what it must not pass (F11). */
@Composable
private fun targetLine(habit: Habit): String {
    val unit = habit.unit
    val limit = habit.polarity == Polarity.BAD
    return when {
        unit.isNullOrBlank() && limit -> stringResource(R.string.habit_entry_limit_plain, habit.target)
        unit.isNullOrBlank() -> stringResource(R.string.habit_entry_target_plain, habit.target)
        limit -> stringResource(R.string.habit_entry_limit, habit.target, unit)
        else -> stringResource(R.string.habit_entry_target, habit.target, unit)
    }
}

/**
 * Second line: the current value for counters, and for CHECK whatever moves the day
 * forward (points or required), so the rule of the day stays readable.
 */
@Composable
private fun habitSubtitle(item: HabitItem, goalType: GoalType): String {
    val habit = item.entry.habit
    if (habit.type != HabitType.CHECK) {
        // The count itself sits in the stepper, so the line states what to reach —
        // or, for a habit to avoid, what not to go beyond (F11).
        return targetLine(habit)
    }
    // A habit to avoid has no target to state, so the line carries the outcome
    // instead: it is the only thing that says what the mark on the right means.
    if (habit.polarity == Polarity.BAD) {
        val clean = if (item.entry.progress == 0) R.string.habit_entry_clean else R.string.habit_entry_slipped
        return stringResource(clean)
    }
    return when {
        goalType == GoalType.ALL_REQUIRED && habit.required -> {
            stringResource(R.string.habit_required)
        }

        goalType == GoalType.POINTS -> {
            pluralStringResource(R.plurals.habit_points, habit.points, habit.points)
        }

        else -> {
            ""
        }
    }
}
