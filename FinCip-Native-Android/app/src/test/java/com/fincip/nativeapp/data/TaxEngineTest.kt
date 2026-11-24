package com.fincip.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxEngineTest {
    private fun r(c: String, g: Double, f: (TaxInput) -> TaxInput = { it }) = TaxEngine.compute(f(TaxInput(c, g)))

    @Test fun indiaNewRegimeRebateMakesTwelveLakhFree() {
        // 12.75L gross - 75k standard deduction = 12L taxable, fully rebated
        assertEquals(0.0, r("IN", 1275000.0).total, 0.5)
    }
    @Test fun indiaNewRegimeAboveRebate() {
        // 20L gross -> 19.25L taxable: 0+20k+40k+60k+(19.25-16)*20% = 770k? slabs: 4-8:20k, 8-12:40k, 12-16:60k, 16-19.25:65k
        assertEquals(185000.0 * 1.04, r("IN", 2000000.0).incomeTax, 1.0)
    }
    @Test fun usSingleHundredK() {
        val x = r("US", 100000.0)
        assertEquals(13170.0 + 7650.0, x.incomeTax + x.social, 1.0)
    }
    @Test fun ukSixtyK() {
        val x = r("GB", 60000.0)
        assertEquals(11432.0, x.incomeTax, 0.5)
        assertEquals(3210.6, x.social, 0.5)
    }
    @Test fun ukAllowanceTaper() {
        val x = r("GB", 125140.0)
        assertEquals(0.0, x.deduction, 0.5)
    }
    @Test fun germanTariffKnownPoints() {
        assertEquals(0.0, TaxEngine.deTariff(12000.0), 0.0)
        assertEquals(10548.0, TaxEngine.deTariff(50000.0), 3.0)
        assertEquals(0.42 * 100000 - 11135.63, TaxEngine.deTariff(100000.0), 1.0)
    }
    @Test fun chinaThreeHundredK() {
        // 300k - 60k - 10.5% social = 208,500 taxable -> 24,780
        assertEquals(24780.0, r("CN", 300000.0).incomeTax, 1.0)
    }
    @Test fun canadaHundredK() {
        assertEquals(14392.9, r("CA", 100000.0).incomeTax, 1.0)
    }
    @Test fun australiaNinetyK() {
        val x = r("AU", 90000.0)
        assertEquals(17520.0, x.incomeTax, 0.5)
        assertEquals(1800.0, x.social, 0.5)
    }
    @Test fun singaporeCpfOverrideZero() {
        assertEquals(5535.0, r("SG", 100000.0) { it.copy(socialPct = 0.0) }.incomeTax, 1.0)
    }
    @Test fun marginalRateIsBetweenZeroAndHundredAndRises() {
        val low = r("US", 40000.0).marginalPct
        val high = r("US", 300000.0).marginalPct
        assertTrue(low in 0.0..100.0 && high in 0.0..100.0 && high > low)
    }
    @Test fun everyCountryProducesSaneResult() {
        TaxEngine.countries.forEach { c ->
            val x = TaxEngine.compute(TaxInput(c.code, 60000.0))
            assertTrue("${c.code} take-home", x.takeHome in 0.0..60000.0)
            assertTrue("${c.code} lines", x.lines.isNotEmpty())
            assertTrue("${c.code} not NaN", !x.effectivePct.isNaN() && !x.marginalPct.isNaN())
        }
    }
    @Test fun zeroIncomeIsZeroTax() { TaxEngine.countries.forEach { assertEquals(it.code, 0.0, TaxEngine.compute(TaxInput(it.code, 0.0)).total, 0.01) } }
}
