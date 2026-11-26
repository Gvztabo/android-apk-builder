package com.cuentasclaras.app.util

import java.text.Normalizer
import kotlin.math.round

data class ParsedVoiceProduct(
    val name: String,
    val price: Double?
)

object VoiceProductParser {

    private val unitsMap = mapOf(
        "cero" to 0,
        "un" to 1,
        "uno" to 1,
        "una" to 1,
        "dos" to 2,
        "tres" to 3,
        "cuatro" to 4,
        "cinco" to 5,
        "seis" to 6,
        "siete" to 7,
        "ocho" to 8,
        "nueve" to 9
    )

    private val teensAndTwentiesMap = mapOf(
        "diez" to 10,
        "once" to 11,
        "doce" to 12,
        "trece" to 13,
        "catorce" to 14,
        "quince" to 15,
        "dieciseis" to 16,
        "diecisiete" to 17,
        "dieciocho" to 18,
        "diecinueve" to 19,
        "veinte" to 20,
        "veintiun" to 21,
        "veintiuno" to 21,
        "veintiuna" to 21,
        "veintidos" to 22,
        "veintitres" to 23,
        "veinticuatro" to 24,
        "veinticinco" to 25,
        "veintiseis" to 26,
        "veintisiete" to 27,
        "veintiocho" to 28,
        "veintinueve" to 29
    )

    private val tensMap = mapOf(
        "treinta" to 30,
        "cuarenta" to 40,
        "cincuenta" to 50,
        "sesenta" to 60,
        "setenta" to 70,
        "ochenta" to 80,
        "noventa" to 90
    )

    private val hundredsMap = mapOf(
        "cien" to 100,
        "ciento" to 100,
        "doscientos" to 200,
        "doscientas" to 200,
        "trescientos" to 300,
        "trescientas" to 300,
        "cuatrocientos" to 400,
        "cuatrocientas" to 400,
        "quinientos" to 500,
        "quinientas" to 500,
        "seiscientos" to 600,
        "seiscientas" to 600,
        "setecientos" to 700,
        "setecientas" to 700,
        "ochocientos" to 800,
        "ochocientas" to 800,
        "novecientos" to 900,
        "novecientas" to 900
    )

    private val noiseWords = setOf(
        "dolares", "dolar", "bolivares", "bolivar", "bs", "usd", "pesos", "peso", "bcv"
    )

    private val centMarkers = setOf("centavos", "centavo", "centimos", "centimo")

    fun parse(rawTranscript: String): ParsedVoiceProduct {
        val trimmed = rawTranscript.trim()
        if (trimmed.isEmpty()) return ParsedVoiceProduct("", null)

        val normalized = stripAccents(trimmed.lowercase())
            .replace("$", " ")
            .replace("bs.", " ")
            .replace(Regex("[^a-z0-9.,\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val rawTokens = normalized.split(" ").filter { it.isNotBlank() }
        if (rawTokens.isEmpty()) return ParsedVoiceProduct(capitalizeName(trimmed), null)

        // 1. Try explicit price connectors: "cuesta", "por", "a"
        val explicitConnectors = listOf("cuesta", "por", "a")
        for (i in rawTokens.indices) {
            if (rawTokens[i] in explicitConnectors && i > 0 && i < rawTokens.lastIndex) {
                val pricePart = rawTokens.subList(i + 1, rawTokens.size)
                val parsedPrice = parsePriceTokens(pricePart)
                if (parsedPrice != null) {
                    val namePart = extractOriginalNamePrefix(trimmed, i)
                    return ParsedVoiceProduct(capitalizeName(namePart), parsedPrice)
                }
            }
        }

        // 2. Try implicit decimal/price separator "con" or "y" (e.g. "Pan con treinta")
        for (i in rawTokens.indices) {
            if ((rawTokens[i] == "con" || rawTokens[i] == "y") && i > 0 && i < rawTokens.lastIndex) {
                val priceWithConnector = rawTokens.subList(i, rawTokens.size)
                val parsedPrice = parsePriceTokens(priceWithConnector)
                if (parsedPrice != null) {
                    val namePart = extractOriginalNamePrefix(trimmed, i)
                    return ParsedVoiceProduct(capitalizeName(namePart), parsedPrice)
                }
            }
        }

        // 3. Fallback: try splitting trailing numeric tokens from the end (e.g. "Leche 2.50")
        for (splitIdx in 1 until rawTokens.size) {
            val candidatePrice = parsePriceTokens(rawTokens.subList(splitIdx, rawTokens.size))
            if (candidatePrice != null) {
                val namePart = extractOriginalNamePrefix(trimmed, splitIdx)
                return ParsedVoiceProduct(capitalizeName(namePart), candidatePrice)
            }
        }

        // 4. Whole phrase might just be a number
        val wholePrice = parsePriceTokens(rawTokens)
        return if (wholePrice != null) {
            ParsedVoiceProduct("", wholePrice)
        } else {
            ParsedVoiceProduct(capitalizeName(trimmed), null)
        }
    }

    private fun extractOriginalNamePrefix(original: String, wordCount: Int): String {
        val originalWords = original.trim().split(Regex("\\s+"))
        return if (wordCount <= originalWords.size) {
            originalWords.subList(0, wordCount).joinToString(" ")
        } else {
            original
        }
    }

    private fun parsePriceTokens(tokens: List<String>): Double? {
        val hasCentWord = tokens.any { it in centMarkers }
        val filtered = tokens.filter { it !in noiseWords && it !in centMarkers && it != "de" }
        if (filtered.isEmpty()) return null

        // Direct single numeric literal like "2.50" or "2,50" or "10"
        if (filtered.size == 1) {
            val direct = filtered[0].replace(',', '.').toDoubleOrNull()
            if (direct != null) {
                return if (hasCentWord && direct < 100.0 && !filtered[0].contains('.') && !filtered[0].contains(',')) {
                    round2(direct / 100.0)
                } else {
                    round2(direct)
                }
            }
        }

        // Split by decimal connector: "con", "punto", "coma"
        val decimalIdx = filtered.indexOfFirst { it == "con" || it == "punto" || it == "coma" }
        if (decimalIdx >= 0) {
            val intTokens = filtered.subList(0, decimalIdx)
            val decTokens = filtered.subList(decimalIdx + 1, filtered.size)
            if (decTokens.isEmpty()) return null
            val intVal = if (intTokens.isEmpty()) 0 else parseIntegerWords(intTokens) ?: return null
            val decVal = parseDecimalPart(decTokens, hasCentWord) ?: return null
            return round2(intVal + decVal)
        }

        // Check "y" as decimal separator when NOT compound tens (e.g. "treinta y cinco" is 35, "dos y cincuenta" is 2.50)
        val yIdx = filtered.indexOf("y")
        if (yIdx > 0 && yIdx < filtered.lastIndex) {
            val leftTokens = filtered.subList(0, yIdx)
            val rightTokens = filtered.subList(yIdx + 1, filtered.size)
            val leftLast = leftTokens.last()
            val rightFirst = rightTokens.first()
            val isCompoundTens = leftLast in tensMap && rightTokens.size == 1 && rightFirst in unitsMap
            if (!isCompoundTens) {
                val intVal = parseIntegerWords(leftTokens)
                val decVal = parseDecimalPart(rightTokens, hasCentWord)
                if (intVal != null && decVal != null) {
                    return round2(intVal + decVal)
                }
            }
        }

        // Pure integer expression (or "medio")
        if (filtered.size == 1 && (filtered[0] == "medio" || filtered[0] == "media")) {
            return 0.50
        }
        val integer = parseIntegerWords(filtered) ?: return null
        return if (hasCentWord && integer < 100) {
            round2(integer / 100.0)
        } else {
            round2(integer.toDouble())
        }
    }

    private fun parseDecimalPart(tokens: List<String>, hasCentWord: Boolean): Double? {
        if (tokens.isEmpty()) return null
        if (tokens.size == 1 && (tokens[0] == "medio" || tokens[0] == "media")) {
            return 0.50
        }
        // Handle leading "cero" e.g. "cero cinco" -> 0.05
        if (tokens[0] == "cero" && tokens.size >= 2) {
            val rest = parseIntegerWords(tokens.drop(1)) ?: return null
            return rest / 100.0
        }
        // Handle single numeric token like "05" or "50" or "5"
        if (tokens.size == 1) {
            val raw = tokens[0]
            val num = raw.toIntOrNull()
            if (num != null) {
                if (raw.startsWith("0") && raw.length >= 2) {
                    return num / 100.0
                }
                return if (num in 1..9 && !hasCentWord) (num * 10) / 100.0 else num / 100.0
            }
        }
        val parsed = parseIntegerWords(tokens) ?: return null
        if (parsed < 0 || parsed > 99) return null
        return if (parsed in 1..9 && !hasCentWord) {
            (parsed * 10) / 100.0
        } else {
            parsed / 100.0
        }
    }

    private fun parseIntegerWords(tokens: List<String>): Int? {
        if (tokens.isEmpty()) return null
        if (tokens.size == 1) {
            tokens[0].toIntOrNull()?.let { return it }
        }
        var total = 0
        var current = 0
        var sawNumber = false
        for (token in tokens) {
            if (token == "y") continue
            val asInt = token.toIntOrNull()
            if (asInt != null) {
                current += asInt
                sawNumber = true
                continue
            }
            when {
                token in unitsMap -> {
                    current += unitsMap.getValue(token)
                    sawNumber = true
                }
                token in teensAndTwentiesMap -> {
                    current += teensAndTwentiesMap.getValue(token)
                    sawNumber = true
                }
                token in tensMap -> {
                    current += tensMap.getValue(token)
                    sawNumber = true
                }
                token in hundredsMap -> {
                    current += hundredsMap.getValue(token)
                    sawNumber = true
                }
                token == "mil" -> {
                    total += if (current == 0) 1000 else current * 1000
                    current = 0
                    sawNumber = true
                }
                else -> return null
            }
        }
        return if (sawNumber) total + current else null
    }

    private fun stripAccents(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun capitalizeName(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return ""
        return cleaned.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
