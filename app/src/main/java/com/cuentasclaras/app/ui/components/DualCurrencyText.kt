package com.cuentasclaras.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.Factura
import com.cuentasclaras.app.util.CurrencyFormatter

@Composable
fun DualCurrencyText(
    amountUsd: Double,
    bcvRate: Double?,
    modifier: Modifier = Modifier,
    primaryStyle: TextStyle = MaterialTheme.typography.titleLarge,
    primaryColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    val usdFormatted = CurrencyFormatter.formatUsd(amountUsd)
    val bsFormatted = CurrencyFormatter.formatBs(amountUsd, bcvRate)

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = usdFormatted,
            style = primaryStyle,
            color = primaryColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = bsFormatted ?: "Tasa BCV no disponible",
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Medium,
            color = secondaryColor
        )
    }
}

@Composable
fun CurrencySegmentedSelector(
    selectedCurrency: String,
    onSelectCurrency: (String) -> Unit,
    bcvRateAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val options = listOf("USD", "BS")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { label ->
                val enabled = label == "USD" || bcvRateAvailable
                val selected = selectedCurrency == label
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(enabled = enabled) { onSelectCurrency(label) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                selected -> MaterialTheme.colorScheme.onSurface
                                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
        if (!bcvRateAvailable) {
            Text(
                text = "Tasa BCV no disponible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun DeleteInvoiceConfirmDialog(
    factura: Factura,
    onDismiss: () -> Unit,
    onConfirmDelete: (refundMoney: Boolean) -> Unit
) {
    val isPaid = factura.estado == EstadoFactura.PAGADA
    var refundChecked by remember(factura.id) { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (isPaid) "¿Eliminar factura pagada?" else "¿Eliminar factura?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isPaid) {
                        "Esta factura ya fue pagada. Si marcas 'Devolución de dinero', se revertirán los gastos asociados y los saldos de las cuentas volverán a su estado anterior. Si lo desmarcas, la factura se eliminará pero los gastos se mantendrán."
                    } else {
                        "Esta factura está en borrador. Esta acción no se puede deshacer."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isPaid) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { refundChecked = !refundChecked }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = refundChecked,
                            onCheckedChange = { refundChecked = it }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Devolución de dinero",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmDelete(if (isPaid) refundChecked else false) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
