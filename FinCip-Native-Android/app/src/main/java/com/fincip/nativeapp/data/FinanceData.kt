package com.fincip.nativeapp.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.Locale
import kotlin.math.max

val CURRENCY_INFO = listOf(
    CurrencyInfo("INR", "Indian rupee", "₹", "🇮🇳", "India"),
    CurrencyInfo("USD", "US dollar", "$", "🇺🇸", "United States"),
    CurrencyInfo("EUR", "Euro", "€", "🇪🇺", "Euro area"),
    CurrencyInfo("GBP", "Pound sterling", "£", "🇬🇧", "United Kingdom"),
    CurrencyInfo("JPY", "Japanese yen", "¥", "🇯🇵", "Japan"),
    CurrencyInfo("CNY", "Chinese yuan", "¥", "🇨🇳", "China"),
    CurrencyInfo("CHF", "Swiss franc", "CHF", "🇨🇭", "Switzerland"),
    CurrencyInfo("CAD", "Canadian dollar", "$", "🇨🇦", "Canada"),
    CurrencyInfo("AUD", "Australian dollar", "$", "🇦🇺", "Australia"),
    CurrencyInfo("SGD", "Singapore dollar", "$", "🇸🇬", "Singapore"),
)

data class CurrencyInfo(val code: String, val name: String, val symbol: String, val flag: String, val country: String)
data class NavPage(val id: String, val title: String, val group: String, val glyph: String)

val NAV_PAGES = listOf(
    NavPage("overview", "Overview", "Home", "▦"),
    NavPage("transactions", "Transactions", "Money", "↔"),
    NavPage("recurring", "Bills & repeats", "Money", "◷"),
    NavPage("calendar", "Money calendar", "Money", "▦"),
    NavPage("subscriptions", "Subscriptions", "Money", "⟳"),
    NavPage("currency", "Currency converter", "Money", "⇄"),
    NavPage("budgets", "Budgets", "Plan", "▤"),
    NavPage("goals", "Goals & debt", "Plan", "◎"),
    NavPage("networth", "Net worth", "Plan", "◇"),
    NavPage("simulator", "What-if lab", "Plan", "✳"),
    NavPage("india", "India finance", "Plan", "₹"),
    NavPage("tax", "Global tax planner", "Plan", "▧"),
    NavPage("portfolio", "Portfolio", "Plan", "⌁"),
    NavPage("forecast", "Forecast", "Plan", "⌇"),
    NavPage("insights", "Insights", "Explore", "◌"),
    NavPage("rules", "Smart rules", "Explore", "⚡"),
    NavPage("receipts", "Receipt vault", "Explore", "▣"),
    NavPage("household", "Household & sync", "Explore", "♧"),
    NavPage("security", "Privacy & backups", "Explore", "⬡"),
    NavPage("settings", "Settings & vibe", "Explore", "⚙"),
)

fun defaultWorkspace(): JsonObject {
    val root = JsonObject()
    listOf("transactions", "budgets", "goals", "recurringRules", "investments", "sharedMembers", "netWorthItems", "financeProducts", "smartRules", "receipts").forEach { root.add(it, JsonArray()) }
    root.add("emergencyFund", JsonObject().apply { addProperty("balance", 0); addProperty("targetMonths", 6) })
    root.add("taxProfile", JsonObject().apply {
        addProperty("country", "IN"); addProperty("currency", "INR"); addProperty("taxYear", "2026–27"); addProperty("mode", "country")
        addProperty("regime", "new"); addProperty("income", 0); addProperty("deductions", 0)
        addProperty("standardDeduction", 75000); addProperty("allowance", 0); addProperty("effectiveRate", 20)
        addProperty("ageGroup", "below60"); addProperty("resident", true)
    })
    root.add("settings", JsonObject().apply {
        addProperty("theme", "light"); addProperty("vibeTheme", "Material You"); addProperty("baseCurrency", "INR")
        add("dashboardOrder", JsonArray().apply { listOf("balance", "income", "expenses", "savings").forEach(::add) })
        add("fxRates", JsonObject().apply { addProperty("INR", 1); addProperty("USD", 92.9699); addProperty("EUR", 102.1504); addProperty("GBP", 0); addProperty("JPY", 0); addProperty("CNY", 0); addProperty("CHF", 0); addProperty("CAD", 0); addProperty("AUD", 0); addProperty("SGD", 0) })
        addProperty("privacyMode", false)
    })
    root.add("mascot", JsonObject().apply { addProperty("name", "Mochi"); addProperty("kind", "🐱"); addProperty("accent", "#D8FF3E") })
    return root
}

fun JsonObject.deepCopyObject(): JsonObject = JsonParser.parseString(toString()).asJsonObject
fun JsonObject.objectOrEmpty(key: String): JsonObject = get(key)?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
fun JsonObject.arrayOrEmpty(key: String): JsonArray = get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
fun JsonObject.string(key: String, fallback: String = ""): String = try { get(key)?.takeIf { !it.isJsonNull }?.asString ?: fallback } catch (_: Exception) { fallback }
fun JsonObject.double(key: String, fallback: Double = 0.0): Double = try { get(key)?.takeIf { !it.isJsonNull }?.asDouble ?: fallback } catch (_: Exception) { fallback }
fun JsonObject.long(key: String, fallback: Long = 0): Long = try { get(key)?.takeIf { !it.isJsonNull }?.asLong ?: fallback } catch (_: Exception) { fallback }
fun JsonObject.bool(key: String, fallback: Boolean = false): Boolean = try { get(key)?.takeIf { !it.isJsonNull }?.asBoolean ?: fallback } catch (_: Exception) { fallback }
fun JsonElement.asObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null
fun JsonElement.asStringOr(default: String = ""): String = try { if (isJsonNull) default else asString } catch (_: Exception) { default }
fun JsonElement.asDoubleOr(default: Double = 0.0): Double = try { if (isJsonNull) default else asDouble } catch (_: Exception) { default }
fun JsonArray.objects(): List<JsonObject> = mapNotNull { it.asObjectOrNull() }

fun money(amount: Double, currencyCode: String = "INR"): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.currency = Currency.getInstance(currencyCode)
        format.maximumFractionDigits = 2
        format.format(amount)
    } catch (_: Exception) {
        "$currencyCode ${"%,.2f".format(amount)}"
    }
}

fun fxRate(workspace: JsonObject, currency: String): Double {
    val rates = workspace.objectOrEmpty("settings").objectOrEmpty("fxRates")
    return if (currency == "INR") 1.0 else rates.double(currency, 0.0)
}

fun toBase(workspace: JsonObject, amount: Double, currency: String): Double {
    val settings = workspace.objectOrEmpty("settings")
    val base = settings.string("baseCurrency", "INR")
    val source = fxRate(workspace, currency)
    val target = fxRate(workspace, base)
    return if (source > 0 && target > 0) amount * source / target else 0.0
}

fun transactions(workspace: JsonObject): List<JsonObject> = workspace.arrayOrEmpty("transactions").objects()
fun monthlyTransactions(workspace: JsonObject, month: String): List<JsonObject> = transactions(workspace).filter { it.string("date").startsWith(month) }
fun incomeTotal(workspace: JsonObject, month: String? = null): Double = (if (month == null) transactions(workspace) else monthlyTransactions(workspace, month)).filter { it.string("type") == "income" }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
fun expenseTotal(workspace: JsonObject, month: String? = null): Double = (if (month == null) transactions(workspace) else monthlyTransactions(workspace, month)).filter { it.string("type") != "income" }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
fun normalizeTaxCountry(value: String): String = when (value.trim().uppercase(Locale.ROOT)) {
    "IN", "INDIA" -> "IN"
    "US", "USA", "UNITED STATES" -> "US"
    "GB", "UK", "UNITED KINGDOM" -> "GB"
    "DE", "GERMANY", "EURO AREA", "EU" -> "DE"
    "JP", "JAPAN" -> "JP"
    "CN", "CHINA" -> "CN"
    "CH", "SWITZERLAND" -> "CH"
    "CA", "CANADA" -> "CA"
    "AU", "AUSTRALIA" -> "AU"
    "SG", "SINGAPORE" -> "SG"
    else -> "IN"
}

fun taxCurrency(countryCode: String): String = when (normalizeTaxCountry(countryCode)) {
    "US" -> "USD"; "GB" -> "GBP"; "DE" -> "EUR"; "JP" -> "JPY"; "CN" -> "CNY"
    "CH" -> "CHF"; "CA" -> "CAD"; "AU" -> "AUD"; "SG" -> "SGD"; else -> "INR"
}

fun currentMonth(): String = YearMonth.now().toString()
fun parseMonth(month: String): YearMonth = try { YearMonth.parse(month) } catch (_: Exception) { YearMonth.now() }
fun isoToday(): String = LocalDate.now().toString()
fun nextDueLabel(date: String): String = try {
    val d = LocalDate.parse(date); val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), d)
    when { days < 0 -> "${-days} days overdue"; days == 0L -> "Due today"; days == 1L -> "Due tomorrow"; else -> "In $days days" }
} catch (_: Exception) { date }

fun mergeDefaults(imported: JsonObject): JsonObject {
    val base = defaultWorkspace()
    imported.entrySet().forEach { (key, value) -> base.add(key, value.deepCopy()) }
    listOf("transactions", "budgets", "goals", "recurringRules", "investments", "sharedMembers", "netWorthItems", "financeProducts", "smartRules", "receipts").forEach { key ->
        if (!base.has(key) || !base.get(key).isJsonArray) base.add(key, JsonArray())
    }
    val defaults = defaultWorkspace()
    val settings = base.objectOrEmpty("settings").deepCopyObject()
    val mergedSettings = defaults.objectOrEmpty("settings")
    settings.entrySet().forEach { (k, v) -> mergedSettings.add(k, v.deepCopy()) }
    val fx = defaults.objectOrEmpty("settings").objectOrEmpty("fxRates")
    settings.objectOrEmpty("fxRates").entrySet().forEach { (k, v) -> fx.add(k, v.deepCopy()) }
    mergedSettings.add("fxRates", fx)
    base.add("settings", mergedSettings)
    return base
}
