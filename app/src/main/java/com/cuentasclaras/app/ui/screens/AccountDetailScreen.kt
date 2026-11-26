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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuentasclaras.app.data.local.entity.TipoTransaccion
import com.cuentasclaras.app.data.local.entity.Transaccion
import com.cuentasclaras.app.ui.components.CurrencySegmentedSelector
import com.cuentasclaras.app.ui.components.DualCurrencyText
import com.cuentasclaras.app.ui.components.EmptyStateView
import com.cuentasclaras.app.ui.theme.AdjustAmber
import com.cuentasclaras.app.ui.theme.EmeraldPrimary
import com.cuentasclaras.app.ui.theme.ExpenseRed
import com.cuentasclaras.app.ui.theme.IncomeGreen
import com.cuentasclaras.app.ui.viewmodel.AccountDetailViewModel
import com.cuentasclaras.app.util.CurrencyFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel,
    onBack: () -> Unit,
    onInvoiceClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val account = uiState.account
    var showTransactionSheet by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.cuenta?.nombre ?: "Detalle de Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
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
            if (account != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Saldo actual",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DualCurrencyText(
                                amountUsd = account.saldoActual,
                                bcvRate = uiState.bcvRate?.usdRate,
                                primaryStyle = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showTransactionSheet = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Text(" Movimiento")
                                }
                                OutlinedButton(
                                    onClick = { showAdjustDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Outlined.Tune, contentDescription = null)
                                    Text(" Ajustar saldo")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Historial de movimientos",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Receipt,
                            title = "Aún no hay movimientos",
                            subtitle = "Registra un ingreso, gasto o ajuste de saldo arriba."
                        )
                    }
                } else {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        TransactionRow(
                            tx = tx,
                            bcvRate = uiState.bcvRate?.usdRate,
                            onInvoiceClick = onInvoiceClick
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showTransactionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val bcvRate = uiState.bcvRate?.usdRate
        val rateAvailable = bcvRate != null && bcvRate > 0.0
        var txCurrency by remember(showTransactionSheet) {
            mutableStateOf(
                if (uiState.defaultCurrency == "BS" && rateAvailable) "BS" else "USD"
            )
        }
        var amountStr by remember { mutableStateOf("") }
        var tipo by remember { mutableStateOf(TipoTransaccion.GASTO) }
        var concepto by remember { mutableStateOf("") }
        val amountFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(showTransactionSheet) {
            if (showTransactionSheet) {
                amountFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        val rawAmount = amountStr.replace(',', '.').toDoubleOrNull()
        val parsedAmountUsd = when {
            rawAmount == null -> null
            txCurrency == "BS" && rateAvailable -> rawAmount / bcvRate!!
            else -> rawAmount
        }

        ModalBottomSheet(
            onDismissRequest = { showTransactionSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nuevo movimiento",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipo == TipoTransaccion.GASTO,
                        onClick = { tipo = TipoTransaccion.GASTO },
                        label = { Text("Gasto (GASTO)", style = MaterialTheme.typography.bodyLarge) }
                    )
                    FilterChip(
                        selected = tipo == TipoTransaccion.INGRESO,
                        onClick = { tipo = TipoTransaccion.INGRESO },
                        label = { Text("Ingreso (INGRESO)", style = MaterialTheme.typography.bodyLarge) }
                    )
                }

                CurrencySegmentedSelector(
                    selectedCurrency = txCurrency,
                    onSelectCurrency = { txCurrency = it },
                    bcvRateAvailable = rateAvailable
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto ($txCurrency)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester)
                )

                if (parsedAmountUsd != null && parsedAmountUsd > 0.0) {
                    Text(
                        text = if (txCurrency == "BS") {
                            "Equivale a ${CurrencyFormatter.formatUsd(parsedAmountUsd)} (guardado en USD)"
                        } else {
                            CurrencyFormatter.formatDual(parsedAmountUsd, bcvRate)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto / Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (parsedAmountUsd != null && parsedAmountUsd > 0.0) {
                            viewModel.addTransaction(parsedAmountUsd, tipo, concepto)
                            showTransactionSheet = false
                        }
                    },
                    enabled = parsedAmountUsd != null && parsedAmountUsd > 0.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Guardar movimiento", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showAdjustDialog && account != null) {
        val bcvRate = uiState.bcvRate?.usdRate
        val rateAvailable = bcvRate != null && bcvRate > 0.0
        val initialAdjustCurrency = if (uiState.defaultCurrency == "BS" && rateAvailable) "BS" else "USD"
        var adjustCurrency by remember(showAdjustDialog) { mutableStateOf(initialAdjustCurrency) }
        var desiredText by remember(showAdjustDialog) {
            mutableStateOf(
                if (initialAdjustCurrency == "BS" && rateAvailable) {
                    String.format(Locale.US, "%.2f", account.saldoActual * bcvRate!!)
                } else {
                    String.format(Locale.US, "%.2f", account.saldoActual)
                }
            )
        }
        var note by remember { mutableStateOf("") }

        val rawDesired = desiredText.toDoubleOrNull()
        val desiredUsd = when {
            rawDesired == null -> null
            adjustCurrency == "BS" && rateAvailable -> rawDesired / bcvRate!!
            else -> rawDesired
        }
        val deltaUsd = desiredUsd?.let { it - account.saldoActual }

        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Ajustar saldo", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Actual: ${CurrencyFormatter.formatDual(account.saldoActual, bcvRate)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    CurrencySegmentedSelector(
                        selectedCurrency = adjustCurrency,
                        onSelectCurrency = { newCurrency ->
                            adjustCurrency = newCurrency
                            if (newCurrency == "BS" && rateAvailable) {
                                desiredText = String.format(Locale.US, "%.2f", account.saldoActual * bcvRate!!)
                            } else {
                                desiredText = String.format(Locale.US, "%.2f", account.saldoActual)
                            }
                        },
                        bcvRateAvailable = rateAvailable
                    )
                    OutlinedTextField(
                        value = desiredText,
                        onValueChange = { desiredText = it },
                        label = { Text("Saldo deseado ($adjustCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Motivo (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (deltaUsd != null) {
                        val sign = if (deltaUsd >= 0) "+" else ""
                        Text(
                            text = "Transacción AJUSTE: $sign${CurrencyFormatter.formatDual(deltaUsd, bcvRate)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AdjustAmber
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (desiredUsd != null) {
                            viewModel.adjustBalance(desiredUsd, note)
                            showAdjustDialog = false
                        }
                    },
                    enabled = desiredUsd != null && kotlin.math.abs(deltaUsd ?: 0.0) >= 0.005
                ) {
                    Text("Confirmar ajuste", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun TransactionRow(
    tx: Transaccion,
    bcvRate: Double?,
    onInvoiceClick: (Long) -> Unit
) {
    val signedAmount = when (tx.tipo) {
        TipoTransaccion.INGRESO -> tx.monto
        TipoTransaccion.GASTO -> -tx.monto
        TipoTransaccion.AJUSTE -> tx.monto
    }
    val tint = when (tx.tipo) {
        TipoTransaccion.INGRESO -> IncomeGreen
        TipoTransaccion.GASTO -> ExpenseRed
        TipoTransaccion.AJUSTE -> AdjustAmber
    }
    val linkedFacturaId = tx.facturaId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (linkedFacturaId != null) {
                    Modifier.clickable { onInvoiceClick(linkedFacturaId) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tx.concepto ?: tx.tipo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tx.tipo.name} · ${CurrencyFormatter.formatDate(tx.fecha)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (linkedFacturaId != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ir a la factura",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DualCurrencyText(
            amountUsd = signedAmount,
            bcvRate = bcvRate,
            primaryStyle = MaterialTheme.typography.titleMedium,
            primaryColor = tint,
            horizontalAlignment = Alignment.Start
        )

        if (linkedFacturaId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Toca para abrir esta factura",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )
            }
        }
    }
}
