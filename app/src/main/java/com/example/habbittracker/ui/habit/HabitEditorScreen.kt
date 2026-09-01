package com.example.habbittracker.ui.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.ui.components.BackTopAppBar
import com.example.habbittracker.ui.components.LabeledSection
import com.example.habbittracker.ui.components.SegmentedChoice
import com.example.habbittracker.ui.components.SettingRow
import com.example.habbittracker.ui.components.ValueStepper
import com.example.habbittracker.ui.icons.HabitIcons
import com.example.habbittracker.ui.theme.HabbitTrackerTheme

/**
 * Creating and editing a habit (F1). Saving stays disabled while the form is
 * incomplete, rather than complaining once the user taps save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    state: HabitEditorUiState,
    onNameChange: (String) -> Unit,
    onTypeChange: (HabitType) -> Unit,
    onTargetChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPointsChange: (Int) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
    onIconChange: (String) -> Unit,
    onSave: () -> Unit,
    onToggleArchived: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopAppBar(
                title =
                    stringResource(
                        if (form.isNew) R.string.habit_editor_title_new else R.string.habit_editor_title_edit,
                    ),
                onBack = onBack,
            ) {
                TextButton(onClick = onSave, enabled = form.canSave && !state.loading) {
                    Text(stringResource(R.string.habit_editor_save))
                }
            }
        },
    ) { innerPadding ->
        if (state.loading) return@Scaffold

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            NameField(form = form, onNameChange = onNameChange)

            LabeledSection(label = stringResource(R.string.habit_editor_icon_label)) {
                IconPicker(selected = form.icon, onIconChange = onIconChange)
            }

            LabeledSection(label = stringResource(R.string.habit_editor_type_label)) {
                SegmentedChoice(
                    options = HabitType.entries,
                    selected = form.type,
                    onSelect = onTypeChange,
                ) { stringResource(it.labelRes()) }
            }

            if (form.showsTargetAndUnit) {
                TargetAndUnitFields(
                    form = form,
                    onTargetChange = onTargetChange,
                    onUnitChange = onUnitChange,
                )
            }

            PointsRow(points = form.points, onPointsChange = onPointsChange)

            RequiredRow(required = form.required, onRequiredChange = onRequiredChange)

            NoteField(note = form.note, onNoteChange = onNoteChange)

            if (!form.isNew) {
                DangerZone(
                    archived = form.archived,
                    onToggleArchived = onToggleArchived,
                    onDelete = { showDeleteDialog = true },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.habit_editor_delete_title)) },
            text = { Text(stringResource(R.string.habit_editor_delete_message, form.name.trim())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.habit_editor_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.habit_editor_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

@Composable
private fun NameField(
    form: HabitFormState,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasError = form.nameError != null && form.name.isNotEmpty()
    OutlinedTextField(
        value = form.name,
        onValueChange = onNameChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.habit_editor_name_label)) },
        singleLine = true,
        isError = hasError,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        supportingText = {
            Text(
                text =
                    if (hasError) {
                        stringResource(R.string.habit_editor_name_error)
                    } else {
                        stringResource(
                            R.string.habit_editor_name_counter,
                            form.name.length,
                            com.example.habbittracker.domain.model.Habit.NAME_MAX_LENGTH,
                        )
                    },
                style = MaterialTheme.typography.labelMedium,
            )
        },
    )
}

/** A free description of the habit (F1). Multi-line, because a note is prose. */
@Composable
private fun NoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.habit_editor_note_label)) },
        placeholder = { Text(stringResource(R.string.habit_editor_note_placeholder)) },
        minLines = 3,
        maxLines = 6,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun IconPicker(
    selected: String,
    onIconChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HabitIcons.catalog.forEach { (name, vector) ->
                val isSelected = name == selected
                Surface(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onIconChange(name) },
                            ),
                    shape = CircleShape,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = vector,
                            // The selection is conveyed by the button state, not by the icon.
                            contentDescription = name,
                            tint =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetAndUnitFields(
    form: HabitFormState,
    onTargetChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = form.target,
            onValueChange = onTargetChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.habit_editor_target_label)) },
            singleLine = true,
            isError = form.targetError != null,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
            supportingText =
                form.targetError?.let { error ->
                    {
                        Text(
                            text =
                                when (error) {
                                    HabitFormError.TARGET_REQUIRED -> {
                                        stringResource(R.string.habit_editor_target_error_required)
                                    }

                                    HabitFormError.TARGET_TOO_SMALL -> {
                                        stringResource(R.string.habit_editor_target_error_min)
                                    }

                                    else -> {
                                        stringResource(
                                            R.string.habit_editor_target_error_max,
                                            HabitFormState.TARGET_MAX,
                                        )
                                    }
                                },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
        )
        OutlinedTextField(
            value = form.unit,
            onValueChange = onUnitChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.habit_editor_unit_label)) },
            placeholder = { Text(stringResource(R.string.habit_editor_unit_placeholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
}

@Composable
private fun PointsRow(
    points: Int,
    onPointsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        modifier = modifier,
        title = stringResource(R.string.habit_editor_points_label),
        subtitle = stringResource(R.string.habit_editor_points_hint),
    ) {
        ValueStepper(
            value = points,
            onValueChange = onPointsChange,
            decreaseLabel = stringResource(R.string.habit_editor_points_decrease),
            increaseLabel = stringResource(R.string.habit_editor_points_increase),
            range = HabitFormState.POINTS_MIN..HabitFormState.POINTS_MAX,
        )
    }
}

@Composable
private fun RequiredRow(
    required: Boolean,
    onRequiredChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        modifier = modifier,
        title = stringResource(R.string.habit_editor_required_label),
        subtitle = stringResource(R.string.habit_editor_required_hint),
    ) {
        Switch(checked = required, onCheckedChange = onRequiredChange)
    }
}

@Composable
private fun DangerZone(
    archived: Boolean,
    onToggleArchived: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            onClick = onToggleArchived,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            stringResource(
                                if (archived) R.string.habit_editor_unarchive else R.string.habit_editor_archive,
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.habit_editor_archive_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.Start)) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.habit_editor_delete),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// --- Previews ---------------------------------------------------------------

@Preview(name = "Editor, new", showBackground = true)
@Composable
private fun HabitEditorNewPreview() {
    HabbitTrackerTheme {
        HabitEditorScreen(
            state =
                HabitEditorUiState(
                    HabitFormState(
                        name = "Drink water",
                        type = HabitType.COUNTER,
                        target = "8",
                        unit = "glasses",
                        points = 2,
                        icon = "water_drop",
                    ),
                ),
            onNameChange = {},
            onTypeChange = {},
            onTargetChange = {},
            onUnitChange = {},
            onNoteChange = {},
            onPointsChange = {},
            onRequiredChange = {},
            onIconChange = {},
            onSave = {},
            onToggleArchived = {},
            onDelete = {},
            onBack = {},
        )
    }
}

@Preview(name = "Editor, edit, dark", showBackground = true)
@Composable
private fun HabitEditorEditPreview() {
    HabbitTrackerTheme(darkTheme = true) {
        HabitEditorScreen(
            state =
                HabitEditorUiState(
                    HabitFormState(
                        id = 2,
                        name = "Exercise",
                        type = HabitType.CHECK,
                        points = 3,
                        required = true,
                        icon = "directions_run",
                    ),
                ),
            onNameChange = {},
            onTypeChange = {},
            onTargetChange = {},
            onUnitChange = {},
            onNoteChange = {},
            onPointsChange = {},
            onRequiredChange = {},
            onIconChange = {},
            onSave = {},
            onToggleArchived = {},
            onDelete = {},
            onBack = {},
        )
    }
}
