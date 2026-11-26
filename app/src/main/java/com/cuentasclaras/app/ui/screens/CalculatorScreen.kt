package com.cuentasclaras.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuentasclaras.app.data.local.CalculatorTempItem
import com.cuentasclaras.app.ui.components.AppLogo
import com.cuentasclaras.app.ui.components.BcvRateBubble
import com.cuentasclaras.app.ui.components.CurrencySegmentedSelector
import com.cuentasclaras.app.ui.components.DualCurrencyText
import com.cuentasclaras.app.ui.components.GroupedCardHeader
import com.cuentasclaras.app.ui.components.GroupedCardItemSlice
import com.cuentasclaras.app.ui.theme.EmeraldPrimary
import com.cuentasclaras.app.ui.theme.ExpenseRed
import com.cuentasclaras.app.ui.viewmodel.CalculatorViewModel
import com.cuentasclaras.app.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bcvRate = uiState.bcvRate?.usdRate
    val rateAvailable = bcvRate != null && bcvRate > 0.0

    var selectedCurrency by remember(uiState.defaultCurrency, rateAvailable) {
        mutableStateOf(if (uiState.defaultCurrency == "BS" && rateAvailable) "BS" else "USD")
    }
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val rawPrice = priceText.replace(',', '.').toDoubleOrNull()
    val parsedUnitUsd = when {
        rawPrice == null || rawPrice < 0.0 -> null
        selectedCurrency == "BS" && rateAvailable -> rawPrice / bcvRate!!
        else -> rawPrice
    }
    val parsedQty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1

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
            item(key = "calc_header", contentType = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Calculadora de precios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Calcula el precio total de tu compra",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Calculator Entry Card
            item(key = "calc_input_card", contentType = "input_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CurrencySegmentedSelector(
                            selectedCurrency = selectedCurrency,
                            onSelectCurrency = { selectedCurrency = it },
                            bcvRateAvailable = rateAvailable
                        )

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = {
                                Text("Precio ($selectedCurrency)", style = MaterialTheme.typography.bodyLarge)
                            },
                            placeholder = { Text("0.00", style = MaterialTheme.typography.titleLarge) },
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Real-time automatic conversion
                        if (parsedUnitUsd != null) {
                            val conversionText = if (selectedCurrency == "USD") {
                                val bsStr = CurrencyFormatter.formatBs(parsedUnitUsd, bcvRate)
                                if (bsStr != null) "Equivale a $bsStr" else "Tasa BCV no disponible"
                            } else {
                                "Equivale a ${CurrencyFormatter.formatUsd(parsedUnitUsd)}"
                            }
                            Text(
                                text = conversionText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }

                        // Quantity Field + Large 56dp - and + buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Cantidad",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CalcStepperButton(
                                    icon = Icons.Outlined.Remove,
                                    contentDescription = "Disminuir cantidad",
                                    enabled = parsedQty > 1,
                                    onClick = {
                                        val next = (parsedQty - 1).coerceAtLeast(1)
                                        quantityText = next.toString()
                                    }
                                )

                                OutlinedTextField(
                                    value = quantityText,
                                    onValueChange = { raw ->
                                        quantityText = raw.filter { it.isDigit() }
                                    },
                                    textStyle = MaterialTheme.typography.titleLarge.copy(
                                        textAlign = TextAlign.Center,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                CalcStepperButton(
                                    icon = Icons.Outlined.Add,
                                    contentDescription = "Aumentar cantidad",
                                    enabled = true,
                                    onClick = {
                                        quantityText = (parsedQty + 1).toString()
                                    }
                                )
                            }
                        }

                        if (parsedUnitUsd != null && parsedUnitUsd > 0.0) {
                            val lineTotal = parsedUnitUsd * parsedQty
                            Text(
                                text = "Cálculo: ${CurrencyFormatter.formatDual(lineTotal, bcvRate)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                if (parsedUnitUsd != null && parsedUnitUsd > 0.0) {
                                    viewModel.addTempItem(parsedUnitUsd, parsedQty)
                                    priceText = ""
                                    quantityText = "1"
                                }
                            },
                            enabled = parsedUnitUsd != null && parsedUnitUsd > 0.0 && parsedQty >= 1,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Añadir a la lista",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Total Acumulado + Saldo disponible total & por cuenta (sin descontar) + Limpiar
            item(key = "calc_total_card", contentType = "total_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Total Acumulado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DualCurrencyText(
                                amountUsd = uiState.totalAccumulatedUsd,
                                bcvRate = bcvRate,
                                primaryStyle = MaterialTheme.typography.headlineMedium
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        // Saldo disponible total y en cada cuenta
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Saldo disponible total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DualCurrencyText(
                                amountUsd = uiState.totalAvailableBalanceUsd,
                                bcvRate = bcvRate,
                                primaryStyle = MaterialTheme.typography.titleLarge,
                                horizontalAlignment = Alignment.Start
                            )

                            if (uiState.accounts.isEmpty()) {
                                Text(
                                    text = "No tienes cuentas registradas.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        uiState.accounts.forEachIndexed { idx, acc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = acc.cuenta.nombre,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 10.dp)
                                                )
                                                DualCurrencyText(
                                                    amountUsd = acc.saldoActual,
                                                    bcvRate = bcvRate,
                                                    primaryStyle = MaterialTheme.typography.titleMedium,
                                                    horizontalAlignment = Alignment.End
                                                )
                                            }
                                            if (idx < uiState.accounts.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            enabled = uiState.items.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.items.isNotEmpty()) ExpenseRed else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ExpenseRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Limpiar lista",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Saved Calculator List
            item(key = "temp_list_header", contentType = "list_header") {
                val count = uiState.items.size
                GroupedCardHeader(
                    title = "Lista guardada",
                    trailing = if (count == 1) "1 ítem" else "$count ítems"
                )
            }

            if (uiState.items.isEmpty()) {
                item(key = "temp_list_empty", contentType = "empty") {
                    GroupedCardItemSlice(
                        isFirst = true,
                        isLast = true,
                        hasHeader = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "Tu lista está vacía",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Ingresa un precio arriba y toca \"Añadir a la lista\" para ir sumando.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.items,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "calc_temp_item" }
                ) { index, item ->
                    GroupedCardItemSlice(
                        isFirst = index == 0,
                        isLast = index == uiState.items.lastIndex,
                        hasHeader = true
                    ) {
                        TempCalcItemRow(
                            item = item,
                            bcvRate = bcvRate,
                            onRemove = { viewModel.removeTempItem(item.id) }
                        )
                    }
                }
            }

            item(key = "calc_bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("¿Limpiar calculadora?", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    text = "Se eliminarán todos los ítems sumados en la lista de la calculadora. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearItems()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.heightIn(min = 56.dp)
                ) {
                    Text("Sí, limpiar", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    modifier = Modifier.heightIn(min = 56.dp)
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun CalcStepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(56.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TempCalcItemRow(
    item: CalculatorTempItem,
    bcvRate: Double?,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${item.quantity} × ${CurrencyFormatter.formatDualBsFirst(item.unitUsd, bcvRate)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DualCurrencyText(
                amountUsd = item.subtotalUsd,
                bcvRate = bcvRate,
                primaryStyle = MaterialTheme.typography.titleLarge,
                horizontalAlignment = Alignment.Start
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Quitar de la lista",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
