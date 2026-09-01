package com.example.habbittracker.ui.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habbittracker.AppContainer
import com.example.habbittracker.R
import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.ui.habit.HabitEditorScreen
import com.example.habbittracker.ui.habit.HabitEditorViewModel
import com.example.habbittracker.ui.habit.HabitListScreen
import com.example.habbittracker.ui.habit.HabitListViewModel
import com.example.habbittracker.ui.history.HistoryScreen
import com.example.habbittracker.ui.history.HistoryViewModel
import com.example.habbittracker.ui.settings.SettingsRoute
import com.example.habbittracker.ui.settings.SettingsViewModel
import com.example.habbittracker.ui.today.TodayRoute
import com.example.habbittracker.ui.today.TodayViewModel
import java.time.LocalDate

object Routes {
    const val HABITS = "habits"
    const val SETTINGS = "settings"
    const val HISTORY = "history"

    const val DATE_ARG = "date"

    /** Without a date the screen shows the running day. */
    const val TODAY = "today?$DATE_ARG={$DATE_ARG}"

    fun today(date: LocalDate? = null) = "today?$DATE_ARG=${date?.toString().orEmpty()}"

    const val HABIT_ID_ARG = "habitId"
    const val HABIT_EDITOR = "habit_editor?$HABIT_ID_ARG={$HABIT_ID_ARG}"

    /** Without an id a habit is created, with an id it is edited. */
    fun habitEditor(habitId: Long = NEW_HABIT_ID) = "habit_editor?$HABIT_ID_ARG=$habitId"
}

@Composable
fun HabitNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.today(),
        modifier = modifier,
    ) {
        composable(
            route = Routes.TODAY,
            arguments =
                listOf(
                    navArgument(Routes.DATE_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) { backStackEntry ->
            val shownDate =
                backStackEntry.arguments
                    ?.getString(Routes.DATE_ARG)
                    ?.takeIf { it.isNotBlank() }
                    ?.let(LocalDate::parse)
            val viewModel: TodayViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer {
                                TodayViewModel(container.habitRepository, shownDate = shownDate)
                            }
                        },
                )
            TodayRoute(
                viewModel = viewModel,
                onAddHabit = { navController.navigate(Routes.habitEditor()) },
                onEditHabit = { habitId -> navController.navigate(Routes.habitEditor(habitId)) },
                onOpenHabits = { navController.navigate(Routes.HABITS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer { HistoryViewModel(container.habitRepository) }
                        },
                )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            HistoryScreen(
                state = state,
                onPreviousMonth = viewModel::onPreviousMonth,
                onNextMonth = viewModel::onNextMonth,
                // Opening a past day is how values get backfilled (F3).
                onOpenDay = { date -> navController.navigate(Routes.today(date)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer {
                                SettingsViewModel(
                                    repository = container.settingsRepository,
                                    backupManager = container.backupManager,
                                )
                            }
                        },
                )
            SettingsRoute(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.HABITS) {
            val viewModel: HabitListViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer { HabitListViewModel(container.habitRepository) }
                        },
                )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            HabitListScreen(
                state = state,
                onAddHabit = { navController.navigate(Routes.habitEditor()) },
                onEditHabit = { habitId -> navController.navigate(Routes.habitEditor(habitId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.HABIT_EDITOR,
            arguments =
                listOf(
                    navArgument(Routes.HABIT_ID_ARG) {
                        type = NavType.LongType
                        defaultValue = NEW_HABIT_ID
                    },
                ),
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong(Routes.HABIT_ID_ARG) ?: NEW_HABIT_ID
            val viewModel: HabitEditorViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer { HabitEditorViewModel(container.habitRepository, habitId) }
                        },
                )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                // Every outcome closes the editor; the calling screen shows the new state.
                viewModel.events.collect { navController.popBackStack() }
            }

            HabitEditorScreen(
                state = state,
                onNameChange = viewModel::onNameChange,
                onTargetChange = viewModel::onTargetChange,
                onUnitChange = viewModel::onUnitChange,
                onNoteChange = viewModel::onNoteChange,
                onPointsChange = viewModel::onPointsChange,
                onRequiredChange = viewModel::onRequiredChange,
                onIconChange = viewModel::onIconChange,
                onSave = viewModel::onSave,
                onToggleArchived = viewModel::onToggleArchived,
                onDelete = viewModel::onDelete,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
