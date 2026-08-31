package com.example.habbittracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Farbkonzept: neutrale Basis plus genau **ein** Akzent, und der ist Grün.
 * Der Statusmarker "bestanden" benutzt denselben Ton, damit das Bild ruhig bleibt.
 *
 * Der Akzent laesst sich hier an einer Stelle tauschen, ohne dass eine andere
 * Datei angefasst werden muss.
 */

// Akzent
val AccentLight = Color(0xFF2E7D4F)
val AccentContainerLight = Color(0xFFE6F4EC)
val OnAccentContainerLight = Color(0xFF0F4429)

val AccentDark = Color(0xFF7FD4A0)
val AccentContainerDark = Color(0xFF22523A)
val OnAccentContainerDark = Color(0xFFB5EDCA)
val OnAccentDark = Color(0xFF0B3B22)

// Statusmarker "bestanden". Seit die App durchgehend gruen ist, teilt er sich
// den Ton mit dem Akzent: unterschieden wird ueber Form (Pille, Haken) und
// ueber den Kontrast zum neutralen "offen", nicht ueber einen zweiten Gruenton.
val PassedLight = AccentLight
val PassedContainerLight = AccentContainerLight
val OnPassedContainerLight = OnAccentContainerLight

val PassedDark = AccentDark
val PassedContainerDark = AccentContainerDark
val OnPassedContainerDark = OnAccentContainerDark

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
