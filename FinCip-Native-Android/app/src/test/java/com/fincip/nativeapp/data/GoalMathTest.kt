package com.fincip.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalMathTest {
    @Test fun monthsToSaveNoReturn() { assertEquals(10, GoalMath.monthsToSave(10000.0, 1000.0)); assertEquals(11, GoalMath.monthsToSave(10001.0, 1000.0)) }
    @Test fun monthsToSaveNothingSaved() { assertNull(GoalMath.monthsToSave(500.0, 0.0)); assertEquals(0, GoalMath.monthsToSave(0.0, 0.0)) }
    @Test fun returnShortensTime() { assertTrue(GoalMath.monthsToSave(100000.0, 2000.0, 12.0)!! < GoalMath.monthsToSave(100000.0, 2000.0)!!) }
    @Test fun requiredMonthly() {
        assertEquals(1000.0, GoalMath.requiredMonthly(12000.0, 12), 0.001)
        val withReturn = GoalMath.requiredMonthly(12000.0, 12, 12.0)
        assertTrue(withReturn < 1000.0)
        // saving that much really reaches the goal
        assertEquals(12000.0, withReturn * ((Math.pow(1.01, 12.0) - 1) / 0.01), 0.5)
    }
    @Test fun debtPayoffZeroInterest() {
        val p = GoalMath.simulateDebt(10000.0, 1000.0, 0.0)!!
        assertEquals(10, p.months); assertEquals(0.0, p.totalInterest, 0.001)
    }
    @Test fun debtWithInterestCostsMore() {
        val p = GoalMath.simulateDebt(100000.0, 5000.0, 18.0)!!
        assertTrue(p.months > 20 && p.totalInterest > 0)
        val faster = GoalMath.simulateDebt(100000.0, 6000.0, 18.0)!!
        assertTrue(faster.months < p.months && faster.totalInterest < p.totalInterest)
    }
    @Test fun debtNeverPaidWhenPaymentBelowInterest() {
        assertNull(GoalMath.simulateDebt(100000.0, 1000.0, 24.0))
        assertNull(GoalMath.simulateDebt(100000.0, 0.0, 10.0))
    }
    @Test fun clearedDebtIsZero() { assertEquals(0, GoalMath.simulateDebt(0.0, 0.0, 10.0)!!.months) }
    @Test fun purposesHaveIcons() { (GoalPurposes.saving + GoalPurposes.debt).forEach { assertNotNull(GoalPurposes.icon(it)) } }
}
