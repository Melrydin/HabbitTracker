package com.example.habbittracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.habbittracker.R
import com.example.habbittracker.ui.theme.HabitTheme

/**
 * Statusmarker fuer den Tag. "bestanden" ist der einzige Ort, an dem Gruen
 * auftaucht, "offen" bleibt neutral grau, damit nichts gewertet wird.
 */
@Composable
fun StatusPill(passed: Boolean, modifier: Modifier = Modifier) {
    val status = HabitTheme.status
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = if (passed) status.passedContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (passed) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = status.passed,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text =
                    stringResource(
                        if (passed) R.string.today_status_passed else R.string.today_status_open,
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = if (passed) status.onPassedContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
