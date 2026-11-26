package com.cuentasclaras.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuentasclaras.app.data.local.InvoiceDraftUiState
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.ItemFactura
import com.cuentasclaras.app.data.repository.PaymentAllocation
import com.cuentasclaras.app.ui.components.CurrencySegmentedSelector
import com.cuentasclaras.app.ui.components.DeleteInvoiceConfirmDialog
import com.cuentasclaras.app.ui.components.DualCurrencyText
import com.cuentasclaras.app.ui.components.GroupedCardHeader
import com.cuentasclaras.app.ui.components.GroupedCardItemSlice
import com.cuentasclaras.app.ui.theme.AdjustAmber
import com.cuentasclaras.app.ui.theme.EmeraldPrimary
import com.cuentasclaras.app.ui.theme.ExpenseRed
import com.cuentasclaras.app.ui.theme.IncomeGreen
import com.cuentasclaras.app.ui.viewmodel.InvoiceEditorViewModel
import com.cuentasclaras.app.util.CurrencyFormatter
import com.cuentasclaras.app.util.VoiceProductParser
import kotlinx.coroutines.launch
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

    val initialDraft = remember(viewModel) { viewModel.getDraftUiState() }
    var showAddForm by remember { mutableStateOf(initialDraft.showAddForm) }
    var itemName by remember { mutableStateOf(initialDraft.itemName) }
    var itemPriceText by remember { mutableStateOf(initialDraft.itemPriceText) }
    var itemQty by remember { mutableIntStateOf(initialDraft.itemQty) }
    var itemCurrency by remember {
        mutableStateOf(
            if (initialDraft.itemCurrency == "BS" && rateAvailable) "BS" else "USD"
        )
    }
    var editingItem by remember { mutableStateOf<ItemFactura?>(null) }
    var showAccountPickerSheet by remember { mutableStateOf(false) }
    var showSingleConfirmDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    val splitAmountTexts = remember { mutableStateMapOf<Long, String>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val selectedAccountIds = remember(viewModel) {
        mutableStateMapOf<Long, Boolean>().apply {
            initialDraft.selectedAccountIds.forEach { put(it, true) }
        }
    }
    val selectedAccounts = uiState.accounts.filter { selectedAccountIds[it.cuenta.id] == true }
    val selectedAccountIdSet = selectedAccountIds.entries
        .filter { it.value }
        .map { it.key }
        .toSet()

    LaunchedEffect(
        isDraft,
        invoiceWithItems?.factura?.id,
        showAddForm,
        itemName,
        itemPriceText,
        itemQty,
        itemCurrency,
        selectedAccountIdSet
    ) {
        if (invoiceWithItems != null && isDraft) {
            viewModel.saveDraftUiState(
                InvoiceDraftUiState(
                    showAddForm = showAddForm,
                    itemName = itemName,
                    itemPriceText = itemPriceText,
                    itemQty = itemQty,
                    itemCurrency = itemCurrency,
                    selectedAccountIds = selectedAccountIdSet
                )
            )
        }
    }

    val latestIsDraft by rememberUpdatedState(isDraft)
    val latestHasInvoice by rememberUpdatedState(invoiceWithItems != null)

    DisposableEffect(Unit) {
        onDispose {
            if (latestHasInvoice && latestIsDraft) {
                viewModel.saveDraftUiState(
                    InvoiceDraftUiState(
                        showAddForm = showAddForm,
                        itemName = itemName,
                        itemPriceText = itemPriceText,
                        itemQty = itemQty,
                        itemCurrency = itemCurrency,
                        selectedAccountIds = selectedAccountIds.entries
                            .filter { it.value }
                            .map { it.key }
                            .toSet()
                    )
                )
            }
        }
    }
    val invoiceTotal = invoiceWithItems?.factura?.total ?: 0.0
    val selectedTotal = selectedAccounts.sumOf { it.saldoActual }
    val diffAfterPayRaw = selectedTotal - invoiceTotal
    val diffAfterPay = if (abs(diffAfterPayRaw) < 0.005) 0.0 else diffAfterPayRaw
    val isSufficient = selectedAccounts.isNotEmpty() && selectedTotal + 0.001 >= invoiceTotal

    val priceFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val startVoiceListening = remember(speechRecognizer, context, scope) {
        {
            if (speechRecognizer == null) {
                scope.launch {
                    snackbarHostState.showSnackbar("El reconocimiento de voz no está disponible en este dispositivo.")
                }
            } else {
                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }
                    override fun onBeginningOfSpeech() {
                        isListening = true
                    }
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() {
                        isListening = false
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        if (error != SpeechRecognizer.ERROR_CLIENT) {
                            scope.launch {
                                snackbarHostState.showSnackbar("No se pudo entender el dictado. Intenta de nuevo.")
                            }
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull().orEmpty()
                        if (spoken.isNotBlank()) {
                            val parsed = VoiceProductParser.parse(spoken)
                            itemName = parsed.name
                            itemPriceText = parsed.price?.let {
                                String.format(Locale.US, "%.2f", it)
                            } ?: ""
                            itemCurrency = "USD"
                            itemQty = 1
                            showAddForm = true
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-VE")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                isListening = true
                speechRecognizer.startListening(intent)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceListening()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Se necesita permiso de micrófono para dictar productos.")
            }
        }
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    LaunchedEffect(showAddForm) {
        if (showAddForm && itemPriceText.isBlank() && itemName.isBlank()) {
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
                        val factura = invoiceWithItems?.factura
                        if (factura != null) {
                            val formattedDate = remember(factura.id, factura.fecha) {
                                CurrencyFormatter.formatDate(factura.fecha)
                            }
                            Column {
                                Text(
                                    text = factura.displayName(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Factura #${factura.numeroFactura} ·",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = factura.estado.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (factura.estado == EstadoFactura.PAGADA) IncomeGreen else AdjustAmber
                                    )
                                    Text(
                                        text = "· $formattedDate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Factura",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { padding ->
        if (invoiceWithItems == null) {
            return@Scaffold
        }

        val isDark = MaterialTheme.colorScheme.background.red < 0.2f

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Total & Account Selection Card (Clean White/Dark Surface matching Web)
            item(key = "invoice_summary", contentType = "summary") {
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Total de la factura",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DualCurrencyText(
                                amountUsd = invoiceTotal,
                                bcvRate = bcvRate,
                                primaryStyle = MaterialTheme.typography.headlineMedium,
                                primaryColor = MaterialTheme.colorScheme.onSurface,
                                secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        if (isDraft) {
                            if (uiState.accounts.isEmpty()) {
                                Text(
                                    text = "Sin cuentas registradas",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                if (selectedAccounts.isEmpty()) {
                                    Button(
                                        onClick = { showAccountPickerSheet = true },
                                        enabled = invoiceTotal > 0.0,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Seleccionar cuentas", style = MaterialTheme.typography.titleMedium)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { showAccountPickerSheet = true },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF171918)
                                        ),
                                        border = BorderStroke(1.5.dp, Color(0xFFD0D5D3)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Seleccionar cuentas (${selectedAccounts.size})",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    Text(
                                        text = "Cuentas: ${selectedAccounts.joinToString(", ") { it.cuenta.nombre }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "Saldo disponible",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                DualCurrencyText(
                                                    amountUsd = selectedTotal,
                                                    bcvRate = bcvRate,
                                                    primaryStyle = MaterialTheme.typography.titleMedium,
                                                    primaryColor = MaterialTheme.colorScheme.onSurface,
                                                    secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    horizontalAlignment = Alignment.Start
                                                )
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "Saldo disponible luego de pagar",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                        secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                    ) {
                                        Text("Pagar", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Factura pagada", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add Item Button + Mic Voice Dictation Button (Draft Only)
            if (isDraft) {
                item(key = "add_item_section", contentType = "add_item") {
                    if (isListening) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(2.dp, EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Mic,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Escuchando...",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "Ej: \"Leche a dos con cincuenta\"",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        speechRecognizer?.stopListening()
                                        isListening = false
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ExpenseRed,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.heightIn(min = 56.dp)
                                ) {
                                    Icon(Icons.Outlined.Stop, contentDescription = "Detener", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    if (!showAddForm) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    itemCurrency = if (uiState.defaultCurrency == "BS" && rateAvailable) "BS" else "USD"
                                    showAddForm = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) EmeraldPrimary else Color(0xFF171918),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Añadir producto", style = MaterialTheme.typography.titleMedium)
                            }

                            Button(
                                onClick = {
                                    if (isListening) {
                                        speechRecognizer?.stopListening()
                                        isListening = false
                                    } else {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            startVoiceListening()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) ExpenseRed else EmeraldPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Outlined.Stop else Icons.Outlined.Mic,
                                    contentDescription = "Dictar producto por voz",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    } else {
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Añadir producto",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = { showAddForm = false }) {
                                        Text("Cerrar", style = MaterialTheme.typography.labelLarge)
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
                                    placeholder = { Text("0.00") },
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
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StepperIconButton(
                                            icon = Icons.Outlined.Remove,
                                            contentDescription = "Disminuir cantidad",
                                            enabled = itemQty > 1,
                                            onClick = { if (itemQty > 1) itemQty -= 1 }
                                        )
                                        Text(
                                            text = "$itemQty",
                                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(38.dp)
                                        )
                                        StepperIconButton(
                                            icon = Icons.Outlined.Add,
                                            contentDescription = "Aumentar cantidad",
                                            enabled = true,
                                            onClick = { itemQty += 1 }
                                        )
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Guardar", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Virtualized Grouped Line Items Header & Rows matching Web
            item(key = "products_header", contentType = "products_header") {
                val lineCount = invoiceWithItems.items.size
                GroupedCardHeader(
                    title = "Productos",
                    trailing = if (lineCount == 1) "1 línea" else "$lineCount líneas"
                )
            }

            if (invoiceWithItems.items.isEmpty()) {
                item(key = "empty_products", contentType = "empty") {
                    GroupedCardItemSlice(
                        isFirst = true,
                        isLast = true,
                        hasHeader = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "La factura está vacía",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Toca \"Añadir producto\" para agregar artículos a esta factura.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = invoiceWithItems.items,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "invoice_item_row" }
                ) { index, item ->
                    GroupedCardItemSlice(
                        isFirst = index == 0,
                        isLast = index == invoiceWithItems.items.lastIndex,
                        hasHeader = true
                    ) {
                        InvoiceItemRow(
                            item = item,
                            bcvRate = bcvRate,
                            isDraft = isDraft,
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
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Nombre de la factura", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Si lo dejas vacío, se guardará como \"Factura #${invoiceWithItems.factura.numeroFactura}\".",
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
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${invoiceWithItems.factura.displayName()} — Total: ${CurrencyFormatter.formatDual(invoiceTotal, bcvRate)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.accounts.isEmpty()) {
                    Text(
                        text = "Sin cuentas registradas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            uiState.accounts.forEachIndexed { index, acc ->
                                val isChecked = selectedAccountIds[acc.cuenta.id] == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAccountIds[acc.cuenta.id] = !isChecked
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = acc.cuenta.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatDual(acc.saldoActual, bcvRate),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (index < uiState.accounts.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                                }
                            }
                        }
                    }
                }

                if (selectedAccounts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedAccounts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { selectedAccountIds.clear() },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Limpiar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Button(
                        onClick = { showAccountPickerSheet = false },
                        shape = RoundedCornerShape(16.dp),
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Confirmar pago", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Se descontará ${CurrencyFormatter.formatDual(invoiceTotal, bcvRate)} de la cuenta \"${onlyAcc.cuenta.nombre}\".",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Saldo disponible luego de pagar: ${CurrencyFormatter.formatDual(normalizedAfterPay, bcvRate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
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
            containerColor = MaterialTheme.colorScheme.surface,
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

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
private fun StepperIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.9f else 0.45f),
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
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InvoiceItemRow(
    item: ItemFactura,
    bcvRate: Double?,
    isDraft: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subtotal = item.precioUnitario * item.cantidad
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.nombre ?: "Artículo sin nombre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${CurrencyFormatter.formatDualBsFirst(item.precioUnitario, bcvRate)} × ${item.cantidad}",
                style = MaterialTheme.typography.bodyMedium,
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
                    StepperIconButton(
                        icon = Icons.Outlined.Remove,
                        contentDescription = "Disminuir cantidad",
                        enabled = item.cantidad > 1,
                        onClick = onDecrement
                    )
                    Text(
                        text = "${item.cantidad}",
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(34.dp)
                    )
                    StepperIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Aumentar cantidad",
                        enabled = true,
                        onClick = onIncrement
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar precio o cantidad",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Eliminar artículo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

    val rawPrecio = precioText.replace(',', '.').toDoubleOrNull()
    val precioUsd = when {
        rawPrecio == null -> null
        itemCurrency == "BS" && rateAvailable -> rawPrecio / bcvRate!!
        else -> rawPrecio
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
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
            Button(
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
