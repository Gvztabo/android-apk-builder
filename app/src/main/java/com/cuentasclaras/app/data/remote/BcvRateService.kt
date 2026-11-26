package com.cuentasclaras.app.data.remote

import android.content.Context
import com.cuentasclaras.app.util.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class BcvTrendDirection {
    UP,
    DOWN,
    SAME
}

data class BcvRateHistoryEntry(
    val usdRate: Double,
    val fechaValor: String,
    val actualizadoEl: String,
    val recordedAtMillis: Long
)

data class BcvRateInfo(
    val usdRate: Double,
    val fechaValor: String,
    val actualizadoEl: String,
    val fetchedAtMillis: Long
)

class BcvRateService(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "bcv_rate_cache",
        Context.MODE_PRIVATE
    )

    fun getCachedRate(): BcvRateInfo? {
        val rate = java.lang.Double.longBitsToDouble(
            prefs.getLong(KEY_USD_RATE_BITS, java.lang.Double.doubleToRawLongBits(-1.0))
        )
        if (rate <= 0.0) return null
        return BcvRateInfo(
            usdRate = rate,
            fechaValor = prefs.getString(KEY_FECHA_VALOR, "") ?: "",
            actualizadoEl = prefs.getString(KEY_ACTUALIZADO_EL, "") ?: "",
            fetchedAtMillis = prefs.getLong(KEY_FETCHED_AT, 0L)
        )
    }

    fun getCachedHistory(): List<BcvRateHistoryEntry> {
        val raw = prefs.getString(KEY_RATE_HISTORY, null)
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<BcvRateHistoryEntry>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val rate = obj.optDouble("usdRate", -1.0)
                    if (rate > 0.0) {
                        list.add(
                            BcvRateHistoryEntry(
                                usdRate = rate,
                                fechaValor = obj.optString("fechaValor", ""),
                                actualizadoEl = obj.optString("actualizadoEl", ""),
                                recordedAtMillis = obj.optLong("recordedAtMillis", 0L)
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) return list.take(MAX_HISTORY_SIZE)
            } catch (_: Exception) {
                // Fallback to cached rate below if history JSON is corrupt
            }
        }
        val cached = getCachedRate() ?: return emptyList()
        val seeded = listOf(
            BcvRateHistoryEntry(
                usdRate = cached.usdRate,
                fechaValor = cached.fechaValor,
                actualizadoEl = cached.actualizadoEl,
                recordedAtMillis = cached.fetchedAtMillis
            )
        )
        saveHistory(seeded)
        return seeded
    }

    fun isRateStaleForToday(): Boolean {
        val cached = getCachedRate() ?: return true
        if (cached.fetchedAtMillis <= 0L) return true
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cachedDay = dayFormat.format(Date(cached.fetchedAtMillis))
        val todayDay = dayFormat.format(Date(System.currentTimeMillis()))
        if (cachedDay != todayDay) return true
        return System.currentTimeMillis() - cached.fetchedAtMillis >= ONE_DAY_MILLIS
    }

    private fun saveHistory(history: List<BcvRateHistoryEntry>) {
        val array = JSONArray()
        history.take(MAX_HISTORY_SIZE).forEach { entry ->
            val obj = JSONObject().apply {
                put("usdRate", entry.usdRate)
                put("fechaValor", entry.fechaValor)
                put("actualizadoEl", entry.actualizadoEl)
                put("recordedAtMillis", entry.recordedAtMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RATE_HISTORY, array.toString()).apply()
    }

    private fun saveCachedRate(info: BcvRateInfo) {
        prefs.edit()
            .putLong(KEY_USD_RATE_BITS, java.lang.Double.doubleToRawLongBits(info.usdRate))
            .putString(KEY_FECHA_VALOR, info.fechaValor)
            .putString(KEY_ACTUALIZADO_EL, info.actualizadoEl)
            .putLong(KEY_FETCHED_AT, info.fetchedAtMillis)
            .apply()

        val currentHistory = getCachedHistory()
        val newEntry = BcvRateHistoryEntry(
            usdRate = info.usdRate,
            fechaValor = info.fechaValor,
            actualizadoEl = info.actualizadoEl,
            recordedAtMillis = info.fetchedAtMillis
        )
        val latest = currentHistory.firstOrNull()
        val nextHistory = when {
            latest == null -> listOf(newEntry)
            abs(newEntry.usdRate - latest.usdRate) > 0.0001 ||
                (newEntry.fechaValor.isNotEmpty() && newEntry.fechaValor != latest.fechaValor) -> {
                (listOf(newEntry) + currentHistory).take(MAX_HISTORY_SIZE)
            }
            else -> {
                listOf(newEntry) + currentHistory.drop(1)
            }
        }
        saveHistory(nextHistory)
    }

    suspend fun fetchLatestRate(): AppResult<BcvRateInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(BCV_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext AppResult.Error("El servidor BCV respondió con HTTP $code")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tasas = json.optJSONObject("tasas")
                ?: return@withContext AppResult.Error("Falta el campo 'tasas' en la respuesta del BCV")

            val usdRate = tasas.optDouble("USD", -1.0)
            if (usdRate.isNaN() || usdRate <= 0.0) {
                return@withContext AppResult.Error("Tasa USD inválida en la respuesta del BCV")
            }

            val info = BcvRateInfo(
                usdRate = usdRate,
                fechaValor = json.optString("fecha_valor", ""),
                actualizadoEl = json.optString("actualizado_el", ""),
                fetchedAtMillis = System.currentTimeMillis()
            )
            saveCachedRate(info)
            AppResult.Success(info)
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "No se pudo conectar al servicio de tasa BCV", e)
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val BCV_ENDPOINT = "https://datos.cromstudio.com.ve/tasa"
        private const val KEY_USD_RATE_BITS = "usd_rate_bits"
        private const val KEY_FECHA_VALOR = "fecha_valor"
        private const val KEY_ACTUALIZADO_EL = "actualizado_el"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_RATE_HISTORY = "rate_history_json"
        private const val MAX_HISTORY_SIZE = 5
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

        fun computeTrend(history: List<BcvRateHistoryEntry>): BcvTrendDirection {
            val current = history.getOrNull(0) ?: return BcvTrendDirection.SAME
            val previous = history.getOrNull(1) ?: return BcvTrendDirection.SAME
            val diff = current.usdRate - previous.usdRate
            return when {
                diff > 0.0001 -> BcvTrendDirection.UP
                diff < -0.0001 -> BcvTrendDirection.DOWN
                else -> BcvTrendDirection.SAME
            }
        }
    }
}
