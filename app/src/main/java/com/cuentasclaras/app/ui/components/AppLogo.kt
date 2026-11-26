package com.cuentasclaras.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cuentasclaras.app.ui.theme.EmeraldPrimary

@Composable
fun AppLogo(modifier: Modifier = Modifier.size(32.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 100f
        val emeraldBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF137F64),
                EmeraldPrimary,
                Color(0xFF0B5643)
            ),
            start = Offset(6f * s, 6f * s),
            end = Offset(94f * s, 94f * s)
        )

        // Primary Emerald Squircle Background
        drawRoundRect(
            brush = emeraldBrush,
            topLeft = Offset(2f * s, 2f * s),
            size = Size(96f * s, 96f * s),
            cornerRadius = CornerRadius(24f * s, 24f * s)
        )

        // White Calculator (Base Layer, centered & compact inside emerald field)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(25f * s, 24f * s),
            size = Size(38f * s, 43f * s),
            cornerRadius = CornerRadius(7f * s, 7f * s),
            style = Stroke(width = 4.2f * s, join = StrokeJoin.Round)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(31.5f * s, 30.5f * s),
            size = Size(25f * s, 8.5f * s),
            cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
        )

        val btnCols = listOf(31.5f, 41f, 50.5f)
        val btnRows = listOf(43.5f, 53.5f)
        for (ry in btnRows) {
            for (cx in btnCols) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx * s, ry * s),
                    size = Size(6f * s, 6f * s),
                    cornerRadius = CornerRadius(1.6f * s, 1.6f * s)
                )
            }
        }

        // Seamless Emerald Halo Separation
        drawCircle(
            brush = emeraldBrush,
            radius = 16f * s,
            center = Offset(61f * s, 62f * s)
        )

        // White Reload Symbol (Top Layer)
        val arcTopLeft = Offset((61f - 10.2f) * s, (62f - 10.2f) * s)
        val arcSize = Size(20.4f * s, 20.4f * s)
        drawArc(
            color = Color.White,
            startAngle = 205f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 3.8f * s, cap = StrokeCap.Round)
        )
        val topArrow = Path().apply {
            moveTo(72.2f * s, 60.1f * s)
            lineTo(72.2f * s, 52.7f * s)
            lineTo(66.0f * s, 57.4f * s)
            close()
        }
        drawPath(path = topArrow, color = Color.White)

        drawArc(
            color = Color.White,
            startAngle = 25f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 3.8f * s, cap = StrokeCap.Round)
        )
        val bottomArrow = Path().apply {
            moveTo(49.8f * s, 63.9f * s)
            lineTo(49.8f * s, 71.3f * s)
            lineTo(56.0f * s, 66.6f * s)
            close()
        }
        drawPath(path = bottomArrow, color = Color.White)
    }
}
