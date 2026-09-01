package com.example.habbittracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Minus, value, plus. Used wherever a small whole number is set by hand. */
@Composable
fun ValueStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    decreaseLabel: String,
    increaseLabel: String,
    modifier: Modifier = Modifier,
    range: IntRange = 1..99,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChange(value - 1) }, enabled = value > range.first) {
            Icon(Icons.Outlined.Remove, contentDescription = decreaseLabel)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = { onValueChange(value + 1) }, enabled = value < range.last) {
            Icon(Icons.Outlined.Add, contentDescription = increaseLabel)
        }
    }
}
