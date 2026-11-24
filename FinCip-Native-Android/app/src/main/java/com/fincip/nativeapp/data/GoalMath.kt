package com.fincip.nativeapp.data

import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

data class DebtPlan(val months: Int, val totalInterest: Double)

object GoalPurposes {
    val saving = listOf("Emergency", "Trip", "Gadget", "Education", "Home", "Vehicle", "Wedding", "Retirement", "Gift", "Other")
    val debt = listOf("Credit card", "Personal loan", "Education loan", "Car loan", "Home loan", "Friend / family", "Buy now pay later", "Other")
    fun icon(p: String): String = when (p) {
        "Emergency" -> "🛟"; "Trip" -> "✈️"; "Gadget" -> "📱"; "Education" -> "🎓"; "Home" -> "🏠"; "Vehicle" -> "🚗"; "Wedding" -> "💍"; "Retirement" -> "🌴"; "Gift" -> "🎁"
        "Credit card" -> "💳"; "Personal loan" -> "🤝"; "Education loan" -> "📚"; "Car loan" -> "🚙"; "Home loan" -> "🏡"; "Friend / family" -> "🧑‍🤝‍🧑"; "Buy now pay later" -> "🛍️"
        else -> "🎯"
    }
    fun suggestions(p: String): List<String> = when (p) {
        "Emergency" -> listOf("Emergency fund", "6-month cushion")
        "Trip" -> listOf("Goa trip", "Europe trip", "Weekend getaway")
        "Gadget" -> listOf("New phone", "Laptop")
        "Education" -> listOf("Course fee", "Exam fund")
        "Home" -> listOf("Down payment", "Home renovation")
        "Vehicle" -> listOf("Bike", "Car down payment")
        "Wedding" -> listOf("Wedding fund")
        "Retirement" -> listOf("Retirement corpus")
        "Credit card" -> listOf("Credit card bill")
        "Personal loan" -> listOf("Personal loan")
        "Education loan" -> listOf("Education loan")
        "Car loan" -> listOf("Car loan")
        "Home loan" -> listOf("Home loan")
        else -> emptyList()
    }
}

object GoalMath {
    private fun monthlyRate(aprPct: Double) = aprPct / 1200.0

    /** Whole months to reach [remaining] by saving [monthly] a month at [annualReturnPct]. Null when nothing is being saved. */
    fun monthsToSave(remaining: Double, monthly: Double, annualReturnPct: Double = 0.0): Int? {
        if (remaining <= 0) return 0
        if (monthly <= 0) return null
        val i = monthlyRate(annualReturnPct)
        val n = if (i <= 0) remaining / monthly else ln(1 + remaining * i / monthly) / ln(1 + i)
        return ceil(n - 1e-9).toInt().coerceAtLeast(1)
    }

    /** Monthly amount needed to cover [remaining] in [months]. */
    fun requiredMonthly(remaining: Double, months: Int, annualReturnPct: Double = 0.0): Double {
        if (remaining <= 0 || months <= 0) return 0.0
        val i = monthlyRate(annualReturnPct)
        return if (i <= 0) remaining / months else remaining * i / ((1 + i).pow(months) - 1)
    }

    /** Month-by-month payoff. Null if the payment never beats the interest (or is zero). */
    fun simulateDebt(balance: Double, payment: Double, aprPct: Double): DebtPlan? {
        if (balance <= 0) return DebtPlan(0, 0.0)
        if (payment <= 0) return null
        val i = monthlyRate(aprPct)
        if (payment <= balance * i + 1e-9) return null
        var bal = balance; var months = 0; var interest = 0.0
        while (bal > 0.005 && months < 1200) {
            val add = bal * i
            interest += add
            bal = bal + add - payment
            months++
        }
        return DebtPlan(months, interest)
    }

    fun dateAfter(months: Int, from: LocalDate = LocalDate.now()): LocalDate = from.plusMonths(months.toLong())

    fun monthsUntil(date: LocalDate, from: LocalDate = LocalDate.now()): Int = java.time.temporal.ChronoUnit.MONTHS.between(from, date).toInt()
}
