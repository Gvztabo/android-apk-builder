package com.cuentasclaras.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cuentasclaras.app.data.local.dao.CuentaConSaldo
import com.cuentasclaras.app.data.local.entity.Cuenta
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
data class AccountsUiState(
    val accounts: List<CuentaConSaldo> = emptyList(),
    val totalBalanceUsd: Double = 0.0,
    val bcvRate: BcvRateInfo? = null,
    val bcvHistory: List<BcvRateHistoryEntry> = emptyList(),
    val isManualBcv: Boolean = false,
    val isRefreshingRate: Boolean = false,
    val defaultCurrency: String = "USD"
)

class AccountsViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val isRefreshingRate = MutableStateFlow(false)
    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val uiState: StateFlow<AccountsUiState> = combine(
        repository.observeAccounts(),
        repository.bcvRate,
        repository.bcvHistory,
        isRefreshingRate,
        repository.settings
    ) { accounts, rate, history, refreshing, settings ->
        AccountsUiState(
            accounts = accounts,
            totalBalanceUsd = accounts.sumOf { it.saldoActual },
            bcvRate = rate,
            bcvHistory = history,
            isManualBcv = !settings.autoRefreshBcv,
            isRefreshingRate = refreshing,
            defaultCurrency = settings.defaultCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountsUiState(
            bcvRate = repository.bcvRate.value,
            bcvHistory = repository.bcvHistory.value,
            isManualBcv = !repository.settings.value.autoRefreshBcv,
            defaultCurrency = repository.settings.value.defaultCurrency
        )
    )

    init {
        if (repository.settings.value.autoRefreshBcv) {
            viewModelScope.launch {
                repository.refreshBcvRateIfStaleForToday()
            }
        }
    }

    fun refreshBcvRate() {
        viewModelScope.launch {
            isRefreshingRate.value = true
            when (val result = repository.refreshBcvRate()) {
                is AppResult.Success -> Unit
                is AppResult.Error -> {
                    val hasCache = repository.bcvRate.value != null
                    val message = if (hasCache) {
                        "No se pudo actualizar la tasa BCV (${result.message}). Usando tasa en caché."
                    } else {
                        "Tasa BCV no disponible (${result.message}). Mostrando solo USD."
                    }
                    _snackbarMessages.emit(message)
                }
            }
            isRefreshingRate.value = false
        }
    }

    fun saveAccount(id: Long = 0L, nombre: String, saldoInicialUsd: Double, moneda: String) {
        viewModelScope.launch {
            repository.saveAccount(
                Cuenta(
                    id = id,
                    nombre = nombre.trim(),
                    saldoInicial = saldoInicialUsd,
                    moneda = moneda
                )
            )
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
            _snackbarMessages.emit("Datos de ejemplo cargados")
        }
    }

    fun deleteAccount(cuenta: Cuenta) {
        viewModelScope.launch {
            repository.deleteAccount(cuenta)
        }
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountsViewModel(repository) as T
        }
    }
}
