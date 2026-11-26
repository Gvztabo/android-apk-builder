package com.cuentasclaras.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.ItemFactura
import com.cuentasclaras.app.data.repository.PaymentAllocation
import com.cuentasclaras.app.ui.components.CurrencySegmentedSelector
import com.cuentasclaras.app.ui.components.DeleteInvoiceConfirmDialog
import com.cuentasclaras.app.ui.components.DualCurrencyText
import com.cuentasclaras.app.ui.components.EmptyStateView
import com.cuentasclaras.app.ui.theme.EmeraldPrimary
import com.cuentasclaras.app.ui.theme.ExpenseRed
import com.cuentasclaras.app.ui.theme.IncomeGreen
import com.cuentasclaras.app.ui.viewmodel.InvoiceEditorViewModel
import com.cuentasclaras.app.util.CurrencyFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    viewModel: InvoiceEditorViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val invoiceWithItems = uiState.invoiceWithItems
    val isDraft = invoiceWithItems?.factura?.estado == EstadoFactura.BORRADOR
    val bcvRate = uiState.bcvRate?.usdRate
    val rateAvailable = bcvRate != null && bcvRate > 0.0

    var showAddForm by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemPriceText by remember { mutableStateOf("") }
    var itemQty by remember { mutableIntStateOf(1) }
    var itemCurrency by remember {
        mutableStateOf(if (uiState.defaultCurrency == "BS" && rateAvailable) "BS" else "USD")
    }
    var editingItem by remember { mutableStateOf<ItemFactura?>(null) }
    var showAccountPickerSheet by remember { mutableStateOf(false) }
    var showSingleConfirmDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    val splitAmountTexts = remember { mutableStateMapOf<Long, String>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val invoiceId = invoiceWithItems?.factura?.id ?: 0L
    val selectedAccountIds = remember(invoiceId) { mutableStateMapOf<Long, Boolean>() }
    val selectedAccounts = uiState.accounts.filter { selectedAccountIds[it.cuenta.id] == true }
    val invoiceTotal = invoiceWithItems?.factura?.total ?: 0.0
    val selectedTotal = selectedAccounts.sumOf { it.saldoActual }
    val diffAfterPayRaw = selectedTotal - invoiceTotal
    val diffAfterPay = if (abs(diffAfterPayRaw) < 0.005) 0.0 else diffAfterPayRaw
    val isSufficient = selectedAccounts.isNotEmpty() && selectedTotal + 0.001 >= invoiceTotal

    val priceFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(showAddForm) {
        if (showAddForm) {
            itemCurrency = if (uiState.defaultCurrency == "BS" && rateAvailable) "BS" else "USD"
            priceFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val factura = invoiceWithItems?.factura
                    if (factura != null) {
                        Column {
                            Text(
                                text = factura.displayName(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Factura #${factura.numeroFactura} · ${factura.estado.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("Factura", style = MaterialTheme.typography.titleMedium)
                    }
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
                    if (invoiceWithItems != null) {
                        IconButton(
                            onClick = {
                                renameText = invoiceWithItems.factura.displayName()
                                showRenameDialog = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar nombre de factura"
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Eliminar factura"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (invoiceWithItems == null) {
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Total de la factura",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        DualCurrencyText(
                            amountUsd = invoiceTotal,
                            bcvRate = bcvRate,
                            primaryStyle = MaterialTheme.typography.titleLarge,
                            primaryColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            secondaryColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        if (isDraft) {
                            if (uiState.accounts.isEmpty()) {
                                Text(
                                    text = "Sin cuentas registradas",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            } else {
                                if (selectedAccounts.isEmpty()) {
                                    Button(
                                        onClick = { showAccountPickerSheet = true },
                                        enabled = invoiceTotal > 0.0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null)
                                        Text(" Seleccionar cuentas", style = MaterialTheme.typography.labelLarge)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { showAccountPickerSheet = true },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF0F172A)
                                        ),
                                        border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null)
                                        Text(
                                            text = " Seleccionar cuentas (${selectedAccounts.size})",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    Text(
                                        text = "Cuentas: ${selectedAccounts.joinToString(", ") { it.cuenta.nombre }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Saldo disponible",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                        DualCurrencyText(
                                            amountUsd = selectedTotal,
                                            bcvRate = bcvRate,
                                            primaryStyle = MaterialTheme.typography.titleMedium,
                                            primaryColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            secondaryColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                            horizontalAlignment = Alignment.Start
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Saldo disponible luego de pagar",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            DualCurrencyText(
                                                amountUsd = diffAfterPay,
                                                bcvRate = bcvRate,
                                                primaryStyle = MaterialTheme.typography.titleMedium,
                                                primaryColor = if (isSufficient) IncomeGreen else ExpenseRed,
                                                secondaryColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                                horizontalAlignment = Alignment.Start
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isSufficient) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = if (isSufficient) IncomeGreen else ExpenseRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isSufficient) "Alcanza" else "No alcanza",
                                                    color = if (isSufficient) IncomeGreen else ExpenseRed,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (selectedAccounts.size == 1) {
                                                showSingleConfirmDialog = true
                                            } else if (selectedAccounts.size >= 2) {
                                                splitAmountTexts.clear()
                                                var remaining = invoiceTotal
                                                selectedAccounts.forEach { acc ->
                                                    val take = min(remaining, acc.saldoActual.coerceAtLeast(0.0))
                                                    remaining = (remaining - take).coerceAtLeast(0.0)
                                                    splitAmountTexts[acc.cuenta.id] = String.format(Locale.US, "%.2f", take)
                                                }
                                                showSplitDialog = true
                                            }
                                        },
                                        enabled = isSufficient && invoiceTotal > 0.0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Text("Pagar", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                Text(" Factura pagada", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            if (isDraft) {
                item {
                    if (!showAddForm) {
                        FilledTonalButton(
                            onClick = { showAddForm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text(" Añadir producto", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Añadir producto",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    TextButton(onClick = { showAddForm = false }) {
                                        Text("Cerrar")
                                    }
                                }

                                CurrencySegmentedSelector(
                                    selectedCurrency = itemCurrency,
                                    onSelectCurrency = { itemCurrency = it },
                                    bcvRateAvailable = rateAvailable
                                )

                                OutlinedTextField(
                                    value = itemName,
                                    onValueChange = { itemName = it },
                                    label = { Text("Nombre del producto (opcional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = itemPriceText,
                                    onValueChange = { itemPriceText = it },
                                    label = { Text("Precio unitario ($itemCurrency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(priceFocusRequester)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cantidad",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { if (itemQty > 1) itemQty -= 1 },
                                            enabled = itemQty > 1,
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Outlined.Remove, contentDescription = "Disminuir cantidad")
                                        }
                                        Text(
                                            text = "$itemQty",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                        FilledTonalButton(
                                            onClick = { itemQty += 1 },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = "Aumentar cantidad")
                                        }
                                    }
                                }

                                val rawPrice = itemPriceText.replace(',', '.').toDoubleOrNull()
                                val priceUsd = when {
                                    rawPrice == null -> null
                                    itemCurrency == "BS" && rateAvailable -> rawPrice / bcvRate!!
                                    else -> rawPrice
                                }

                                if (priceUsd != null && itemCurrency == "BS") {
                                    Text(
                                        text = "Equivale a ${CurrencyFormatter.formatUsd(priceUsd)} por unidad",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (priceUsd != null && itemQty > 0) {
                                            viewModel.addItem(itemName, priceUsd, itemQty)
                                            itemName = ""
                                            itemPriceText = ""
                                            itemQty = 1
                                            priceFocusRequester.requestFocus()
                                        }
                                    },
                                    enabled = priceUsd != null && priceUsd >= 0.0 && itemQty > 0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Text(" Agregar a la factura", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Productos",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (invoiceWithItems.items.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.ShoppingBag,
                        title = "La factura está vacía",
                        subtitle = "Toca \"Añadir producto\" para agregar artículos a esta factura."
                    )
                }
            } else {
                items(invoiceWithItems.items, key = { it.id }) { item ->
                    if (isDraft) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteItem(item)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {}
                        ) {
                            InvoiceItemCard(
                                item = item,
                                bcvRate = bcvRate,
                                isDraft = true,
                                onIncrement = { viewModel.updateItemQuantity(item, item.cantidad + 1) },
                                onDecrement = {
                                    if (item.cantidad > 1) {
                                        viewModel.updateItemQuantity(item, item.cantidad - 1)
                                    }
                                },
                                onEdit = { editingItem = item },
                                onDelete = { viewModel.deleteItem(item) }
                            )
                        }
                    } else {
                        InvoiceItemCard(
                            item = item,
                            bcvRate = bcvRate,
                            isDraft = false,
                            onIncrement = {},
                            onDecrement = {},
                            onEdit = {},
                            onDelete = {}
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && invoiceWithItems != null) {
        DeleteInvoiceConfirmDialog(
            factura = invoiceWithItems.factura,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = { refundMoney ->
                showDeleteDialog = false
                viewModel.deleteCurrentInvoice(refundMoney) {
                    onBack()
                }
            }
        )
    }

    editingItem?.let { item ->
        EditInvoiceItemDialog(
            item = item,
            bcvRate = bcvRate,
            onDismiss = { editingItem = null },
            onSave = { nombre, precioUsd, cantidad ->
                viewModel.updateItemDetails(item, nombre, precioUsd, cantidad)
                editingItem = null
            }
        )
    }

    if (showRenameDialog && invoiceWithItems != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Nombre de la factura", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Si lo dejas vacío, se usará \"Factura #${invoiceWithItems.factura.numeroFactura}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameInvoice(renameText)
                        showRenameDialog = false
                    }
                ) {
                    Text("Guardar", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showAccountPickerSheet && invoiceWithItems != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAccountPickerSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Seleccionar cuentas",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${invoiceWithItems.factura.displayName()} — Total: ${CurrencyFormatter.formatDual(invoiceTotal, bcvRate)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                if (uiState.accounts.isEmpty()) {
                    Text(
                        text = "Sin cuentas registradas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.accounts.forEach { acc ->
                            val isChecked = selectedAccountIds[acc.cuenta.id] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAccountIds[acc.cuenta.id] = !isChecked
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selectedAccountIds[acc.cuenta.id] = it
                                    }
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = acc.cuenta.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatDual(acc.saldoActual, bcvRate),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                if (selectedAccounts.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Saldo disponible: ${CurrencyFormatter.formatDual(selectedTotal, bcvRate)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Luego de pagar: ${CurrencyFormatter.formatDual(diffAfterPay, bcvRate)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSufficient) IncomeGreen else ExpenseRed
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedAccounts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { selectedAccountIds.clear() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Limpiar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Button(
                        onClick = { showAccountPickerSheet = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                    ) {
                        Text("Listo", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showSingleConfirmDialog && invoiceWithItems != null && selectedAccounts.size == 1) {
        val onlyAcc = selectedAccounts.first()
        val afterPay = onlyAcc.saldoActual - invoiceTotal
        val normalizedAfterPay = if (abs(afterPay) < 0.005) 0.0 else afterPay
        AlertDialog(
            onDismissRequest = { showSingleConfirmDialog = false },
            title = {
                Text("Confirmar pago", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Se descontará ${CurrencyFormatter.formatDual(invoiceTotal, bcvRate)} de la cuenta \"${onlyAcc.cuenta.nombre}\".",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Saldo disponible luego de pagar: ${CurrencyFormatter.formatDual(normalizedAfterPay, bcvRate)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val allocations = listOf(PaymentAllocation(onlyAcc.cuenta.id, invoiceTotal))
                        viewModel.markAsPaid(allocations) {
                            showSingleConfirmDialog = false
                            selectedAccountIds.clear()
                        }
                    }
                ) {
                    Text("Confirmar pago", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleConfirmDialog = false }) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showSplitDialog && invoiceWithItems != null && selectedAccounts.size >= 2) {
        val parsedAmounts = selectedAccounts.associate { acc ->
            val raw = splitAmountTexts[acc.cuenta.id]?.replace(',', '.')?.toDoubleOrNull()
            acc.cuenta.id to raw
        }
        val allParsedValid = selectedAccounts.all { acc ->
            val v = parsedAmounts[acc.cuenta.id]
            v != null && v >= 0.0 && v <= acc.saldoActual + 0.001
        }
        val allocatedSum = selectedAccounts.sumOf { parsedAmounts[it.cuenta.id] ?: 0.0 }
        val matchesTotal = abs(allocatedSum - invoiceTotal) <= 0.02

        AlertDialog(
            onDismissRequest = { showSplitDialog = false },
            title = {
                Text("Distribuir pago", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Indica cuánto descontar de cada cuenta (Total: ${CurrencyFormatter.formatDual(invoiceTotal, bcvRate)})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    selectedAccounts.forEach { acc ->
                        val textVal = splitAmountTexts[acc.cuenta.id] ?: ""
                        val parsed = parsedAmounts[acc.cuenta.id]
                        val exceedsBalance = parsed != null && parsed > acc.saldoActual + 0.001
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = acc.cuenta.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Disponible: ${CurrencyFormatter.formatDual(acc.saldoActual, bcvRate)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = textVal,
                                onValueChange = { splitAmountTexts[acc.cuenta.id] = it },
                                label = { Text("Monto a descontar (USD)") },
                                isError = exceedsBalance,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (exceedsBalance) {
                                Text(
                                    text = "Supera el saldo disponible",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Asignado: ${CurrencyFormatter.formatUsd(allocatedSum)} / ${CurrencyFormatter.formatUsd(invoiceTotal)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (matchesTotal && allParsedValid) IncomeGreen else ExpenseRed
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val allocations = selectedAccounts.mapNotNull { acc ->
                            val v = parsedAmounts[acc.cuenta.id] ?: 0.0
                            if (v > 0.005) PaymentAllocation(acc.cuenta.id, v) else null
                        }
                        viewModel.markAsPaid(allocations) {
                            showSplitDialog = false
                            selectedAccountIds.clear()
                        }
                    },
                    enabled = allParsedValid && matchesTotal
                ) {
                    Text("Confirmar pago", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSplitDialog = false }) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun InvoiceItemCard(
    item: ItemFactura,
    bcvRate: Double?,
    isDraft: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subtotal = item.precioUnitario * item.cantidad
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.nombre ?: "Artículo sin nombre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${CurrencyFormatter.formatDualBsFirst(item.precioUnitario, bcvRate)} × ${item.cantidad}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DualCurrencyText(
                    amountUsd = subtotal,
                    bcvRate = bcvRate,
                    primaryStyle = MaterialTheme.typography.titleLarge,
                    horizontalAlignment = Alignment.Start
                )
            }

            if (isDraft) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onDecrement,
                            enabled = item.cantidad > 1,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Disminuir cantidad")
                        }
                        Text(
                            text = "${item.cantidad}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        FilledTonalButton(
                            onClick = onIncrement,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Aumentar cantidad")
                        }
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar precio o cantidad")
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar artículo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditInvoiceItemDialog(
    item: ItemFactura,
    bcvRate: Double?,
    onDismiss: () -> Unit,
    onSave: (nombre: String?, precioUsd: Double, cantidad: Int) -> Unit
) {
    val rateAvailable = bcvRate != null && bcvRate > 0.0
    var itemCurrency by remember { mutableStateOf("USD") }
    var nombre by remember { mutableStateOf(item.nombre ?: "") }
    var precioText by remember { mutableStateOf(String.format(Locale.US, "%.2f", item.precioUnitario)) }
    var qtyText by remember { mutableStateOf(item.cantidad.toString()) }

    val rawPrecio = precioText.toDoubleOrNull()
    val precioUsd = when {
        rawPrecio == null -> null
        itemCurrency == "BS" && rateAvailable -> rawPrecio / bcvRate!!
        else -> rawPrecio
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar artículo", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CurrencySegmentedSelector(
                    selectedCurrency = itemCurrency,
                    onSelectCurrency = { newCurrency ->
                        itemCurrency = newCurrency
                        val currentUsd = precioUsd ?: item.precioUnitario
                        precioText = if (newCurrency == "BS" && rateAvailable) {
                            String.format(Locale.US, "%.2f", currentUsd * bcvRate!!)
                        } else {
                            String.format(Locale.US, "%.2f", currentUsd)
                        }
                    },
                    bcvRateAvailable = rateAvailable
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precioText,
                    onValueChange = { precioText = it },
                    label = { Text("Precio unitario ($itemCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (precioUsd != null && itemCurrency == "BS") {
                    Text(
                        text = "Equivale a ${CurrencyFormatter.formatUsd(precioUsd)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val qty = qtyText.toIntOrNull()
            TextButton(
                onClick = {
                    if (precioUsd != null && qty != null && qty > 0) {
                        onSave(nombre, precioUsd, qty)
                    }
                },
                enabled = precioUsd != null && precioUsd >= 0.0 && qty != null && qty > 0
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
