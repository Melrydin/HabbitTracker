package com.example.habbittracker.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Color concept: a neutral base plus exactly **one** accent, and that accent is
 * green. The "passed" status marker reuses the same tone to keep the look calm.
 *
 * The accent can be swapped here in a single place without touching any other file.
 */

// Accent
val AccentLight = Color(0xFF2E7D4F)
val AccentContainerLight = Color(0xFFE6F4EC)
val OnAccentContainerLight = Color(0xFF0F4429)

val AccentDark = Color(0xFF7FD4A0)
val AccentContainerDark = Color(0xFF22523A)
val OnAccentContainerDark = Color(0xFFB5EDCA)
val OnAccentDark = Color(0xFF0B3B22)

// The "passed" status marker. Now that the app is green throughout it shares the
// accent tone: it is told apart by shape (pill, check mark) and by the contrast to
// the neutral "open" state, not by a second shade of green.
val PassedLight = AccentLight
val PassedContainerLight = AccentContainerLight
val OnPassedContainerLight = OnAccentContainerLight

val PassedDark = AccentDark
val PassedContainerDark = AccentContainerDark
val OnPassedContainerDark = OnAccentContainerDark

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
