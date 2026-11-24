package com.fincip.nativeapp.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndiaMathTest {
    @Test fun sipZeroRateIsPlainSum() { assertEquals(12000.0, IndiaMath.sipFutureValue(1000.0, 0.0, 12), 0.001) }

    @Test fun sipGrowsAboveContributions() {
        val fv = IndiaMath.sipFutureValue(5000.0, 12.0, 120)
        assertTrue(fv > 5000.0 * 120)
        // annuity-due, 1% monthly, 120 months: about 11.6 lakh
        assertEquals(1161695.0, fv, 2000.0)
    }

    @Test fun lumpQuarterlyFd() { assertEquals(100000 * Math.pow(1.0175, 4.0), IndiaMath.lumpFutureValue(100000.0, 7.0, 1.0, 4), 0.01) }

    @Test fun fiscalYearBoundaries() {
        assertEquals(LocalDate.of(2026, 4, 1), IndiaMath.fiscalYearStart(LocalDate.of(2026, 4, 1)))
        assertEquals(LocalDate.of(2025, 4, 1), IndiaMath.fiscalYearStart(LocalDate.of(2026, 3, 31)))
        assertEquals("FY 2026-27", IndiaMath.fiscalYearLabel(LocalDate.of(2026, 11, 24)))
        assertEquals("FY 2008-09", IndiaMath.fiscalYearLabel(LocalDate.of(2008, 5, 1)))
    }

    @Test fun contributedInFyCountsOnlyCurrentYear() {
        val flows = listOf(LocalDate.of(2026, 3, 31) to -500.0, LocalDate.of(2026, 4, 1) to -1000.0, LocalDate.of(2026, 11, 1) to -2000.0)
        assertEquals(3000.0, IndiaMath.contributedInFy(flows, LocalDate.of(2026, 11, 24)), 0.001)
    }

    @Test fun projectionNullWhenMaturityBeforeStart() {
        assertNull(IndiaMath.projectedValue(IndiaKinds.of("FD"), 1000.0, 0.0, 7.0, LocalDate.of(2026, 1, 1), LocalDate.of(2025, 1, 1)))
    }

    @Test fun projectionForFd() {
        val v = IndiaMath.projectedValue(IndiaKinds.of("FD"), 100000.0, 0.0, 7.0, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1))!!
        assertEquals(107186.0, v, 50.0)
    }

    @Test fun doubling() { assertEquals(10.3, IndiaMath.doublingYears(7.0)!!, 0.001); assertNull(IndiaMath.doublingYears(0.0)) }

    @Test fun cumulativeSeries() {
        val s = IndiaMath.cumulativeByMonth(listOf(LocalDate.of(2026, 9, 5) to -100.0, LocalDate.of(2026, 11, 2) to -50.0), LocalDate.of(2026, 11, 24), 3)
        assertEquals(listOf(100.0, 100.0, 150.0), s.map { it.second })
    }

    @Test fun unknownKindFallsBackToOther() { assertEquals("Other", IndiaKinds.of("zzz").id) }
}
