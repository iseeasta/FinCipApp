package com.fincip.nativeapp.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** Fetches published reference FX rates. Frankfurter returns units of quote currency per one INR;
 * FinCip stores the inverse as INR per one unit of foreign currency. */
object FxRateApi {
    data class Result(val rates: Map<String, Double>, val date: String)

    fun fetchInrRates(currencies: List<String>): Result {
        require(currencies.isNotEmpty()) { "Choose at least one currency." }
        val quotes = currencies.distinct().filter { it != "INR" }.joinToString(",")
        require(quotes.isNotBlank()) { "No foreign currencies were selected." }
        val url = URL("https://api.frankfurter.dev/v2/rates?base=INR&quotes=$quotes")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 18_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Frankfurter returned HTTP $code")
            val payload = JsonParser.parseString(text)
            val rows: List<JsonObject> = when {
                payload.isJsonArray -> payload.asJsonArray.mapNotNull { element -> if (element.isJsonObject) element.asJsonObject else null }
                payload.isJsonObject -> {
                    val root = payload.asJsonObject
                    val list = root.get("rates")
                    if (list != null && list.isJsonArray) list.asJsonArray.mapNotNull { element -> if (element.isJsonObject) element.asJsonObject else null }
                    else if (list != null && list.isJsonObject) list.asJsonObject.entrySet().map { (quote, rate) ->
                        JsonObject().apply { addProperty("quote", quote); add("rate", rate.deepCopy()); if (root.has("date")) add("date", root.get("date").deepCopy()) }
                    }
                    else emptyList()
                }
                else -> emptyList()
            }
            require(rows.isNotEmpty()) { "The provider returned no reference rates." }
            val result = linkedMapOf<String, Double>()
            rows.forEach { row ->
                val quote = row.string("quote", row.string("currency")).uppercase()
                val perInr = row.double("rate")
                if (quote in currencies && perInr > 0.0) result[quote] = 1.0 / perInr
            }
            require(result.isNotEmpty()) { "No requested currency rates were available." }
            val date = rows.firstNotNullOfOrNull { it.string("date").takeIf(String::isNotBlank) } ?: LocalDate.now().toString()
            return Result(result, date)
        } finally {
            connection.disconnect()
        }
    }
}
