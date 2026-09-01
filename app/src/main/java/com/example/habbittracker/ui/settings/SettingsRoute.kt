package com.example.habbittracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habbittracker.R
import com.example.habbittracker.data.backup.BackupManager
import com.example.habbittracker.data.backup.BackupOutcome
import com.example.habbittracker.data.backup.BackupProblem

/**
 * Binds the settings screen to its view model and owns the two storage access
 * framework launchers. Picking a file is a UI concern, so it stays out of the
 * view model.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val messages = backupMessages()

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BackupManager.MIME_TYPE),
        ) { uri -> uri?.let(viewModel::onExport) }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> uri?.let(viewModel::onImport) }

    LaunchedEffect(viewModel) {
        viewModel.backupOutcomes.collect { outcome ->
            snackbarHostState.showSnackbar(messages(outcome))
        }
    }

    SettingsScreen(
        settings = settings,
        snackbarHostState = snackbarHostState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onGoalTypeChange = viewModel::onGoalTypeChange,
        onThresholdChange = viewModel::onThresholdChange,
        onExport = { exportLauncher.launch(viewModel.suggestedFileName()) },
        onImport = { importLauncher.launch(arrayOf(BackupManager.MIME_TYPE)) },
        onBack = onBack,
        modifier = modifier,
    )
}

/** Resolves an outcome to the message shown in the snackbar. */
@Composable
private fun backupMessages(): (BackupOutcome) -> String {
    val resources = LocalResources.current
    return { outcome ->
        when (outcome) {
            BackupOutcome.Exported -> {
                resources.getString(R.string.settings_backup_exported)
            }

            is BackupOutcome.Imported -> {
                resources.getString(R.string.settings_backup_imported, outcome.habits, outcome.days)
            }

            is BackupOutcome.Failed -> {
                resources.getString(outcome.problem.messageRes())
            }
        }
    }
}

@StringRes
private fun BackupProblem?.messageRes(): Int =
    when (this) {
        BackupProblem.NOT_A_BACKUP -> R.string.settings_backup_not_a_backup
        BackupProblem.NEWER_SCHEMA -> R.string.settings_backup_newer_schema
        BackupProblem.DAMAGED -> R.string.settings_backup_damaged
        null -> R.string.settings_backup_failed
    }
