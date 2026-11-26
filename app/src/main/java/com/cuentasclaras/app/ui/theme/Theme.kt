package com.cuentasclaras.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = EmeraldOnPrimary,
    secondary = TextPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = SurfaceSubtleLight,
    onSecondaryContainer = TextPrimaryLight,
    background = CanvasLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceSubtleLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
    error = ExpenseRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = EmeraldOnPrimary,
    secondary = SurfaceSubtleDark,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SurfaceSubtleDark,
    onSecondaryContainer = TextPrimaryDark,
    background = CanvasDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceSubtleDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

@Composable
fun CuentasClarasTheme(
    darkTheme: Boolean = false,
    largeText: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = createAppTypography(largeText = largeText),
        content = content
    )
}
