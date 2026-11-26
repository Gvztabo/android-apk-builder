package com.cuentasclaras.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val usdFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val bsNumberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "VE"))

    fun formatUsd(amountUsd: Double): String {
        val absFormatted = usdFormat.format(kotlin.math.abs(amountUsd))
        return if (amountUsd < 0) "-$absFormatted" else absFormatted
    }

    fun formatBs(amountUsd: Double, bcvRate: Double?): String? {
        if (bcvRate == null || bcvRate <= 0.0) return null
        val bs = amountUsd * bcvRate
        val absFormatted = bsNumberFormat.format(kotlin.math.abs(bs))
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
        return dateFormat.format(Date(timestamp))
    }
}
