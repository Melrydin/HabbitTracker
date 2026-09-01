package com.example.habbittracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode
import com.example.habbittracker.ui.components.BackTopAppBar
import com.example.habbittracker.ui.components.LabeledSection
import com.example.habbittracker.ui.components.SegmentedChoice
import com.example.habbittracker.ui.components.SettingRow
import com.example.habbittracker.ui.components.ValueStepper
import com.example.habbittracker.ui.theme.HabbitTrackerTheme

/** App settings (F7): theme and the goal new days start with. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    snackbarHostState: SnackbarHostState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGoalTypeChange: (GoalType) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmImport by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BackTopAppBar(title = stringResource(R.string.settings_title), onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LabeledSection(label = stringResource(R.string.settings_theme_label)) {
                SegmentedChoice(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    onSelect = onThemeModeChange,
                ) { stringResource(it.labelRes()) }
            }

            LabeledSection(
                label = stringResource(R.string.settings_goal_label),
                hint = stringResource(R.string.settings_goal_hint),
            ) {
                SegmentedChoice(
                    options = GoalType.entries,
                    selected = settings.defaultGoalType,
                    onSelect = onGoalTypeChange,
                ) { stringResource(it.labelRes()) }
                // Only the points rule has a bar of its own; the others come from
                // the habits of the day.
                if (settings.defaultGoalType == GoalType.POINTS) {
                    Spacer(Modifier.height(12.dp))
                    SettingRow(
                        title = stringResource(R.string.settings_goal_threshold_label),
                        subtitle =
                            stringResource(
                                R.string.settings_goal_threshold_points,
                                settings.defaultGoalThreshold,
                            ),
                    ) {
                        ValueStepper(
                            value = settings.defaultGoalThreshold,
                            onValueChange = onThresholdChange,
                            decreaseLabel = stringResource(R.string.settings_threshold_decrease),
                            increaseLabel = stringResource(R.string.settings_threshold_increase),
                            range = AppSettings.GOAL_THRESHOLD_MIN..AppSettings.GOAL_THRESHOLD_MAX,
                        )
                    }
                }
            }

            LabeledSection(label = stringResource(R.string.settings_backup_label)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingRow(
                        title = stringResource(R.string.settings_backup_export),
                        subtitle = stringResource(R.string.settings_backup_export_hint),
                        onClick = onExport,
                    )
                    SettingRow(
                        title = stringResource(R.string.settings_backup_import),
                        subtitle = stringResource(R.string.settings_backup_import_hint),
                        onClick = { confirmImport = true },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.settings_backup_import_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmImport = false
                        onImport()
                    },
                ) {
                    Text(stringResource(R.string.settings_backup_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) {
                    Text(stringResource(R.string.habit_editor_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

@Preview(name = "Settings", showBackground = true)
@Composable
private fun SettingsPreview() {
    HabbitTrackerTheme {
        SettingsScreen(
            settings = AppSettings(),
            snackbarHostState = remember { SnackbarHostState() },
            onThemeModeChange = {},
            onGoalTypeChange = {},
            onThresholdChange = {},
            onExport = {},
            onImport = {},
            onBack = {},
        )
    }
}
