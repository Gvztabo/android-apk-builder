package com.cuentasclaras.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

        // Solid Primary Emerald Squircle Background
        drawRoundRect(
            color = EmeraldPrimary,
            topLeft = Offset(2f * s, 2f * s),
            size = Size(96f * s, 96f * s),
            cornerRadius = CornerRadius(24f * s, 24f * s)
        )

        // Centered White Calculator (Base Layer)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(28f * s, 24f * s),
            size = Size(44f * s, 52f * s),
            cornerRadius = CornerRadius(7.5f * s, 7.5f * s),
            style = Stroke(width = 4.2f * s, join = StrokeJoin.Round)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(34.5f * s, 30.5f * s),
            size = Size(31f * s, 8.5f * s),
            cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
        )

        val btnCols = listOf(34.5f, 60.3f)
        val btnRows = listOf(45.5f, 58f)
        for (ry in btnRows) {
            for (cx in btnCols) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx * s, ry * s),
                    size = Size(5.2f * s, 5.2f * s),
                    cornerRadius = CornerRadius(1.5f * s, 1.5f * s)
                )
            }
        }

        // Centered Emerald Halo Separation
        drawCircle(
            color = EmeraldPrimary,
            radius = 12.2f * s,
            center = Offset(50f * s, 56f * s)
        )

        // Centered White Reload Symbol (Top Layer)
        val arcTopLeft = Offset((50f - 8.6f) * s, (56f - 8.6f) * s)
        val arcSize = Size(17.2f * s, 17.2f * s)
        drawArc(
            color = Color.White,
            startAngle = 205f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 3.4f * s, cap = StrokeCap.Round)
        )
        val topArrow = Path().apply {
            moveTo(59.6f * s, 54.6f * s)
            lineTo(59.6f * s, 47.8f * s)
            lineTo(54.0f * s, 52.1f * s)
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
            style = Stroke(width = 3.4f * s, cap = StrokeCap.Round)
        )
        val bottomArrow = Path().apply {
            moveTo(40.4f * s, 57.4f * s)
            lineTo(40.4f * s, 64.2f * s)
            lineTo(46.0f * s, 59.9f * s)
            close()
        }
        drawPath(path = bottomArrow, color = Color.White)
    }
}
