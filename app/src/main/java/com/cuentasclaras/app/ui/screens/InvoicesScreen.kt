package com.cuentasclaras.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuentasclaras.app.data.local.dao.FacturaConItems
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.Factura
import com.cuentasclaras.app.ui.components.BcvRateBubble
import com.cuentasclaras.app.ui.components.DeleteInvoiceConfirmDialog
import com.cuentasclaras.app.ui.components.DualCurrencyText
import com.cuentasclaras.app.ui.components.EmptyStateView
import com.cuentasclaras.app.ui.theme.IncomeGreen
import com.cuentasclaras.app.ui.viewmodel.InvoicesViewModel
import com.cuentasclaras.app.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    viewModel: InvoicesViewModel,
    snackbarHostState: SnackbarHostState,
    onInvoiceClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteFactura by remember { mutableStateOf<Factura?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Facturas", fontWeight = FontWeight.Bold) },
                actions = {
                    BcvRateBubble(
                        bcvRate = uiState.bcvRate,
                        history = uiState.bcvHistory,
                        isRefreshing = uiState.isRefreshingRate,
                        onRefresh = { viewModel.refreshBcvRate() }
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.createNewInvoice(onInvoiceClick) },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nueva Factura", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.invoices.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "Aún no hay facturas",
                        subtitle = "Crea un borrador de factura para detallar artículos y seleccionar cuentas para pagar."
                    )
                }
            } else {
                items(uiState.invoices, key = { it.factura.id }) { item ->
                    InvoiceCard(
                        item = item,
                        bcvRate = uiState.bcvRate?.usdRate,
                        onClick = { onInvoiceClick(item.factura.id) },
                        onDeleteClick = { pendingDeleteFactura = item.factura }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    pendingDeleteFactura?.let { factura ->
        DeleteInvoiceConfirmDialog(
            factura = factura,
            onDismiss = { pendingDeleteFactura = null },
            onConfirmDelete = { refundMoney ->
                viewModel.deleteInvoice(factura, refundMoney)
                pendingDeleteFactura = null
            }
        )
    }
}

@Composable
private fun InvoiceCard(
    item: FacturaConItems,
    bcvRate: Double?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPaid = item.factura.estado == EstadoFactura.PAGADA
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val title = item.factura.displayName()
                    val defaultTitle = "Factura #${item.factura.numeroFactura}"
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (title != defaultTitle) {
                        Text(
                            text = defaultTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${item.factura.estado.name} · ${item.items.size} artículos",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatDate(item.factura.fecha),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Eliminar factura"
                    )
                }
            }
            DualCurrencyText(
                amountUsd = item.factura.total,
                bcvRate = bcvRate,
                primaryStyle = MaterialTheme.typography.titleLarge,
                horizontalAlignment = Alignment.Start
            )
        }
    }
}
