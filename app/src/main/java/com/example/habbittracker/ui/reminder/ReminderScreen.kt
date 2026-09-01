package com.example.habbittracker.ui.reminder

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.Reminder
import com.example.habbittracker.ui.components.BackTopAppBar
import com.example.habbittracker.ui.components.EmptyState
import com.example.habbittracker.ui.components.SettingRow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Manages the local reminders (F5).
 *
 * The notification permission is asked for when the user actually wants a
 * reminder, not on first launch: a request without a reason in front of it tends
 * to get refused.
 */
@Composable
fun ReminderScreen(
    state: ReminderListUiState,
    onSave: (Reminder) -> Unit,
    onDelete: (Long) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Reminder?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val denied = stringResource(R.string.reminders_permission_denied)
    var showDenied by remember { mutableStateOf(false) }

    val permission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> showDenied = !granted }

    LaunchedEffect(showDenied) {
        if (showDenied) {
            snackbarHostState.showSnackbar(denied)
            showDenied = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BackTopAppBar(title = stringResource(R.string.reminders_title), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    editing = Reminder(time = LocalTime.of(DEFAULT_HOUR, 0))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.reminders_add))
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
            items(items = state.reminders, key = { it.id }) { reminder ->
                SettingRow(
                    title = timeLabel(reminder.time),
                    subtitle = summary(reminder, state.habitName(reminder.habitId)),
                    onClick = { editing = reminder },
                ) {
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { onToggle(reminder, it) },
                    )
                }
            }
            if (state.loaded && state.reminders.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.reminders_empty_title),
                        body = stringResource(R.string.reminders_empty_body),
                    )
                }
            }
        }
    }

    editing?.let { reminder ->
        ReminderDialog(
            reminder = reminder,
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
private fun timeLabel(time: LocalTime): String {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    return time.format(formatter)
}

/** Second line: what it is about and which days it runs on. */
@Composable
private fun summary(reminder: Reminder, habitName: String?): String {
    val locale = Locale.getDefault()
    val separator = stringResource(R.string.today_subtitle_separator)
    val about = habitName ?: stringResource(R.string.reminders_general)
    if (reminder.daysOfWeek.isEmpty()) {
        return about + separator + stringResource(R.string.reminders_never)
    }
    val days =
        java.time.DayOfWeek.entries
            .filter { it.value in reminder.daysOfWeek }
            .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, locale) }
    return about + separator + days
}

private const val DEFAULT_HOUR = 20
