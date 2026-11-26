package com.cuentasclaras.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cuentasclaras.app.data.local.InvoiceDraftUiState
import com.cuentasclaras.app.data.local.dao.CuentaConSaldo
import com.cuentasclaras.app.data.local.dao.FacturaConItems
import com.cuentasclaras.app.data.local.entity.ItemFactura
import com.cuentasclaras.app.data.remote.BcvRateInfo
import com.cuentasclaras.app.data.repository.FinanceRepository
import com.cuentasclaras.app.data.repository.PaymentAllocation
import com.cuentasclaras.app.util.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class InvoiceEditorUiState(
    val invoiceWithItems: FacturaConItems? = null,
    val accounts: List<CuentaConSaldo> = emptyList(),
    val totalAvailableUsd: Double = 0.0,
    val bcvRate: BcvRateInfo? = null,
    val defaultCurrency: String = "USD"
)

class InvoiceEditorViewModel(
    private val facturaId: Long,
    private val repository: FinanceRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<InvoiceEditorUiState> = combine(
        repository.observeInvoice(facturaId),
        repository.observeAccounts(),
        repository.bcvRate,
        repository.settings
    ) { invoice, accounts, rate, settings ->
        InvoiceEditorUiState(
            invoiceWithItems = invoice,
            accounts = accounts,
            totalAvailableUsd = accounts.sumOf { it.saldoActual },
            bcvRate = rate,
            defaultCurrency = settings.defaultCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvoiceEditorUiState(
            bcvRate = repository.bcvRate.value,
            defaultCurrency = repository.settings.value.defaultCurrency
        )
    )

    fun getDraftUiState(): InvoiceDraftUiState =
        repository.getInvoiceDraftUiState(facturaId)

    fun saveDraftUiState(state: InvoiceDraftUiState) {
        repository.saveInvoiceDraftUiState(facturaId, state)
    }

    fun clearDraftUiState() {
        repository.clearInvoiceDraftUiState(facturaId)
    }

    fun addItem(nombre: String?, precioUnitarioUsd: Double, cantidad: Int) {
        if (precioUnitarioUsd < 0.0 || cantidad <= 0) return
        viewModelScope.launch {
            repository.addInvoiceItem(
                facturaId = facturaId,
                nombre = nombre,
                precioUnitario = precioUnitarioUsd,
                cantidad = cantidad
            )
        }
    }

    fun updateItemQuantity(item: ItemFactura, newQuantity: Int) {
        if (newQuantity <= 0) return
        viewModelScope.launch {
            repository.updateInvoiceItem(item.copy(cantidad = newQuantity))
        }
    }

    fun updateItemDetails(item: ItemFactura, nombre: String?, precioUnitarioUsd: Double, cantidad: Int) {
        if (precioUnitarioUsd < 0.0 || cantidad <= 0) return
        viewModelScope.launch {
            repository.updateInvoiceItem(
                item.copy(
                    nombre = nombre,
                    precioUnitario = precioUnitarioUsd,
                    cantidad = cantidad
                )
            )
        }
    }

    fun deleteItem(item: ItemFactura) {
        viewModelScope.launch {
            repository.deleteInvoiceItem(item)
        }
    }

    fun renameInvoice(newName: String) {
        viewModelScope.launch {
            repository.renameInvoice(facturaId, newName)
            _events.emit("Nombre de factura actualizado")
        }
    }

    fun deleteCurrentInvoice(refundMoney: Boolean, onDeleted: () -> Unit) {
        val factura = uiState.value.invoiceWithItems?.factura ?: return
        viewModelScope.launch {
            repository.deleteInvoice(factura, refundMoney)
            _events.emit("Factura eliminada")
            onDeleted()
        }
    }

    fun markAsPaid(allocations: List<PaymentAllocation>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.markInvoiceAsPaid(facturaId, allocations)) {
                is AppResult.Success -> {
                    _events.emit("Factura pagada")
                    onSuccess()
                }
                is AppResult.Error -> {
                    _events.emit(result.message)
                }
            }
        }
    }

    class Factory(
        private val facturaId: Long,
        private val repository: FinanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoiceEditorViewModel(facturaId, repository) as T
        }
    }
}
