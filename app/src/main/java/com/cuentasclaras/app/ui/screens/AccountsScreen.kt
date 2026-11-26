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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuentasclaras.app.data.local.dao.CuentaConSaldo
import com.cuentasclaras.app.data.local.entity.Cuenta
import com.cuentasclaras.app.ui.components.AppLogo
import com.cuentasclaras.app.ui.components.BcvRateBubble
import com.cuentasclaras.app.ui.components.CurrencySegmentedSelector
import com.cuentasclaras.app.ui.components.DualCurrencyText
import com.cuentasclaras.app.ui.components.EmptyStateView
import com.cuentasclaras.app.ui.components.GroupedCardItemSlice
import com.cuentasclaras.app.ui.theme.EmeraldPrimary
import com.cuentasclaras.app.ui.viewmodel.AccountsViewModel
import com.cuentasclaras.app.util.CurrencyFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onAccountClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAccountDialog by remember { mutableStateOf(false) }

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
                            onRefresh = { viewModel.refreshBcvRate() }
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
            // Solid Emerald Hero Card matching Web
            item(key = "hero_card", contentType = "hero") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Saldo total",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD1FAE5)
                                )
                                DualCurrencyText(
                                    amountUsd = uiState.totalBalanceUsd,
                                    bcvRate = uiState.bcvRate?.usdRate,
                                    primaryStyle = MaterialTheme.typography.headlineMedium,
                                    primaryColor = Color.White,
                                    secondaryColor = Color(0xFFE6FFF4)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { viewModel.refreshBcvRate() },
                                    enabled = !uiState.isRefreshingRate
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Actualizar tasa BCV",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

                        val rateText = uiState.bcvRate?.let {
                            String.format(Locale.US, "Tasa Oficial BCV: Bs. %.4f / USD", it.usdRate)
                        } ?: "Tasa BCV no disponible"
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = rateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFECFDF5)
                            )
                            uiState.bcvRate?.fechaValor?.takeIf { it.isNotBlank() }?.let { fecha ->
                                Text(
                                    text = fecha,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section Header + Inline Primary Action Button (matching Web)
            item(key = "section_header", contentType = "header") {
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
                            text = "Tus Cuentas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val countLabel = if (uiState.accounts.size == 1) {
                            "1 cuenta guardada localmente"
                        } else {
                            "${uiState.accounts.size} cuentas guardadas localmente"
                        }
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { showAccountDialog = true },
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
                        Text("Nueva Cuenta", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (uiState.accounts.isEmpty()) {
                item(key = "empty_accounts", contentType = "empty") {
                    EmptyStateView(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Aún no hay cuentas",
                        subtitle = "Agrega una cuenta de efectivo, banco o billetera digital para controlar tus saldos en USD y BS."
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showAccountDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                            ) {
                                Text("Crear Primera Cuenta", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(
                                onClick = { viewModel.loadSampleData() },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                            ) {
                                Text("Cargar Datos de Ejemplo", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.accounts,
                    key = { _, item -> item.cuenta.id },
                    contentType = { _, _ -> "account_row" }
                ) { index, item ->
                    GroupedCardItemSlice(
                        isFirst = index == 0,
                        isLast = index == uiState.accounts.lastIndex,
                        hasHeader = false
                    ) {
                        AccountRow(
                            item = item,
                            bcvRate = uiState.bcvRate?.usdRate,
                            onClick = { onAccountClick(item.cuenta.id) }
                        )
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAccountDialog) {
        AccountFormDialog(
            initialAccount = null,
            bcvRate = uiState.bcvRate?.usdRate,
            defaultCurrency = uiState.defaultCurrency,
            onDismiss = { showAccountDialog = false },
            onSave = { nombre, saldoInicialUsd, moneda ->
                viewModel.saveAccount(
                    id = 0L,
                    nombre = nombre,
                    saldoInicialUsd = saldoInicialUsd,
                    moneda = moneda
                )
                showAccountDialog = false
            }
        )
    }
}

@Composable
private fun AccountRow(
    item: CuentaConSaldo,
    bcvRate: Double?,
    onClick: () -> Unit
) {
    val displayMoneda = if (item.cuenta.moneda == "USD") "USD" else "BS"
    val txLabel = if (item.transaccionesCount == 1) "1 movimiento" else "${item.transaccionesCount} movimientos"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.cuenta.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$displayMoneda · $txLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DualCurrencyText(
            amountUsd = item.saldoActual,
            bcvRate = bcvRate,
            primaryStyle = MaterialTheme.typography.titleLarge,
            horizontalAlignment = Alignment.Start
        )
    }
}

@Composable
private fun AccountFormDialog(
    initialAccount: Cuenta?,
    bcvRate: Double?,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (nombre: String, saldoInicialUsd: Double, moneda: String) -> Unit
) {
    val rateAvailable = bcvRate != null && bcvRate > 0.0
    var nombre by remember { mutableStateOf(initialAccount?.nombre ?: "") }
    var inputCurrency by remember {
        mutableStateOf(
            if (initialAccount == null && defaultCurrency == "BS" && rateAvailable) "BS" else "USD"
        )
    }
    var saldoText by remember {
        mutableStateOf(
            initialAccount?.let { String.format(Locale.US, "%.2f", it.saldoInicial) } ?: ""
        )
    }

    val rawAmount = saldoText.replace(',', '.').toDoubleOrNull()
    val convertedUsd = when {
        rawAmount == null -> null
        inputCurrency == "BS" && rateAvailable -> rawAmount / bcvRate!!
        else -> rawAmount
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initialAccount == null) "Nueva Cuenta" else "Editar Cuenta",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la cuenta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Moneda del monto inicial",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                CurrencySegmentedSelector(
                    selectedCurrency = inputCurrency,
                    onSelectCurrency = { inputCurrency = it },
                    bcvRateAvailable = rateAvailable
                )

                OutlinedTextField(
                    value = saldoText,
                    onValueChange = { saldoText = it },
                    label = { Text("Saldo inicial ($inputCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (convertedUsd != null) {
                    Text(
                        text = if (inputCurrency == "BS") {
                            "Equivale a ${CurrencyFormatter.formatUsd(convertedUsd)} (guardado en USD)"
                        } else {
                            CurrencyFormatter.formatDual(convertedUsd, bcvRate)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountUsd = convertedUsd ?: 0.0
                    if (nombre.isNotBlank()) {
                        onSave(nombre, amountUsd, inputCurrency)
                    }
                },
                enabled = nombre.isNotBlank() && (saldoText.isBlank() || convertedUsd != null)
            ) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
