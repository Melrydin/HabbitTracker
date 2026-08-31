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
 * Flacher Fortschrittsbalken. Bewusst selbst gezeichnet statt
 * `LinearProgressIndicator`, damit Hoehe, Radius und Ruhe des Balkens
 * unabhaengig von Material-Defaults bleiben.
 *
 * Der Balken traegt keine Semantik: der begleitende Text sagt schon,
 * wie weit der Tag ist.
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
                // Mindestens Kreisbreite, damit auch ein kleiner Wert sichtbar bleibt.
                size = Size(filled.coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }
    }
}
