package com.cuentasclaras.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cuentasclaras.app.data.local.dao.CuentaConSaldo
import com.cuentasclaras.app.data.local.entity.TipoTransaccion
import com.cuentasclaras.app.data.local.entity.Transaccion
import com.cuentasclaras.app.data.remote.BcvRateInfo
import com.cuentasclaras.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountDetailUiState(
    val account: CuentaConSaldo? = null,
    val transactions: List<Transaccion> = emptyList(),
    val bcvRate: BcvRateInfo? = null,
    val defaultCurrency: String = "USD"
)

class AccountDetailViewModel(
    private val cuentaId: Long,
    private val repository: FinanceRepository
) : ViewModel() {

    val uiState: StateFlow<AccountDetailUiState> = combine(
        repository.observeAccount(cuentaId),
        repository.observeTransactions(cuentaId),
        repository.bcvRate,
        repository.settings
    ) { account, transactions, rate, settings ->
        AccountDetailUiState(
            account = account,
            transactions = transactions,
            bcvRate = rate,
            defaultCurrency = settings.defaultCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountDetailUiState(
            bcvRate = repository.bcvRate.value,
            defaultCurrency = repository.settings.value.defaultCurrency
        )
    )

    fun addTransaction(montoUsd: Double, tipo: TipoTransaccion, concepto: String?) {
        if (montoUsd <= 0.0) return
        viewModelScope.launch {
            repository.addTransaction(
                cuentaId = cuentaId,
                monto = montoUsd,
                tipo = tipo,
                concepto = concepto
            )
        }
    }

    fun adjustBalance(desiredBalanceUsd: Double, note: String? = null) {
        viewModelScope.launch {
            repository.adjustBalance(
                cuentaId = cuentaId,
                desiredBalanceUsd = desiredBalanceUsd,
                note = note
            )
        }
    }

    class Factory(
        private val cuentaId: Long,
        private val repository: FinanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountDetailViewModel(cuentaId, repository) as T
        }
    }
}
