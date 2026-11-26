package com.cuentasclaras.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import com.cuentasclaras.app.ui.components.GroupedCardHeader
import com.cuentasclaras.app.ui.components.GroupedCardItemSlice
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
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

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
                        Text(
                            text = account?.cuenta?.nombre ?: "Detalle de Cuenta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    actions = {
                        if (account != null) {
                            IconButton(
                                onClick = { showDeleteAccountDialog = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Eliminar cuenta",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
            if (account != null) {
                item(key = "account_summary", contentType = "summary") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Saldo inicial: ${CurrencyFormatter.formatDual(account.cuenta.saldoInicial, uiState.bcvRate?.usdRate)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Saldo actual",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                DualCurrencyText(
                                    amountUsd = account.saldoActual,
                                    bcvRate = uiState.bcvRate?.usdRate,
                                    primaryStyle = MaterialTheme.typography.headlineMedium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showTransactionSheet = true },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text(" Movimiento", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { showAdjustDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                ) {
                                    Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text(" Ajustar saldo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.transactions.isEmpty()) {
                    item(key = "empty_transactions", contentType = "empty") {
                        EmptyStateView(
                            icon = Icons.Outlined.Receipt,
                            title = "Aún no hay movimientos",
                            subtitle = "Registra un ingreso, gasto o ajuste de saldo arriba."
                        )
                    }
                } else {
                    item(key = "history_header", contentType = "history_header") {
                        val regLabel = if (uiState.transactions.size == 1) "1 registro" else "${uiState.transactions.size} registros"
                        GroupedCardHeader(
                            title = "Historial de movimientos",
                            trailing = regLabel
                        )
                    }

                    itemsIndexed(
                        items = uiState.transactions,
                        key = { _, tx -> tx.id },
                        contentType = { _, _ -> "transaction_row" }
                    ) { index, tx ->
                        GroupedCardItemSlice(
                            isFirst = index == 0,
                            isLast = index == uiState.transactions.lastIndex,
                            hasHeader = true
                        ) {
                            TransactionRow(
                                tx = tx,
                                bcvRate = uiState.bcvRate?.usdRate,
                                onInvoiceClick = onInvoiceClick
                            )
                        }
                    }
                }

                item(key = "bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDeleteAccountDialog && account != null) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("¿Eliminar cuenta?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Se eliminará \"${account.cuenta.nombre}\" y todos sus movimientos asociados. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount(onBack)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Eliminar", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
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
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Registrar movimiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Segmented GASTO / INGRESO matching Web
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(TipoTransaccion.GASTO, TipoTransaccion.INGRESO).forEach { option ->
                        val selected = tipo == option
                        val activeColor = if (option == TipoTransaccion.GASTO) ExpenseRed else IncomeGreen
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { tipo = option }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Text(
                        text = if (tipo == TipoTransaccion.INGRESO) "Guardar Ingreso" else "Guardar Gasto",
                        style = MaterialTheme.typography.titleMedium
                    )
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

        val rawDesired = desiredText.replace(',', '.').toDoubleOrNull()
        val desiredUsd = when {
            rawDesired == null -> null
            adjustCurrency == "BS" && rateAvailable -> rawDesired / bcvRate!!
            else -> rawDesired
        }
        val deltaUsd = desiredUsd?.let { it - account.saldoActual }

        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Ajustar saldo", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Saldo actual: ${CurrencyFormatter.formatDual(account.saldoActual, bcvRate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencySegmentedSelector(
                        selectedCurrency = adjustCurrency,
                        onSelectCurrency = { newCurrency ->
                            adjustCurrency = newCurrency
                            desiredText = if (newCurrency == "BS" && rateAvailable) {
                                String.format(Locale.US, "%.2f", account.saldoActual * bcvRate!!)
                            } else {
                                String.format(Locale.US, "%.2f", account.saldoActual)
                            }
                        },
                        bcvRateAvailable = rateAvailable
                    )
                    OutlinedTextField(
                        value = desiredText,
                        onValueChange = { desiredText = it },
                        label = { Text("Nuevo saldo deseado ($adjustCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Motivo del ajuste (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (deltaUsd != null) {
                        val sign = if (deltaUsd >= 0) "+" else ""
                        Text(
                            text = "Transacción AJUSTE: $sign${CurrencyFormatter.formatDual(deltaUsd, bcvRate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AdjustAmber
                        )
                    }
                }
            },
            confirmButton = {
                Button(
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
    val icon = when (tx.tipo) {
        TipoTransaccion.INGRESO -> Icons.Outlined.ArrowDownward
        TipoTransaccion.GASTO -> Icons.Outlined.ArrowUpward
        TipoTransaccion.AJUSTE -> Icons.Outlined.Tune
    }
    val linkedFacturaId = tx.facturaId
    val formattedDate = remember(tx.id, tx.fecha) {
        CurrencyFormatter.formatDate(tx.fecha)
    }

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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tx.concepto ?: tx.tipo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${tx.tipo.name} · $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (linkedFacturaId != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ir a la factura",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 52.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
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
}
