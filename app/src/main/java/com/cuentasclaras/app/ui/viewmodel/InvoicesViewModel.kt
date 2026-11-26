package com.cuentasclaras.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cuentasclaras.app.data.local.dao.FacturaConItems
import com.cuentasclaras.app.data.local.entity.Factura
import com.cuentasclaras.app.data.remote.BcvRateHistoryEntry
import com.cuentasclaras.app.data.remote.BcvRateInfo
import com.cuentasclaras.app.data.repository.FinanceRepository
import com.cuentasclaras.app.util.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class InvoicesUiState(
    val invoices: List<FacturaConItems> = emptyList(),
    val bcvRate: BcvRateInfo? = null,
    val bcvHistory: List<BcvRateHistoryEntry> = emptyList(),
    val isManualBcv: Boolean = false,
    val isRefreshingRate: Boolean = false
)

class InvoicesViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val isRefreshingRate = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<InvoicesUiState> = combine(
        repository.observeInvoices(),
        repository.bcvRate,
        repository.bcvHistory,
        isRefreshingRate,
        repository.settings
    ) { invoices, rate, history, refreshing, settings ->
        InvoicesUiState(
            invoices = invoices,
            bcvRate = rate,
            bcvHistory = history,
            isManualBcv = !settings.autoRefreshBcv,
            isRefreshingRate = refreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvoicesUiState(
            bcvRate = repository.bcvRate.value,
            bcvHistory = repository.bcvHistory.value,
            isManualBcv = !repository.settings.value.autoRefreshBcv
        )
    )

    fun refreshBcvRate() {
        viewModelScope.launch {
            isRefreshingRate.value = true
            when (val result = repository.refreshBcvRate()) {
                is AppResult.Success -> Unit
                is AppResult.Error -> _events.emit("Error al actualizar tasa BCV (${result.message})")
            }
            isRefreshingRate.value = false
        }
    }

    fun createNewInvoice(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createEmptyInvoice()
            onCreated(id)
        }
    }

    fun deleteInvoice(factura: Factura, refundMoney: Boolean) {
        viewModelScope.launch {
            repository.deleteInvoice(factura, refundMoney)
            _events.emit("Factura eliminada")
        }
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoicesViewModel(repository) as T
        }
    }
}
