package com.cuentasclaras.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GroupedCardHeader(
    title: String,
    trailing: String,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(surfaceColor)
            .drawWithCache {
                val strokePx = 1.dp.toPx()
                val half = strokePx / 2f
                val r = 24.dp.toPx().coerceAtMost((size.height - strokePx).coerceAtLeast(1f))
                val path = Path().apply {
                    moveTo(half, size.height)
                    lineTo(half, half + r)
                    arcTo(
                        rect = Rect(half, half, half + 2f * r, half + 2f * r),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(size.width - half - r, half)
                    arcTo(
                        rect = Rect(size.width - half - 2f * r, half, size.width - half, half + 2f * r),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(size.width - half, size.height)
                }
                val stroke = Stroke(width = strokePx)
                onDrawBehind {
                    drawPath(path = path, color = outlineColor, style = stroke)
                    drawLine(
                        color = outlineColor,
                        start = Offset(half, size.height - half),
                        end = Offset(size.width - half, size.height - half),
                        strokeWidth = strokePx
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GroupedCardItemSlice(
    isFirst: Boolean,
    isLast: Boolean,
    hasHeader: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val dividerColor = outlineColor.copy(alpha = 0.65f)

    val topRounded = !hasHeader && isFirst
    val bottomRounded = isLast

    if (topRounded && bottomRounded) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, outlineColor),
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
        return
    }

    val clipShape = when {
        topRounded -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        bottomRounded -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(0.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(clipShape)
            .background(surfaceColor)
            .drawWithCache {
                val strokePx = 1.dp.toPx()
                val half = strokePx / 2f
                val r = 24.dp.toPx().coerceAtMost((size.height - strokePx).coerceAtLeast(1f))
                val stroke = Stroke(width = strokePx)

                val topPath = if (topRounded) {
                    Path().apply {
                        moveTo(half, size.height)
                        lineTo(half, half + r)
                        arcTo(
                            rect = Rect(half, half, half + 2f * r, half + 2f * r),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width - half - r, half)
                        arcTo(
                            rect = Rect(size.width - half - 2f * r, half, size.width - half, half + 2f * r),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width - half, size.height)
                    }
                } else {
                    null
                }

                val bottomPath = if (bottomRounded) {
                    Path().apply {
                        moveTo(size.width - half, 0f)
                        lineTo(size.width - half, size.height - half - r)
                        arcTo(
                            rect = Rect(
                                size.width - half - 2f * r,
                                size.height - half - 2f * r,
                                size.width - half,
                                size.height - half
                            ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(half + r, size.height - half)
                        arcTo(
                            rect = Rect(
                                half,
                                size.height - half - 2f * r,
                                half + 2f * r,
                                size.height - half
                            ),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(half, 0f)
                    }
                } else {
                    null
                }

                onDrawWithContent {
                    drawContent()
                    when {
                        topRounded && topPath != null -> {
                            drawPath(path = topPath, color = outlineColor, style = stroke)
                            drawLine(
                                color = dividerColor,
                                start = Offset(half, size.height - half),
                                end = Offset(size.width - half, size.height - half),
                                strokeWidth = strokePx
                            )
                        }
                        bottomRounded && bottomPath != null -> {
                            drawPath(path = bottomPath, color = outlineColor, style = stroke)
                        }
                        else -> {
                            drawLine(
                                color = outlineColor,
                                start = Offset(half, 0f),
                                end = Offset(half, size.height),
                                strokeWidth = strokePx
                            )
                            drawLine(
                                color = outlineColor,
                                start = Offset(size.width - half, 0f),
                                end = Offset(size.width - half, size.height),
                                strokeWidth = strokePx
                            )
                            drawLine(
                                color = dividerColor,
                                start = Offset(half, size.height - half),
                                end = Offset(size.width - half, size.height - half),
                                strokeWidth = strokePx
                            )
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}
