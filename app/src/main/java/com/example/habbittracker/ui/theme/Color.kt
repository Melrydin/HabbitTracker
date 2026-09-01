package com.example.habbittracker.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Color concept: a neutral base, one accent and one status tone. The accent is
 * Indigo, the "passed" marker a restrained green that never covers an area.
 *
 * Both can be swapped here in a single place without touching any other file.
 */

// Accent, Material Indigo
val AccentLight = Color(0xFF3F51B5)
val AccentContainerLight = Color(0xFFE3E5F6)
val OnAccentContainerLight = Color(0xFF1B2260)

val AccentDark = Color(0xFF9FA8DA)
val AccentContainerDark = Color(0xFF2E3676)
val OnAccentContainerDark = Color(0xFFDDE1F9)
val OnAccentDark = Color(0xFF141A4D)

// The "passed" status marker: a restrained green, used as a marker only and never
// to fill an area. Kept apart from the accent so that "done" reads at a glance.
val PassedLight = Color(0xFF2E7D4F)
val PassedContainerLight = Color(0xFFE6F4EC)
val OnPassedContainerLight = Color(0xFF0F4429)

val PassedDark = Color(0xFF7FD4A0)
val PassedContainerDark = Color(0xFF22523A)
val OnPassedContainerDark = Color(0xFFB5EDCA)

// The "open" status is deliberately grey rather than red, so nothing is judged
val OpenLight = Color(0xFF9DA0A9)
val OpenDark = Color(0xFF6E717A)

// Neutral base, light
val BackgroundLight = Color(0xFFFAFAFB)
val OnBackgroundLight = Color(0xFF17181B)
val SurfaceContainerLightC = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFF2F3F6)
val SurfaceVariantLight = Color(0xFFECEDF1)
val OnSurfaceVariantLight = Color(0xFF6A6D77)
val OutlineLight = Color(0xFF8B8E98)
val OutlineVariantLight = Color(0xFFE2E3E9)

// Neutral base, dark
val BackgroundDark = Color(0xFF111214)
val OnBackgroundDark = Color(0xFFE4E5E9)
val SurfaceContainerDarkC = Color(0xFF1A1B1F)
val SurfaceContainerHighDark = Color(0xFF212227)
val SurfaceVariantDark = Color(0xFF43464E)
val OnSurfaceVariantDark = Color(0xFF9EA1AB)
val OutlineDark = Color(0xFF71747D)
val OutlineVariantDark = Color(0xFF2D2F35)
