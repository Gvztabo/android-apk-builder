package com.cuentasclaras.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuentasclaras.app.data.local.dao.FacturaConItems
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.Factura
import com.cuentasclaras.app.ui.components.AppLogo
import com.cuentasclaras.app.ui.components.BcvRateBubble
import com.cuentasclaras.app.ui.components.DeleteInvoiceConfirmDialog
import com.cuentasclaras.app.ui.components.DualCurrencyText
import com.cuentasclaras.app.ui.components.EmptyStateView
import com.cuentasclaras.app.ui.components.GroupedCardItemSlice
import com.cuentasclaras.app.ui.theme.AdjustAmber
import com.cuentasclaras.app.ui.theme.EmeraldPrimary
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppLogo(modifier = Modifier.size(30.dp))
                            Text(
                                text = "Cuentas Claras",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        BcvRateBubble(
                            bcvRate = uiState.bcvRate,
                            history = uiState.bcvHistory,
                            isRefreshing = uiState.isRefreshingRate,
                            onRefresh = { viewModel.refreshBcvRate() },
                            isManualBcv = uiState.isManualBcv
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item(key = "invoices_header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Facturas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val countLabel = if (uiState.invoices.size == 1) {
                            "1 factura"
                        } else {
                            "${uiState.invoices.size} facturas"
                        }
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { viewModel.createNewInvoice(onInvoiceClick) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.heightIn(min = 56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nueva Factura", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (uiState.invoices.isEmpty()) {
                item(key = "empty_invoices", contentType = "empty") {
                    EmptyStateView(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "Aún no hay facturas",
                        subtitle = "Crea un borrador de factura para detallar productos y verificar la disponibilidad entre tus cuentas."
                    ) {
                        Button(
                            onClick = { viewModel.createNewInvoice(onInvoiceClick) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Text("Crear Nueva Factura", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.invoices,
                    key = { _, item -> item.factura.id },
                    contentType = { _, _ -> "invoice_row" }
                ) { index, item ->
                    GroupedCardItemSlice(
                        isFirst = index == 0,
                        isLast = index == uiState.invoices.lastIndex,
                        hasHeader = false
                    ) {
                        InvoiceRow(
                            item = item,
                            bcvRate = uiState.bcvRate?.usdRate,
                            onClick = { onInvoiceClick(item.factura.id) },
                            onDeleteClick = { pendingDeleteFactura = item.factura }
                        )
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
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
private fun InvoiceRow(
    item: FacturaConItems,
    bcvRate: Double?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPaid = item.factura.estado == EstadoFactura.PAGADA
    val formattedDate = remember(item.factura.id, item.factura.fecha) {
        CurrencyFormatter.formatDate(item.factura.fecha)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (title != defaultTitle) {
                    Text(
                        text = defaultTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val articlesText = if (item.items.size == 1) "1 artículo" else "${item.items.size} artículos"
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.factura.estado.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPaid) IncomeGreen else AdjustAmber
                    )
                    Text(
                        text = "· $articlesText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Eliminar factura",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
