package com.cuentasclaras.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun createAppTypography(largeText: Boolean): Typography {
    val scale = if (largeText) 1.12f else 1.0f
    return Typography(
        headlineMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp * scale,
            lineHeight = 32.sp * scale,
            fontFeatureSettings = "tnum"
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp * scale,
            lineHeight = 26.sp * scale,
            fontFeatureSettings = "tnum"
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp * scale,
            lineHeight = 24.sp * scale
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 15.5.sp * scale,
            lineHeight = 22.sp * scale
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp * scale,
            lineHeight = 20.sp * scale
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp * scale,
            lineHeight = 20.sp * scale
        ),
        labelMedium = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp * scale,
            lineHeight = 18.sp * scale
        )
    )
}

val AppTypography = createAppTypography(largeText = false)
