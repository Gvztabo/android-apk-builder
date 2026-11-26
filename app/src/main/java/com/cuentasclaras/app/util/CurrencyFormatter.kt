package com.cuentasclaras.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    private val esVeLocale = Locale("es", "VE")

    private val usdFormatThreadLocal: ThreadLocal<NumberFormat> = ThreadLocal.withInitial {
        NumberFormat.getCurrencyInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    private val bsFormatThreadLocal: ThreadLocal<NumberFormat> = ThreadLocal.withInitial {
        NumberFormat.getNumberInstance(esVeLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", esVeLocale)

    fun formatUsd(amountUsd: Double): String {
        val formatter = usdFormatThreadLocal.get()!!
        val absFormatted = formatter.format(abs(amountUsd))
        return if (amountUsd < 0) "-$absFormatted" else absFormatted
    }

    fun formatBs(amountUsd: Double, bcvRate: Double?): String? {
        if (bcvRate == null || bcvRate <= 0.0) return null
        val bs = amountUsd * bcvRate
        val formatter = bsFormatThreadLocal.get()!!
        val absFormatted = formatter.format(abs(bs))
        return if (bs < 0) "-Bs. $absFormatted" else "Bs. $absFormatted"
    }

    fun formatDual(amountUsd: Double, bcvRate: Double?): String {
        val usd = formatUsd(amountUsd)
        val bs = formatBs(amountUsd, bcvRate)
        return if (bs != null) {
            "$usd / $bs"
        } else {
            "$usd · Tasa BCV no disponible"
        }
    }

    fun formatDualBsFirst(amountUsd: Double, bcvRate: Double?): String {
        val usd = formatUsd(amountUsd)
        val bs = formatBs(amountUsd, bcvRate)
        return if (bs != null) {
            "$bs / $usd"
        } else {
            "$usd · Tasa BCV no disponible"
        }
    }

    fun formatDate(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }
}
