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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode
import com.example.habbittracker.ui.theme.HabbitTrackerTheme

/** App settings (F7): theme and the goal new days start with. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGoalTypeChange: (GoalType) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.habit_editor_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
            )
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
            SettingsSection(label = stringResource(R.string.settings_theme_label)) {
                ThemeSelector(selected = settings.themeMode, onThemeModeChange = onThemeModeChange)
            }

            SettingsSection(
                label = stringResource(R.string.settings_goal_label),
                hint = stringResource(R.string.settings_goal_hint),
            ) {
                GoalTypeSelector(selected = settings.defaultGoalType, onGoalTypeChange = onGoalTypeChange)
                Spacer(Modifier.height(12.dp))
                ThresholdRow(settings = settings, onThresholdChange = onThresholdChange)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(
    selected: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onThemeModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
            ) {
                Text(stringResource(mode.labelRes()))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalTypeSelector(
    selected: GoalType,
    onGoalTypeChange: (GoalType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = GoalType.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        types.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onGoalTypeChange(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
            ) {
                Text(stringResource(type.labelRes()))
            }
        }
    }
}

/**
 * ALL_REQUIRED derives its threshold from the required habits of the day, so the
 * stepper would have nothing to set and is replaced by an explanation.
 */
@Composable
private fun ThresholdRow(
    settings: AppSettings,
    onThresholdChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val threshold = settings.defaultGoalThreshold
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_goal_threshold_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = thresholdSummary(settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (settings.defaultGoalType != GoalType.ALL_REQUIRED) {
                Stepper(threshold = threshold, onThresholdChange = onThresholdChange)
            }
        }
    }
}

@Composable
private fun Stepper(
    threshold: Int,
    onThresholdChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onThresholdChange(threshold - 1) },
            enabled = threshold > AppSettings.GOAL_THRESHOLD_MIN,
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.settings_threshold_decrease),
            )
        }
        Text(
            text = threshold.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = { onThresholdChange(threshold + 1) },
            enabled = threshold < AppSettings.GOAL_THRESHOLD_MAX,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.settings_threshold_increase),
            )
        }
    }
}

@Composable
private fun thresholdSummary(settings: AppSettings): String =
    when (settings.defaultGoalType) {
        GoalType.ALL_REQUIRED -> {
            stringResource(R.string.settings_goal_threshold_unused)
        }

        GoalType.MIN_COUNT -> {
            stringResource(R.string.settings_goal_threshold_habits, settings.defaultGoalThreshold)
        }

        GoalType.POINTS -> {
            stringResource(R.string.settings_goal_threshold_points, settings.defaultGoalThreshold)
        }
    }

@Composable
private fun SettingsSection(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        content()
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Preview(name = "Settings", showBackground = true)
@Composable
private fun SettingsPreview() {
    HabbitTrackerTheme {
        SettingsScreen(
            settings = AppSettings(),
            onThemeModeChange = {},
            onGoalTypeChange = {},
            onThresholdChange = {},
            onBack = {},
        )
    }
}
