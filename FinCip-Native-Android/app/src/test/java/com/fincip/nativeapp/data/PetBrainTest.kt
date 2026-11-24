package com.fincip.nativeapp.data

import org.junit.Assert.assertTrue
import org.junit.Test

class PetBrainTest {
    @Test fun emptyLedgerAsksForFirstEntry() {
        val l = PetBrain.lines(PetCtx(name = "Mochi", txCount = 0))
        assertTrue(l.any { it.contains("Log one", true) || it.contains("Feed", true) || it.contains("transactions", true) })
        assertTrue(l.isNotEmpty())
    }
    @Test fun overspendingMentionsNumbers() {
        val l = PetBrain.lines(PetCtx(txCount = 5, income = 1000.0, expenses = 1500.0, incomeStr = "₹1,000", expensesStr = "₹1,500"))
        assertTrue(l.any { it.contains("₹1,500") && it.contains("₹1,000") })
    }
    @Test fun billSoonIsMentioned() {
        val l = PetBrain.lines(PetCtx(txCount = 5, income = 1.0, expenses = 0.5, billName = "Rent", billDays = 2, billAmtStr = "₹9,000"))
        assertTrue(l.any { it.contains("Rent") && it.contains("2 days") })
    }
    @Test fun noDuplicatesAndNoNaN() {
        val l = PetBrain.lines(PetCtx(txCount = 3, income = 100.0, expenses = 40.0, savingsRate = 60.0, topCategory = "Food", topCategoryShare = 50, topCategoryAmtStr = "₹20"))
        assertTrue(l.size == l.distinct().size)
        assertTrue(l.none { it.contains("NaN") || it.contains("null") })
    }
}
