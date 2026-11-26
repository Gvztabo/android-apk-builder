package com.cuentasclaras.app.data.local

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.round

@Immutable
data class AppSettings(
    val isDarkTheme: Boolean = false,
    val largeText: Boolean = false,
    val defaultCurrency: String = "USD",
    val autoRefreshBcv: Boolean = true,
    val manualBcvRate: Double? = null
)

@Immutable
data class CalculatorTempItem(
    val id: Long,
    val unitUsd: Double,
    val quantity: Int,
    val subtotalUsd: Double
)

@Immutable
data class InvoiceDraftUiState(
    val showAddForm: Boolean = false,
    val itemName: String = "",
    val itemPriceText: String = "",
    val itemQty: Int = 1,
    val itemCurrency: String = "USD",
    val selectedAccountIds: Set<Long> = emptySet()
)

class AppSettingsService(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "cuentas_claras_settings",
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _calculatorItems = MutableStateFlow(loadCalculatorItems())
    val calculatorItems: StateFlow<List<CalculatorTempItem>> = _calculatorItems.asStateFlow()

    private fun loadSettings(): AppSettings {
        val currency = prefs.getString(KEY_DEFAULT_CURRENCY, "USD")
        val manualBits = prefs.getLong(
            KEY_MANUAL_BCV_RATE_BITS,
            java.lang.Double.doubleToRawLongBits(-1.0)
        )
        val rawManual = java.lang.Double.longBitsToDouble(manualBits)
        val manualRate = if (rawManual.isFinite() && rawManual > 0.0) rawManual else null
        return AppSettings(
            isDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false),
            largeText = prefs.getBoolean(KEY_LARGE_TEXT, false),
            defaultCurrency = if (currency == "BS") "BS" else "USD",
            autoRefreshBcv = prefs.getBoolean(KEY_AUTO_REFRESH_BCV, true),
            manualBcvRate = manualRate
        )
    }

    private fun loadCalculatorItems(): List<CalculatorTempItem> {
        val raw = prefs.getString(KEY_CALCULATOR_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optLong("id", 0L)
                    val unitUsd = obj.optDouble("unitUsd", 0.0)
                    val quantity = obj.optInt("quantity", 1).coerceAtLeast(1)
                    val subtotalUsd = obj.optDouble("subtotalUsd", unitUsd * quantity)
                    if (id > 0L && unitUsd >= 0.0) {
                        add(
                            CalculatorTempItem(
                                id = id,
                                unitUsd = unitUsd,
                                quantity = quantity,
                                subtotalUsd = subtotalUsd
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCalculatorItems(items: List<CalculatorTempItem>, nextId: Long? = null) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("unitUsd", item.unitUsd)
                put("quantity", item.quantity)
                put("subtotalUsd", item.subtotalUsd)
            }
            array.put(obj)
        }
        val editor = prefs.edit().putString(KEY_CALCULATOR_ITEMS, array.toString())
        if (nextId != null) {
            editor.putLong(KEY_CALCULATOR_NEXT_ID, nextId)
        }
        editor.apply()
        _calculatorItems.value = items
    }

    fun addCalculatorItem(unitUsd: Double, quantity: Int) {
        if (unitUsd <= 0.0 || quantity < 1) return
        val nextId = prefs.getLong(KEY_CALCULATOR_NEXT_ID, 1L)
        val cleanUnit = round(unitUsd * 10000.0) / 10000.0
        val cleanSubtotal = round(cleanUnit * quantity * 100.0) / 100.0
        val newItem = CalculatorTempItem(
            id = nextId,
            unitUsd = cleanUnit,
            quantity = quantity,
            subtotalUsd = cleanSubtotal
        )
        saveCalculatorItems(listOf(newItem) + _calculatorItems.value, nextId = nextId + 1L)
    }

    fun removeCalculatorItem(itemId: Long) {
        val next = _calculatorItems.value.filterNot { it.id == itemId }
        saveCalculatorItems(next)
    }

    fun clearCalculatorItems() {
        saveCalculatorItems(emptyList(), nextId = 1L)
    }

    private fun loadInvoiceDraftMap(): JSONObject {
        val raw = prefs.getString(KEY_INVOICE_DRAFTS, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun getInvoiceDraftUiState(facturaId: Long): InvoiceDraftUiState {
        if (facturaId <= 0L) return InvoiceDraftUiState(itemCurrency = _settings.value.defaultCurrency)
        val root = loadInvoiceDraftMap()
        val obj = root.optJSONObject(facturaId.toString())
            ?: return InvoiceDraftUiState(itemCurrency = _settings.value.defaultCurrency)
        val selectedIds = buildSet {
            val arr = obj.optJSONArray("selectedAccountIds")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val id = arr.optLong(i, 0L)
                    if (id > 0L) add(id)
                }
            }
        }
        val rawCurrency = obj.optString("itemCurrency", _settings.value.defaultCurrency)
        return InvoiceDraftUiState(
            showAddForm = obj.optBoolean("showAddForm", false),
            itemName = obj.optString("itemName", ""),
            itemPriceText = obj.optString("itemPriceText", ""),
            itemQty = obj.optInt("itemQty", 1).coerceAtLeast(1),
            itemCurrency = if (rawCurrency == "BS") "BS" else "USD",
            selectedAccountIds = selectedIds
        )
    }

    fun saveInvoiceDraftUiState(facturaId: Long, state: InvoiceDraftUiState) {
        if (facturaId <= 0L) return
        val root = loadInvoiceDraftMap()
        val isEmptyState = !state.showAddForm &&
            state.itemName.isEmpty() &&
            state.itemPriceText.isEmpty() &&
            state.itemQty == 1 &&
            state.selectedAccountIds.isEmpty()
        if (isEmptyState) {
            root.remove(facturaId.toString())
        } else {
            val accArray = JSONArray()
            state.selectedAccountIds.forEach { accArray.put(it) }
            val obj = JSONObject().apply {
                put("showAddForm", state.showAddForm)
                put("itemName", state.itemName)
                put("itemPriceText", state.itemPriceText)
                put("itemQty", state.itemQty.coerceAtLeast(1))
                put("itemCurrency", if (state.itemCurrency == "BS") "BS" else "USD")
                put("selectedAccountIds", accArray)
            }
            root.put(facturaId.toString(), obj)
        }
        prefs.edit().putString(KEY_INVOICE_DRAFTS, root.toString()).apply()
    }

    fun clearInvoiceDraftUiState(facturaId: Long) {
        if (facturaId <= 0L) return
        val root = loadInvoiceDraftMap()
        if (root.has(facturaId.toString())) {
            root.remove(facturaId.toString())
            prefs.edit().putString(KEY_INVOICE_DRAFTS, root.toString()).apply()
        }
    }

    fun clearAllInvoiceDraftUiStates() {
        prefs.edit().remove(KEY_INVOICE_DRAFTS).apply()
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _settings.value = _settings.value.copy(isDarkTheme = enabled)
    }

    fun setLargeText(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply()
        _settings.value = _settings.value.copy(largeText = enabled)
    }

    fun setDefaultCurrency(currency: String) {
        val clean = if (currency == "BS") "BS" else "USD"
        prefs.edit().putString(KEY_DEFAULT_CURRENCY, clean).apply()
        _settings.value = _settings.value.copy(defaultCurrency = clean)
    }

    fun setAutoRefreshBcv(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH_BCV, enabled).apply()
        _settings.value = _settings.value.copy(autoRefreshBcv = enabled)
    }

    fun setManualBcvRate(rate: Double?) {
        val clean = rate?.takeIf { it.isFinite() && it > 0.0 }
        val editor = prefs.edit()
        if (clean != null) {
            editor.putLong(KEY_MANUAL_BCV_RATE_BITS, java.lang.Double.doubleToRawLongBits(clean))
        } else {
            editor.remove(KEY_MANUAL_BCV_RATE_BITS)
        }
        editor.apply()
        _settings.value = _settings.value.copy(manualBcvRate = clean)
    }

    companion object {
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_LARGE_TEXT = "large_text"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_AUTO_REFRESH_BCV = "auto_refresh_bcv"
        private const val KEY_MANUAL_BCV_RATE_BITS = "manual_bcv_rate_bits"
        private const val KEY_CALCULATOR_ITEMS = "calculator_items_json"
        private const val KEY_CALCULATOR_NEXT_ID = "calculator_next_id"
        private const val KEY_INVOICE_DRAFTS = "invoice_drafts_ui_json"
    }
}
