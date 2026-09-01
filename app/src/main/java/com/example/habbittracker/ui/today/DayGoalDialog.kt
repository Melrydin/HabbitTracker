package com.example.habbittracker.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.ui.components.LabeledSection
import com.example.habbittracker.ui.components.SegmentedChoice
import com.example.habbittracker.ui.components.ValueStepper
import com.example.habbittracker.ui.settings.labelRes

/**
 * Gives one day a goal of its own, or hands it back to the default (F2).
 *
 * Only the points rule carries a threshold; the other two take theirs from the
 * habits of the day, so there is nothing to set for them here either.
 */
@Composable
fun DayGoalDialog(
    goalType: GoalType,
    threshold: Int,
    overridden: Boolean,
    onConfirm: (GoalType, Int) -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    var rule by remember { mutableStateOf(goalType) }
    var points by remember { mutableStateOf(threshold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.today_goal_dialog_title)) },
        text = {
            Column {
                SegmentedChoice(options = GoalType.entries, selected = rule, onSelect = { rule = it }) {
                    stringResource(it.labelRes())
                }
                if (rule == GoalType.POINTS) {
                    Spacer(Modifier.height(12.dp))
                    LabeledSection(label = stringResource(R.string.settings_goal_threshold_label)) {
                        ValueStepper(
                            value = points,
                            onValueChange = { points = it },
                            decreaseLabel = stringResource(R.string.settings_threshold_decrease),
                            increaseLabel = stringResource(R.string.settings_threshold_increase),
                            range = AppSettings.GOAL_THRESHOLD_MIN..AppSettings.GOAL_THRESHOLD_MAX,
                        )
                    }
                }
                if (overridden) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onUseDefault) {
                        Text(stringResource(R.string.today_goal_dialog_default))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rule, points) }) {
                Text(stringResource(R.string.today_goal_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_editor_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}
