package com.fincip.nativeapp.data

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsMathTest {
    private fun s(d: String, a: Double, c: String, t: String = "") = Spend(LocalDate.parse(d), a, c, t)
    private val nov = YearMonth.of(2026, 11)

    @Test fun totalsAndChange() {
        val cur = listOf(s("2026-11-02", 600.0, "Food"), s("2026-11-03", 400.0, "Travel"))
        val prev = listOf(s("2026-10-05", 500.0, "Food"))
        val r = InsightsMath.analyze(cur, prev, nov, LocalDate.of(2026, 11, 10))
        assertEquals(1000.0, r.total, 0.001)
        assertEquals(100.0, r.changePct!!, 0.001)
        assertEquals("Food", r.biggestRise!!.category)
        assertEquals(100.0, r.biggestRise!!.change, 0.001)
    }
    @Test fun projectionOnlyForCurrentMonth() {
        val cur = listOf(s("2026-11-01", 1000.0, "Food"))
        val now = InsightsMath.analyze(cur, emptyList(), nov, LocalDate.of(2026, 11, 10))
        assertEquals(3000.0, now.projected!!, 0.001) // 1000/10*30
        val past = InsightsMath.analyze(cur, emptyList(), nov, LocalDate.of(2026, 12, 5))
        assertNull(past.projected); assertEquals(30, past.daysElapsed)
    }
    @Test fun weekdayAndWeekend() {
        // 2026-11-07 is a Saturday, 2026-11-09 a Monday
        val r = InsightsMath.analyze(listOf(s("2026-11-07", 300.0, "Food"), s("2026-11-09", 100.0, "Food")), emptyList(), nov, LocalDate.of(2026, 11, 20))
        assertEquals(5, r.busiestWeekday); assertEquals(0.75, r.weekendShare, 0.001)
    }
    @Test fun topTitlesMergeCase() {
        val r = InsightsMath.analyze(listOf(s("2026-11-01", 50.0, "Food", "Zomato"), s("2026-11-02", 70.0, "Food", "zomato"), s("2026-11-03", 20.0, "Food", "Tea")), emptyList(), nov, LocalDate.of(2026, 11, 20))
        assertEquals(120.0, r.topTitles.first().second, 0.001); assertNotNull(r.biggest)
    }
    @Test fun emptyIsSafe() {
        val r = InsightsMath.analyze(emptyList(), emptyList(), nov, LocalDate.of(2026, 11, 20))
        assertEquals(0.0, r.total, 0.0); assertNull(r.changePct); assertNull(r.busiestWeekday); assertTrue(r.categories.isEmpty())
    }
    @Test fun dropFound() {
        val r = InsightsMath.analyze(listOf(s("2026-11-01", 100.0, "Food")), listOf(s("2026-10-01", 400.0, "Food")), nov, LocalDate.of(2026, 11, 20))
        assertEquals("Food", r.biggestDrop!!.category); assertNull(r.biggestRise)
    }
}
