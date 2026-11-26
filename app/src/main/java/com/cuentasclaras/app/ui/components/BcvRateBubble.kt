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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentasclaras.app.data.remote.BcvRateHistoryEntry
import com.cuentasclaras.app.data.remote.BcvRateInfo
import com.cuentasclaras.app.data.remote.BcvRateService
import com.cuentasclaras.app.data.remote.BcvTrendDirection
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
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val trend = remember(history) { BcvRateService.computeTrend(history) }

    Surface(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .padding(end = 8.dp)
            .heightIn(min = 40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val label = bcvRate?.let {
                String.format(Locale.US, "BCV Bs. %.2f", it.usdRate)
            } ?: "Sincronizar BCV"
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (bcvRate != null) {
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Tasa Oficial BCV",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Se actualiza automáticamente cada día",
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
                                    text = "Precio actual",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format(Locale.US, "Bs. %.4f", bcvRate.usdRate),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
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

                            if (history.size >= 2) {
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

                            if (bcvRate.fechaValor.isNotBlank()) {
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
                        text = "Tasa BCV no disponible aún.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Text(
                    text = "Historial reciente (últimos 5 precios)",
                    style = MaterialTheme.typography.titleMedium
                )

                if (history.isEmpty()) {
                    Text(
                        text = "Sin registros guardados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        history.forEachIndexed { index, entry ->
                            val prev = history.getOrNull(index + 1)
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
                                    val dateLabel = entry.fechaValor.ifBlank {
                                        if (entry.recordedAtMillis > 0L) {
                                            CurrencyFormatter.formatDate(entry.recordedAtMillis)
                                        } else {
                                            ""
                                        }
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
                            if (index < history.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
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
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
