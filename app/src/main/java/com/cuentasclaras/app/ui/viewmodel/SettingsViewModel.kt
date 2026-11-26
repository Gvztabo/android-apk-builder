package com.cuentasclaras.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cuentasclaras.app.data.local.AppSettings
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
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val bcvRate: BcvRateInfo? = null,
    val bcvHistory: List<BcvRateHistoryEntry> = emptyList(),
    val isRefreshingRate: Boolean = false,
    val accountsCount: Int = 0,
    val invoicesCount: Int = 0
)

class SettingsViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val isRefreshingRate = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            repository.settings,
            repository.bcvRate,
            repository.bcvHistory,
            ::Triple
        ),
        combine(
            isRefreshingRate,
            repository.observeAccountCount(),
            repository.observeInvoiceCount(),
            ::Triple
        )
    ) { left, right ->
        val (settings, rate, history) = left
        val (refreshing, accountsCount, invoicesCount) = right

        SettingsUiState(
            settings = settings,
            bcvRate = rate,
            bcvHistory = history,
            isRefreshingRate = refreshing,
            accountsCount = accountsCount,
            invoicesCount = invoicesCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            settings = repository.settings.value,
            bcvRate = repository.bcvRate.value,
            bcvHistory = repository.bcvHistory.value
        )
    )

    fun setDarkTheme(enabled: Boolean) {
        repository.setDarkTheme(enabled)
    }

    fun setLargeText(enabled: Boolean) {
        repository.setLargeText(enabled)
    }

    fun setDefaultCurrency(currency: String) {
        repository.setDefaultCurrency(currency)
    }

    fun setAutoRefreshBcv(enabled: Boolean) {
        repository.setAutoRefreshBcv(enabled)
    }

    fun setManualBcvRate(rate: Double?) {
        repository.setManualBcvRate(rate)
    }

    fun refreshBcvRate() {
        viewModelScope.launch {
            val wasManual = !repository.settings.value.autoRefreshBcv
            isRefreshingRate.value = true
            when (val result = repository.refreshBcvRate()) {
                is AppResult.Success -> _events.emit(
                    if (wasManual) "Tasa BCV actualizada. Modo automático activado." else "Tasa BCV actualizada"
                )
                is AppResult.Error -> _events.emit("Error al actualizar tasa BCV (${result.message})")
            }
            isRefreshingRate.value = false
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
            _events.emit("Datos de ejemplo cargados")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _events.emit("Todos los datos han sido eliminados")
        }
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
