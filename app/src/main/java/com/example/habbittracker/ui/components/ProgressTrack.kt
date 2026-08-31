package com.example.habbittracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A flat progress bar. Deliberately drawn by hand instead of using
 * `LinearProgressIndicator`, so that its height, radius and calmness stay
 * independent of the Material defaults.
 *
 * The bar carries no semantics: the accompanying text already states how far
 * along the day is.
 */
@Composable
fun ProgressTrack(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        label = "progress",
    )
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clearAndSetSemantics { },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        val filled = size.width * animated
        if (filled > 0f) {
            drawRoundRect(
                color = color,
                // At least one circle wide, so even a small value stays visible.
                size = Size(filled.coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }
    }
}
