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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLogo(modifier = Modifier.size(32.dp))
                        Text("Cuentas Claras", fontWeight = FontWeight.Bold)
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAccountDialog = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nueva Cuenta", style = MaterialTheme.typography.labelLarge) }
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
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Saldo total",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        DualCurrencyText(
                            amountUsd = uiState.totalBalanceUsd,
                            bcvRate = uiState.bcvRate?.usdRate,
                            primaryStyle = MaterialTheme.typography.titleLarge,
                            primaryColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            secondaryColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        val rateText = uiState.bcvRate?.let {
                            String.format(Locale.US, "Tasa BCV: Bs. %.4f / USD · %s", it.usdRate, it.fechaValor)
                        } ?: "Tasa BCV no disponible"
                        Text(
                            text = rateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Tus Cuentas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.accounts.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Aún no hay cuentas",
                        subtitle = "Toca \"Nueva Cuenta\" para agregar tu primer saldo en efectivo, banco o billetera."
                    )
                }
            } else {
                items(uiState.accounts, key = { it.cuenta.id }) { item ->
                    AccountCard(
                        item = item,
                        bcvRate = uiState.bcvRate?.usdRate,
                        onClick = { onAccountClick(item.cuenta.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
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
private fun AccountCard(
    item: CuentaConSaldo,
    bcvRate: Double?,
    onClick: () -> Unit
) {
    val displayMoneda = if (item.cuenta.moneda == "USD") "USD" else "BS"
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.cuenta.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Moneda: $displayMoneda",
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
    val rawAmount = saldoText.toDoubleOrNull()
    val convertedUsd = when {
        rawAmount == null -> null
        inputCurrency == "BS" && rateAvailable -> rawAmount / bcvRate!!
        else -> rawAmount
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
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
            TextButton(
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
