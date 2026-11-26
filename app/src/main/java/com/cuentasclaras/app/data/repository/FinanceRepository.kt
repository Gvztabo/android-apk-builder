package com.cuentasclaras.app.data.repository

import androidx.room.withTransaction
import com.cuentasclaras.app.data.local.AppDatabase
import com.cuentasclaras.app.data.local.AppSettings
import com.cuentasclaras.app.data.local.AppSettingsService
import com.cuentasclaras.app.data.local.CalculatorTempItem
import com.cuentasclaras.app.data.local.InvoiceDraftUiState
import com.cuentasclaras.app.data.local.dao.CuentaConSaldo
import com.cuentasclaras.app.data.local.dao.FacturaConItems
import com.cuentasclaras.app.data.local.entity.Cuenta
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.Factura
import com.cuentasclaras.app.data.local.entity.ItemFactura
import com.cuentasclaras.app.data.local.entity.TipoTransaccion
import com.cuentasclaras.app.data.local.entity.Transaccion
import com.cuentasclaras.app.data.remote.BcvRateHistoryEntry
import com.cuentasclaras.app.data.remote.BcvRateInfo
import com.cuentasclaras.app.data.remote.BcvRateService
import com.cuentasclaras.app.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class PaymentAllocation(
    val cuentaId: Long,
    val amountUsd: Double
)

class FinanceRepository(
    private val database: AppDatabase,
    private val bcvRateService: BcvRateService,
    private val settingsService: AppSettingsService
) {
    private val cuentaDao = database.cuentaDao()
    private val transaccionDao = database.transaccionDao()
    private val facturaDao = database.facturaDao()

    val settings: StateFlow<AppSettings> = settingsService.settings

    val calculatorItems: StateFlow<List<CalculatorTempItem>> = settingsService.calculatorItems

    private val _capturedBcvRate = MutableStateFlow(bcvRateService.getCachedRate())

    private fun computeEffectiveBcvRate(
        captured: BcvRateInfo?,
        currentSettings: AppSettings
    ): BcvRateInfo? {
        val manualRate = currentSettings.manualBcvRate
        return if (!currentSettings.autoRefreshBcv && manualRate != null && manualRate > 0.0) {
            BcvRateInfo(
                usdRate = manualRate,
                fechaValor = captured?.fechaValor.orEmpty(),
                actualizadoEl = captured?.actualizadoEl.orEmpty(),
                fetchedAtMillis = captured?.fetchedAtMillis ?: System.currentTimeMillis()
            )
        } else {
            captured
        }
    }

    private fun syncEffectiveBcvRate() {
        _bcvRate.value = computeEffectiveBcvRate(_capturedBcvRate.value, settings.value)
    }

    private val _bcvRate = MutableStateFlow(
        computeEffectiveBcvRate(_capturedBcvRate.value, settings.value)
    )
    val bcvRate: StateFlow<BcvRateInfo?> = _bcvRate.asStateFlow()

    private val _bcvHistory = MutableStateFlow(bcvRateService.getCachedHistory())
    val bcvHistory: StateFlow<List<BcvRateHistoryEntry>> = _bcvHistory.asStateFlow()

    fun setDarkTheme(enabled: Boolean) = settingsService.setDarkTheme(enabled)

    fun setLargeText(enabled: Boolean) = settingsService.setLargeText(enabled)

    fun setDefaultCurrency(currency: String) = settingsService.setDefaultCurrency(currency)

    fun setAutoRefreshBcv(enabled: Boolean) {
        if (!enabled && settings.value.manualBcvRate == null) {
            _capturedBcvRate.value?.usdRate?.takeIf { it > 0.0 }?.let { captured ->
                settingsService.setManualBcvRate(captured)
            }
        }
        settingsService.setAutoRefreshBcv(enabled)
        syncEffectiveBcvRate()
    }

    fun setManualBcvRate(rate: Double?) {
        settingsService.setManualBcvRate(rate)
        syncEffectiveBcvRate()
    }

    fun addCalculatorItem(unitUsd: Double, quantity: Int) =
        settingsService.addCalculatorItem(unitUsd, quantity)

    fun removeCalculatorItem(itemId: Long) =
        settingsService.removeCalculatorItem(itemId)

    fun clearCalculatorItems() =
        settingsService.clearCalculatorItems()

    fun getInvoiceDraftUiState(facturaId: Long): InvoiceDraftUiState =
        settingsService.getInvoiceDraftUiState(facturaId)

    fun saveInvoiceDraftUiState(facturaId: Long, state: InvoiceDraftUiState) =
        settingsService.saveInvoiceDraftUiState(facturaId, state)

    fun clearInvoiceDraftUiState(facturaId: Long) =
        settingsService.clearInvoiceDraftUiState(facturaId)

    suspend fun refreshBcvRate(): AppResult<BcvRateInfo> {
        return when (val result = bcvRateService.fetchLatestRate()) {
            is AppResult.Success -> {
                _capturedBcvRate.value = result.data
                _bcvHistory.value = bcvRateService.getCachedHistory()
                if (!settings.value.autoRefreshBcv) {
                    settingsService.setAutoRefreshBcv(true)
                }
                syncEffectiveBcvRate()
                result
            }
            is AppResult.Error -> result
        }
    }

    suspend fun refreshBcvRateIfStaleForToday() {
        if (bcvRateService.isRateStaleForToday()) {
            refreshBcvRate()
        }
    }

    fun observeAccounts(): Flow<List<CuentaConSaldo>> = cuentaDao.observeCuentasConSaldo()

    fun observeAccountCount(): Flow<Int> = cuentaDao.observeAccountCount()

    fun observeAccount(cuentaId: Long): Flow<CuentaConSaldo?> =
        cuentaDao.observeCuentaConSaldo(cuentaId)

    fun observeTransactions(cuentaId: Long): Flow<List<Transaccion>> =
        transaccionDao.observeTransaccionesByCuenta(cuentaId)

    fun observeInvoiceCount(): Flow<Int> = facturaDao.observeInvoiceCount()

    suspend fun saveAccount(account: Cuenta): Long {
        return if (account.id == 0L) {
            cuentaDao.insertCuenta(account)
        } else {
            cuentaDao.updateCuenta(account)
            account.id
        }
    }

    suspend fun deleteAccount(account: Cuenta) {
        cuentaDao.deleteCuenta(account)
    }

    suspend fun addTransaction(
        cuentaId: Long,
        monto: Double,
        tipo: TipoTransaccion,
        concepto: String?
    ) {
        require(tipo != TipoTransaccion.AJUSTE) { "Usa adjustBalance() para transacciones AJUSTE" }
        val cleanConcepto = concepto?.trim()?.takeIf { it.isNotEmpty() }
        transaccionDao.insertTransaccion(
            Transaccion(
                cuentaId = cuentaId,
                monto = abs(monto),
                tipo = tipo,
                concepto = cleanConcepto,
                fecha = System.currentTimeMillis()
            )
        )
    }

    suspend fun adjustBalance(
        cuentaId: Long,
        desiredBalanceUsd: Double,
        note: String? = null
    ): Boolean {
        return database.withTransaction {
            val currentBalance = cuentaDao.getSaldoActual(cuentaId) ?: return@withTransaction false
            val delta = desiredBalanceUsd - currentBalance
            if (abs(delta) < 0.005) return@withTransaction false
            val concepto = note?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Ajuste manual de saldo"
            transaccionDao.insertTransaccion(
                Transaccion(
                    cuentaId = cuentaId,
                    monto = delta,
                    tipo = TipoTransaccion.AJUSTE,
                    concepto = concepto,
                    fecha = System.currentTimeMillis()
                )
            )
            true
        }
    }

    fun observeInvoices(): Flow<List<FacturaConItems>> = facturaDao.observeFacturasConItems()

    fun observeInvoice(facturaId: Long): Flow<FacturaConItems?> =
        facturaDao.observeFacturaConItems(facturaId)

    suspend fun createEmptyInvoice(): Long {
        return facturaDao.createSequentialInvoice(fecha = System.currentTimeMillis())
    }

    suspend fun renameInvoice(facturaId: Long, newName: String) {
        database.withTransaction {
            val factura = facturaDao.getFacturaById(facturaId) ?: return@withTransaction
            val finalName = newName.trim().ifBlank { "Factura #${factura.numeroFactura}" }
            facturaDao.updateFacturaNombre(facturaId, finalName)
            transaccionDao.updateConceptoByFacturaId(facturaId, "Pago de $finalName")
        }
    }

    suspend fun deleteInvoice(factura: Factura, refundMoney: Boolean) {
        database.withTransaction {
            if (refundMoney && factura.estado == EstadoFactura.PAGADA) {
                transaccionDao.deleteByFacturaId(factura.id)
            }
            facturaDao.deleteFactura(factura)
        }
        settingsService.clearInvoiceDraftUiState(factura.id)
    }

    suspend fun addInvoiceItem(
        facturaId: Long,
        nombre: String?,
        precioUnitario: Double,
        cantidad: Int
    ) {
        facturaDao.addItemAndRecalculate(
            ItemFactura(
                facturaId = facturaId,
                nombre = nombre?.trim()?.takeIf { it.isNotEmpty() },
                precioUnitario = precioUnitario.coerceAtLeast(0.0),
                cantidad = cantidad.coerceAtLeast(1)
            )
        )
    }

    suspend fun updateInvoiceItem(item: ItemFactura) {
        facturaDao.updateItemAndRecalculate(
            item.copy(
                nombre = item.nombre?.trim()?.takeIf { it.isNotEmpty() },
                precioUnitario = item.precioUnitario.coerceAtLeast(0.0),
                cantidad = item.cantidad.coerceAtLeast(1)
            )
        )
    }

    suspend fun deleteInvoiceItem(item: ItemFactura) {
        facturaDao.deleteItemAndRecalculate(item)
    }

    suspend fun markInvoiceAsPaid(
        facturaId: Long,
        allocations: List<PaymentAllocation>
    ): AppResult<Unit> {
        return try {
            database.withTransaction {
                val factura = facturaDao.getFacturaById(facturaId)
                    ?: throw IllegalStateException("Factura no encontrada")
                val total = facturaDao.calculateFacturaTotal(facturaId)
                val validAllocations = allocations.filter { it.amountUsd > 0.005 }
                val allocatedSum = validAllocations.sumOf { it.amountUsd }
                if (abs(allocatedSum - total) > 0.02) {
                    throw IllegalStateException("El monto asignado debe coincidir con el total de la factura.")
                }

                val now = System.currentTimeMillis()
                validAllocations.forEach { allocation ->
                    transaccionDao.insertTransaccion(
                        Transaccion(
                            cuentaId = allocation.cuentaId,
                            facturaId = facturaId,
                            monto = allocation.amountUsd,
                            tipo = TipoTransaccion.GASTO,
                            concepto = "Pago de ${factura.displayName()}",
                            fecha = now
                        )
                    )
                }
                facturaDao.updateFacturaTotal(facturaId, total)
                facturaDao.updateFacturaEstado(facturaId, EstadoFactura.PAGADA)
            }
            settingsService.clearInvoiceDraftUiState(facturaId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "No se pudo completar el pago", e)
        }
    }

    suspend fun clearAllData() {
        database.withTransaction {
            transaccionDao.deleteAll()
            facturaDao.deleteAllItems()
            facturaDao.deleteAllFacturas()
            cuentaDao.deleteAll()
            try {
                database.openHelper.writableDatabase.execSQL("DELETE FROM sqlite_sequence")
            } catch (_: Exception) {
                // Ignore if sqlite_sequence is not yet created
            }
        }
        settingsService.clearCalculatorItems()
        settingsService.clearAllInvoiceDraftUiStates()
    }

    suspend fun loadSampleData() {
        database.withTransaction {
            val now = System.currentTimeMillis()
            val cashId = cuentaDao.insertCuenta(
                Cuenta(nombre = "Efectivo USD", saldoInicial = 120.0, moneda = "USD")
            )
            val bankId = cuentaDao.insertCuenta(
                Cuenta(nombre = "Banesco (BS)", saldoInicial = 45.0, moneda = "BS")
            )
            cuentaDao.insertCuenta(
                Cuenta(nombre = "Zelle Personal", saldoInicial = 280.0, moneda = "USD")
            )

            transaccionDao.insertTransaccion(
                Transaccion(
                    cuentaId = cashId,
                    monto = 35.0,
                    tipo = TipoTransaccion.INGRESO,
                    concepto = "Freelance consultoría",
                    fecha = now - 1000L * 60 * 60 * 18
                )
            )
            transaccionDao.insertTransaccion(
                Transaccion(
                    cuentaId = cashId,
                    monto = 14.5,
                    tipo = TipoTransaccion.GASTO,
                    concepto = "Café y almuerzo",
                    fecha = now - 1000L * 60 * 60 * 6
                )
            )
            transaccionDao.insertTransaccion(
                Transaccion(
                    cuentaId = bankId,
                    monto = -5.0,
                    tipo = TipoTransaccion.AJUSTE,
                    concepto = "Comisión bancaria mensual",
                    fecha = now - 1000L * 60 * 60 * 3
                )
            )

            val invId = facturaDao.createSequentialInvoice(fecha = now - 1000L * 60 * 30)
            facturaDao.addItemAndRecalculate(
                ItemFactura(facturaId = invId, nombre = "Harina Pan 1kg", precioUnitario = 1.5, cantidad = 6)
            )
            facturaDao.addItemAndRecalculate(
                ItemFactura(facturaId = invId, nombre = "Aceite de oliva 500ml", precioUnitario = 12.5, cantidad = 2)
            )
            facturaDao.addItemAndRecalculate(
                ItemFactura(facturaId = invId, nombre = "Servicio de internet", precioUnitario = 50.5, cantidad = 1)
            )
        }
    }
}
