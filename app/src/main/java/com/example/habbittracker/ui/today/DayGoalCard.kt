package com.example.habbittracker.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.domain.DayGoalProgress
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.ui.components.ProgressTrack
import com.example.habbittracker.ui.components.StatusPill

/** Tagesziel mit Fortschrittsbalken und Statusmarker (F2). */
@Composable
fun DayGoalCard(goal: DayGoalProgress, modifier: Modifier = Modifier) {
    val label = goalLabel(goal)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.today_goal_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // Ohne erreichbares Ziel bleibt der Tag neutral, statt "offen" zu behaupten.
                if (!goal.isNeutral) StatusPill(passed = goal.passed)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(14.dp))
            ProgressTrack(
                fraction = goal.fraction,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.semantics { contentDescription = label },
            )
        }
    }
}

@Composable
private fun goalLabel(goal: DayGoalProgress): String =
    when {
        goal.isNeutral -> {
            stringResource(R.string.today_goal_none)
        }

        goal.goalType == GoalType.POINTS -> {
            stringResource(R.string.today_goal_points, goal.current, goal.threshold)
        }

        goal.goalType == GoalType.MIN_COUNT -> {
            stringResource(R.string.today_goal_min_count, goal.current, goal.threshold)
        }

        else -> {
            stringResource(R.string.today_goal_all_required, goal.current, goal.threshold)
        }
    }
