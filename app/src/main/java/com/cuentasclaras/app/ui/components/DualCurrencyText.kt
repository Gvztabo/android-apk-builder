package com.cuentasclaras.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            text = usdFormatted,
            style = primaryStyle,
            color = primaryColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = bsFormatted ?: "Tasa BCV no disponible",
            style = MaterialTheme.typography.bodyLarge,
            color = secondaryColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val enabled = label == "USD" || bcvRateAvailable
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { if (enabled) onSelectCurrency(label) },
                    selected = selectedCurrency == label,
                    enabled = enabled,
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) }
                )
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
                            .clickable { refundChecked = !refundChecked }
                            .padding(vertical = 4.dp),
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
