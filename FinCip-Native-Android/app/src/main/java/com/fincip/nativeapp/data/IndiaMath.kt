package com.fincip.nativeapp.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

/** Pure helpers for the India wealth tracker. No Android dependencies, so they are unit-tested on the JVM. */
data class IndiaKind(
    val id: String,
    val label: String,
    val blurb: String,
    val providerHints: List<String>,
    val defaultRate: Double?,   // illustrative only, user can change it
    val recurring: Boolean,     // has a monthly contribution
    val hasMaturity: Boolean,
    val compounding: Int        // periods per year used for the estimate (12 = monthly SIP-style, 4 = quarterly FD-style, 1 = yearly)
)

object IndiaKinds {
    val all = listOf(
        IndiaKind("SIP", "SIP", "Monthly mutual-fund plan", listOf("Zerodha Coin", "Groww", "Kuvera", "ET Money", "Paytm Money", "INDmoney"), 12.0, true, false, 12),
        IndiaKind("Mutual fund", "Mutual fund", "One-time fund purchase", listOf("Zerodha Coin", "Groww", "Kuvera", "ET Money", "HDFC AMC", "SBI MF"), 12.0, false, false, 1),
        IndiaKind("ELSS", "ELSS", "Tax-saving equity fund, 3-year lock-in", listOf("Groww", "Kuvera", "ET Money", "HDFC AMC", "SBI MF"), 12.0, true, false, 12),
        IndiaKind("Stocks", "Stocks", "Direct equity holdings", listOf("Zerodha", "Groww", "Upstox", "Angel One", "ICICI Direct"), null, false, false, 1),
        IndiaKind("EPF", "EPF", "Employees' Provident Fund", listOf("EPFO"), 8.25, true, false, 12),
        IndiaKind("PPF", "PPF", "15-year public provident fund", listOf("SBI", "India Post", "HDFC Bank", "ICICI Bank"), 7.1, true, true, 1),
        IndiaKind("NPS", "NPS", "National Pension System", listOf("Protean", "KFintech", "HDFC Pension", "SBI Pension"), 10.0, true, false, 12),
        IndiaKind("FD", "FD", "Fixed deposit", listOf("SBI", "HDFC Bank", "ICICI Bank", "Axis Bank", "Post Office", "Bajaj Finance"), 7.0, false, true, 4),
        IndiaKind("RD", "RD", "Recurring deposit", listOf("SBI", "HDFC Bank", "ICICI Bank", "Post Office"), 6.8, true, true, 4),
        IndiaKind("NSC", "NSC", "National Savings Certificate", listOf("India Post"), 7.7, false, true, 1),
        IndiaKind("Gold", "Gold", "Physical, digital or ETF", listOf("Jewellers", "Google Pay", "PhonePe", "Groww", "Gold ETF"), 8.0, false, false, 1),
        IndiaKind("SGB", "SGB", "Sovereign Gold Bond", listOf("RBI", "Demat account"), 10.0, false, true, 1),
        IndiaKind("Other", "Other", "Anything else you track", emptyList(), null, false, false, 1)
    )
    fun of(id: String): IndiaKind = all.firstOrNull { it.id.equals(id, true) } ?: all.last()
}

object IndiaMath {
    /** Future value of a monthly SIP (payment at start of each month) plus an optional lump sum. Annual rate in percent. */
    fun sipFutureValue(monthly: Double, annualRatePct: Double, months: Int, lumpSum: Double = 0.0): Double {
        if (months <= 0) return lumpSum
        val i = annualRatePct / 1200.0
        val sip = if (i == 0.0) monthly * months else monthly * (((1 + i).pow(months) - 1) / i) * (1 + i)
        val lump = lumpSum * (1 + i).pow(months)
        return sip + lump
    }

    /** Compound growth of a lump sum. periodsPerYear = 4 for typical bank FDs, 1 for yearly. */
    fun lumpFutureValue(principal: Double, annualRatePct: Double, years: Double, periodsPerYear: Int = 1): Double {
        if (years <= 0) return principal
        val n = periodsPerYear.coerceAtLeast(1)
        return principal * (1 + annualRatePct / 100.0 / n).pow(n * years)
    }

    fun monthsBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.MONTHS.between(from.withDayOfMonth(1), to.withDayOfMonth(1)).toInt()

    /** Indian financial year starts on 1 April. */
    fun fiscalYearStart(d: LocalDate): LocalDate = if (d.monthValue >= 4) LocalDate.of(d.year, 4, 1) else LocalDate.of(d.year - 1, 4, 1)
    fun fiscalYearLabel(d: LocalDate): String {
        val s = fiscalYearStart(d)
        return "FY ${s.year}-%02d".format((s.year + 1) % 100)
    }

    /** Sum of contribution amounts (positive) inside the financial year that contains [today]. */
    fun contributedInFy(flows: List<Pair<LocalDate, Double>>, today: LocalDate): Double {
        val start = fiscalYearStart(today)
        val end = start.plusYears(1)
        return flows.filter { !it.first.isBefore(start) && it.first.isBefore(end) }.sumOf { kotlin.math.abs(it.second) }
    }

    /** Estimated value at [maturity] from what is entered. It is an estimate, never a guarantee. */
    fun projectedValue(kind: IndiaKind, invested: Double, monthly: Double, ratePct: Double, start: LocalDate, maturity: LocalDate): Double? {
        if (maturity.isBefore(start)) return null
        val months = monthsBetween(start, maturity)
        if (months <= 0) return null
        return if (kind.recurring && monthly > 0) sipFutureValue(monthly, ratePct, months, invested)
        else lumpFutureValue(invested, ratePct, months / 12.0, if (kind.compounding == 4) 4 else 1)
    }

    /** Years for money to double at a given rate (rule of 72, rounded to one decimal). */
    fun doublingYears(ratePct: Double): Double? = if (ratePct <= 0) null else Math.round(72.0 / ratePct * 10.0) / 10.0

    /** Cumulative invested amount at the end of each of the last [months] months (oldest first). */
    fun cumulativeByMonth(flows: List<Pair<LocalDate, Double>>, today: LocalDate, months: Int): List<Pair<LocalDate, Double>> {
        val first = today.withDayOfMonth(1).minusMonths((months - 1).toLong())
        return (0 until months).map { i ->
            val m = first.plusMonths(i.toLong())
            val end = m.plusMonths(1)
            m to flows.filter { it.first.isBefore(end) }.sumOf { kotlin.math.abs(it.second) }
        }
    }

    /** Share of the portfolio in the largest holding, for the concentration insight. */
    fun topShare(values: List<Double>): Double? {
        val total = values.sum()
        return if (total <= 0) null else values.max() / total * 100.0
    }
}
