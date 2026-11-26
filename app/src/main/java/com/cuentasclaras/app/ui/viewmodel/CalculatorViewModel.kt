package com.cuentasclaras.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cuentasclaras.app.data.local.CalculatorTempItem
import com.cuentasclaras.app.data.local.dao.CuentaConSaldo
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
import kotlin.math.round

@Immutable
data class CalculatorUiState(
    val items: List<CalculatorTempItem> = emptyList(),
    val totalAccumulatedUsd: Double = 0.0,
    val accounts: List<CuentaConSaldo> = emptyList(),
    val totalAvailableBalanceUsd: Double = 0.0,
    val bcvRate: BcvRateInfo? = null,
    val bcvHistory: List<BcvRateHistoryEntry> = emptyList(),
    val isManualBcv: Boolean = false,
    val isRefreshingRate: Boolean = false,
    val defaultCurrency: String = "USD"
)

class CalculatorViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val isRefreshingRate = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<CalculatorUiState> = combine(
        combine(
            repository.calculatorItems,
            repository.observeAccounts()
        ) { items, accounts -> items to accounts },
        combine(
            repository.bcvRate,
            repository.bcvHistory,
            isRefreshingRate,
            repository.settings
        ) { rate, history, refreshing, settings ->
            Quad(rate, history, refreshing, settings)
        }
    ) { (items, accounts), meta ->
        val sum = items.sumOf { it.subtotalUsd }
        val totalAvailable = accounts.sumOf { it.saldoActual }
        CalculatorUiState(
            items = items,
            totalAccumulatedUsd = round(sum * 100.0) / 100.0,
            accounts = accounts,
            totalAvailableBalanceUsd = round(totalAvailable * 100.0) / 100.0,
            bcvRate = meta.first,
            bcvHistory = meta.second,
            isManualBcv = !meta.fourth.autoRefreshBcv,
            isRefreshingRate = meta.third,
            defaultCurrency = meta.fourth.defaultCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalculatorUiState(
            items = repository.calculatorItems.value,
            totalAccumulatedUsd = round(repository.calculatorItems.value.sumOf { it.subtotalUsd } * 100.0) / 100.0,
            bcvRate = repository.bcvRate.value,
            bcvHistory = repository.bcvHistory.value,
            isManualBcv = !repository.settings.value.autoRefreshBcv,
            defaultCurrency = repository.settings.value.defaultCurrency
        )
    )

    fun addTempItem(unitUsd: Double, quantity: Int) {
        if (unitUsd <= 0.0 || quantity < 1) return
        repository.addCalculatorItem(unitUsd, quantity)
    }

    fun removeTempItem(itemId: Long) {
        repository.removeCalculatorItem(itemId)
    }

    fun clearItems() {
        repository.clearCalculatorItems()
    }

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

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(repository) as T
        }
    }
}
