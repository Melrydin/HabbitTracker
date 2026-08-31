package com.example.habbittracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Statusfarben ausserhalb der Material-Rollen: "bestanden" ist ein Marker,
 * kein Flaechenton, deshalb bewusst nicht auf primary/tertiary gemappt.
 */
@Immutable
data class StatusColors(
    val passed: Color,
    val passedContainer: Color,
    val open: Color,
)

private val LightStatusColors = StatusColors(
    passed = PassedLight,
    passedContainer = PassedContainerLight,
    open = OpenLight,
)

private val DarkStatusColors = StatusColors(
    passed = PassedDark,
    passedContainer = PassedContainerDark,
    open = OpenDark,
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** Zugriff auf die App-eigenen Zusatzfarben: `HabitTheme.status.passed`. */
object HabitTheme {
    val status: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}

private val LightColorScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    primaryContainer = AccentContainerLight,
    onPrimaryContainer = OnAccentContainerLight,
    secondary = Color(0xFF5B5F6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EAEE),
    onSecondaryContainer = Color(0xFF2A2D35),
    tertiary = PassedLight,
    onTertiary = Color.White,
    tertiaryContainer = PassedContainerLight,
    onTertiaryContainer = Color(0xFF14351F),
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = SurfaceContainerLightC,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = Color(0xFF2E3033),
    inverseOnSurface = Color(0xFFF1F1F4),
    inversePrimary = AccentDark,
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    primaryContainer = AccentContainerDark,
    onPrimaryContainer = OnAccentContainerDark,
    secondary = Color(0xFFC3C6D2),
    onSecondary = Color(0xFF2C2F3A),
    secondaryContainer = Color(0xFF424653),
    onSecondaryContainer = Color(0xFFDFE1EA),
    tertiary = PassedDark,
    onTertiary = Color(0xFF0B3B22),
    tertiaryContainer = PassedContainerDark,
    onTertiaryContainer = Color(0xFFB5EDCA),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = Color(0xFF0B0C0E),
    surfaceContainerLow = Color(0xFF16171A),
    surfaceContainer = SurfaceContainerDarkC,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF292A30),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = Color(0xFFE4E5E9),
    inverseOnSurface = Color(0xFF2E3033),
    inversePrimary = AccentLight,
)

@Composable
fun HabbitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material-You bleibt aus, damit das Bild konsistent bleibt (F7: optional ab V2).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
