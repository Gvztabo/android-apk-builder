package com.cuentasclaras.app.data.local

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class AppSettings(
    val isDarkTheme: Boolean = false,
    val largeText: Boolean = false,
    val defaultCurrency: String = "USD",
    val autoRefreshBcv: Boolean = true
)

class AppSettingsService(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "cuentas_claras_settings",
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val currency = prefs.getString(KEY_DEFAULT_CURRENCY, "USD")
        return AppSettings(
            isDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false),
            largeText = prefs.getBoolean(KEY_LARGE_TEXT, false),
            defaultCurrency = if (currency == "BS") "BS" else "USD",
            autoRefreshBcv = prefs.getBoolean(KEY_AUTO_REFRESH_BCV, true)
        )
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

    companion object {
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_LARGE_TEXT = "large_text"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_AUTO_REFRESH_BCV = "auto_refresh_bcv"
    }
}
