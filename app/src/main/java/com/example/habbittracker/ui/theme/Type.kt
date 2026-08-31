package com.example.habbittracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Drei Stufen laut Featureliste: Titel, Fliesstext, Label.
 * System-Schrift (Roboto), keine zusaetzlichen Fonts.
 */
private val Sans = FontFamily.Default

val Typography =
    Typography(
        // Titel
        headlineSmall =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.2).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
        // Fliesstext
        bodyLarge =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        // Label
        labelLarge =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
    )
