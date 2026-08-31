package com.example.habbittracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Farbkonzept laut Featureliste: neutrale Basis, genau **ein** Akzent (Indigo),
 * dazu ein dezentes Grün ausschliesslich als Statusmarker. Mehr Farben gibt es nicht.
 *
 * Der Akzent laesst sich hier an einer Stelle tauschen (z. B. gegen Teal),
 * ohne dass eine andere Datei angefasst werden muss.
 */

// Akzent
val AccentLight = Color(0xFF4C56B8)
val AccentContainerLight = Color(0xFFE4E6FA)
val OnAccentContainerLight = Color(0xFF232A6B)

val AccentDark = Color(0xFFB6BDF5)
val AccentContainerDark = Color(0xFF333B85)
val OnAccentContainerDark = Color(0xFFE0E3FB)
val OnAccentDark = Color(0xFF1E2359)

// Statusmarker "bestanden" (dezentes Gruen, nie flaechig)
val PassedLight = Color(0xFF2E7D4F)
val PassedContainerLight = Color(0xFFE2F1E8)
val PassedDark = Color(0xFF7FD4A0)
val PassedContainerDark = Color(0xFF1C3A28)

// Status "offen": bewusst ein Grauton, kein Rot, um nicht zu werten
val OpenLight = Color(0xFF9DA0A9)
val OpenDark = Color(0xFF6E717A)

// Neutrale Basis, hell
val BackgroundLight = Color(0xFFFAFAFB)
val OnBackgroundLight = Color(0xFF17181B)
val SurfaceContainerLightC = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFF2F3F6)
val SurfaceVariantLight = Color(0xFFECEDF1)
val OnSurfaceVariantLight = Color(0xFF6A6D77)
val OutlineLight = Color(0xFF8B8E98)
val OutlineVariantLight = Color(0xFFE2E3E9)

// Neutrale Basis, dunkel
val BackgroundDark = Color(0xFF111214)
val OnBackgroundDark = Color(0xFFE4E5E9)
val SurfaceContainerDarkC = Color(0xFF1A1B1F)
val SurfaceContainerHighDark = Color(0xFF212227)
val SurfaceVariantDark = Color(0xFF43464E)
val OnSurfaceVariantDark = Color(0xFF9EA1AB)
val OutlineDark = Color(0xFF71747D)
val OutlineVariantDark = Color(0xFF2D2F35)
