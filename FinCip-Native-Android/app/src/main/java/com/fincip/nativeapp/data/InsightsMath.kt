package com.fincip.nativeapp.data

import java.time.LocalDate
import java.time.YearMonth

data class Spend(val date: LocalDate, val amount: Double, val category: String, val title: String)

data class CategoryDelta(val category: String, val current: Double, val previous: Double) {
    val change: Double get() = current - previous
}

data class MonthInsights(
    val total: Double, val previousTotal: Double, val daysElapsed: Int, val daysInMonth: Int,
    val dailyAverage: Double, val projected: Double?, val biggest: Spend?,
    val topTitles: List<Pair<String, Double>>, val weekdayTotals: List<Double>, // index 0 = Monday
    val busiestWeekday: Int?, val weekendShare: Double, val categories: List<CategoryDelta>
) {
    /** Percent change vs last month, null when there is no previous data. */
    val changePct: Double? get() = if (previousTotal > 0) (total - previousTotal) / previousTotal * 100.0 else null
    val biggestRise: CategoryDelta? get() = categories.filter { it.previous > 0 && it.change > 0 }.maxByOrNull { it.change }
    val biggestDrop: CategoryDelta? get() = categories.filter { it.previous > 0 && it.change < 0 }.minByOrNull { it.change }
}

object InsightsMath {
    fun analyze(current: List<Spend>, previous: List<Spend>, month: YearMonth, today: LocalDate): MonthInsights {
        val dim = month.lengthOfMonth()
        val isNow = YearMonth.from(today) == month
        val elapsed = if (isNow) today.dayOfMonth else dim
        val total = current.sumOf { it.amount }
        val prevTotal = previous.sumOf { it.amount }
        val weekday = DoubleArray(7).also { arr -> current.forEach { arr[it.date.dayOfWeek.value - 1] += it.amount } }.toList()
        val busiest = if (total > 0) weekday.indices.maxByOrNull { weekday[it] } else null
        val weekend = if (total > 0) (weekday[5] + weekday[6]) / total else 0.0
        val curCat = current.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val prevCat = previous.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val cats = (curCat.keys + prevCat.keys).map { CategoryDelta(it, curCat[it] ?: 0.0, prevCat[it] ?: 0.0) }.sortedByDescending { maxOf(it.current, it.previous) }
        val titles = current.filter { it.title.isNotBlank() }.groupBy { it.title.trim().lowercase() }
            .map { (_, v) -> v.first().title.trim() to v.sumOf { it.amount } }.sortedByDescending { it.second }.take(5)
        return MonthInsights(
            total, prevTotal, elapsed, dim, if (elapsed > 0) total / elapsed else 0.0,
            if (isNow && elapsed >= 3 && total > 0) total / elapsed * dim else null,
            current.maxByOrNull { it.amount }, titles, weekday, busiest, weekend, cats
        )
    }
}
