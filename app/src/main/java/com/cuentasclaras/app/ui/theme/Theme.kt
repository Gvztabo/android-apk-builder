package com.cuentasclaras.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = SlateOnContainer,
    background = CanvasLight,
    onBackground = Color(0xFF101815),
    surface = SurfaceWhite,
    onSurface = Color(0xFF101815),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF414E49),
    outline = OutlineLight,
    error = ExpenseRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF002117),
    primaryContainer = Color(0xFF0A4A3A),
    onPrimaryContainer = Color(0xFFD5F2E8),
    secondary = Color(0xFF8AB5A6),
    onSecondary = Color(0xFF0B251E),
    secondaryContainer = Color(0xFF1F3830),
    onSecondaryContainer = Color(0xFFCDE8DE),
    background = Color(0xFF0B110F),
    onBackground = Color(0xFFE6EFEC),
    surface = Color(0xFF141D1A),
    onSurface = Color(0xFFE6EFEC),
    surfaceVariant = Color(0xFF1E2A26),
    onSurfaceVariant = Color(0xFFAABBB4),
    outline = Color(0xFF2F3F39),
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
