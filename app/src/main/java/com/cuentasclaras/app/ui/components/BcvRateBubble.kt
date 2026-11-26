package com.cuentasclaras.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentasclaras.app.data.remote.BcvRateHistoryEntry
import com.cuentasclaras.app.data.remote.BcvRateInfo
import com.cuentasclaras.app.data.remote.BcvRateService
import com.cuentasclaras.app.data.remote.BcvTrendDirection
import com.cuentasclaras.app.ui.theme.AdjustAmber
import com.cuentasclaras.app.ui.theme.ExpenseRed
import com.cuentasclaras.app.ui.theme.IncomeGreen
import com.cuentasclaras.app.util.CurrencyFormatter
import java.util.Locale

@Composable
fun BcvRateBubble(
    bcvRate: BcvRateInfo?,
    history: List<BcvRateHistoryEntry>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isManualBcv: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val trend = remember(history, isManualBcv) {
        if (isManualBcv) BcvTrendDirection.SAME else BcvRateService.computeTrend(history)
    }

    Surface(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isManualBcv) AdjustAmber.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .padding(end = 10.dp)
            .heightIn(min = 38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val label = bcvRate?.let {
                val prefix = if (isManualBcv) "Manual" else "BCV"
                String.format(Locale.US, "$prefix Bs. %.2f", it.usdRate)
            } ?: if (isManualBcv) "Tasa manual" else "Sincronizar BCV"
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (bcvRate != null && !isManualBcv) {
                when (trend) {
                    BcvTrendDirection.UP -> Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Subió",
                        tint = IncomeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    BcvTrendDirection.DOWN -> Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Bajó",
                        tint = ExpenseRed,
                        modifier = Modifier.size(16.dp)
                    )
                    BcvTrendDirection.SAME -> Unit
                }
            }

            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }

    if (showDialog) {
        BcvHistoryDialog(
            bcvRate = bcvRate,
            history = history,
            trend = trend,
            isManualBcv = isManualBcv,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun BcvHistoryDialog(
    bcvRate: BcvRateInfo?,
    history: List<BcvRateHistoryEntry>,
    trend: BcvTrendDirection,
    isManualBcv: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayHistory = remember(history, isManualBcv) {
        if (isManualBcv) history else history.drop(1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Tasa Oficial BCV",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (isManualBcv) {
                        "Modo manual activo (sin actualización automática)"
                    } else {
                        "Se actualiza automáticamente cada día"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (bcvRate != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isManualBcv) "Precio manual activo" else "Precio actual",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format(Locale.US, "Bs. %.4f", bcvRate.usdRate),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isManualBcv) {
                                        when (trend) {
                                            BcvTrendDirection.UP -> {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowUpward,
                                                    contentDescription = "Subió",
                                                    tint = IncomeGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            BcvTrendDirection.DOWN -> {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowDownward,
                                                    contentDescription = "Bajó",
                                                    tint = ExpenseRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            BcvTrendDirection.SAME -> Unit
                                        }
                                    }
                                }
                            }

                            if (!isManualBcv && history.size >= 2) {
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Anterior → Nuevo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "Bs. %.2f → Bs. %.2f",
                                            history[1].usdRate,
                                            history[0].usdRate
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (!isManualBcv && bcvRate.fechaValor.isNotBlank()) {
                                Text(
                                    text = "Fecha valor: ${bcvRate.fechaValor}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (isManualBcv) {
                            "Modo manual activo sin valor configurado."
                        } else {
                            "Tasa BCV no disponible aún."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (isManualBcv) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AdjustAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AdjustAmber.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Estás usando un valor manual. Puedes reintentar obtener la tasa oficial BCV cuando tengas conexión.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = onRefresh,
                                enabled = !isRefreshing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AdjustAmber,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRefreshing) "Reintentando..." else "Reintentar actualización",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                Text(
                    text = if (isManualBcv) {
                        "Último valor capturado por el teléfono"
                    } else {
                        "Historial de cambios"
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                if (displayHistory.isEmpty()) {
                    Text(
                        text = if (isManualBcv) {
                            "Aún no se ha capturado ninguna tasa BCV desde el servidor."
                        } else {
                            "Sin cambios anteriores registrados."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        displayHistory.forEachIndexed { index, entry ->
                            val prev = displayHistory.getOrNull(index + 1)
                            val diff = if (prev != null) entry.usdRate - prev.usdRate else 0.0
                            val entryTrend = when {
                                prev == null -> BcvTrendDirection.SAME
                                diff > 0.0001 -> BcvTrendDirection.UP
                                diff < -0.0001 -> BcvTrendDirection.DOWN
                                else -> BcvTrendDirection.SAME
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = String.format(Locale.US, "Bs. %.4f", entry.usdRate),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val rawDateLabel = entry.fechaValor.ifBlank {
                                        if (entry.recordedAtMillis > 0L) {
                                            CurrencyFormatter.formatDate(entry.recordedAtMillis)
                                        } else {
                                            ""
                                        }
                                    }
                                    val dateLabel = if (isManualBcv && index == 0) {
                                        if (rawDateLabel.isNotBlank()) "Último capturado · $rawDateLabel" else "Último capturado"
                                    } else {
                                        rawDateLabel
                                    }
                                    if (dateLabel.isNotBlank()) {
                                        Text(
                                            text = dateLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                when (entryTrend) {
                                    BcvTrendDirection.UP -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowUpward,
                                            contentDescription = "Subió",
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "+%.2f", diff),
                                            color = IncomeGreen,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    BcvTrendDirection.DOWN -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDownward,
                                            contentDescription = "Bajó",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f", diff),
                                            color = ExpenseRed,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    BcvTrendDirection.SAME -> Unit
                                }
                            }
                            if (index < displayHistory.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isManualBcv) {
                TextButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Actualizar", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

