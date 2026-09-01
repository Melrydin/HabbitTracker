package com.example.habbittracker.ui.today

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.Habit

/**
 * Types a count in directly, for the days a counter moved further than a few taps
 * would comfortably carry it (F3).
 */
@Composable
fun ProgressEntryDialog(
    habit: Habit,
    progress: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(progress.toString()) }
    val entered = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.habit_entry_title, habit.name)) },
        text = {
            OutlinedTextField(
                value = text,
                // Digits only, so a stray sign or separator cannot reach the parser.
                onValueChange = { text = it.filter(Char::isDigit).take(MAX_DIGITS) },
                label = { Text(stringResource(R.string.habit_entry_label)) },
                supportingText = { Text(targetLabel(habit)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { entered?.let(onConfirm) }, enabled = entered != null) {
                Text(stringResource(R.string.habit_entry_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_editor_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun targetLabel(habit: Habit): String {
    val unit = habit.unit
    return if (unit.isNullOrBlank()) {
        stringResource(R.string.habit_entry_target_plain, habit.target)
    } else {
        stringResource(R.string.habit_entry_target, habit.target, unit)
    }
}

private const val MAX_DIGITS = 4
