package com.fincip.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetWorthMathTest {
    @Test fun savedGroupWins() { assertEquals("Gold", NetWorthGroups.groupOf("Whatever", "Asset", "Gold")) }
    @Test fun savedGroupFromWrongSideIsIgnored() { assertEquals("Other debt", NetWorthGroups.groupOf("Whatever", "Liability", "Gold")) }
    @Test fun guessesAssets() {
        assertEquals("Cash & bank", NetWorthGroups.groupOf("HDFC savings account", "Asset", ""))
        assertEquals("Investments", NetWorthGroups.groupOf("Nifty ETF", "Asset", ""))
        assertEquals("Property", NetWorthGroups.groupOf("Flat in Noida", "Asset", ""))
        assertEquals("Other asset", NetWorthGroups.groupOf("Laptop", "Asset", ""))
    }
    @Test fun guessesLiabilities() {
        assertEquals("Credit card", NetWorthGroups.groupOf("ICICI credit card", "Liability", ""))
        assertEquals("Education loan", NetWorthGroups.groupOf("Education loan", "Liability", ""))
        assertEquals("Other debt", NetWorthGroups.groupOf("Friend", "Liability", ""))
    }
    @Test fun ratioAndBands() {
        assertNull(NetWorthGroups.debtRatio(0.0, 100.0))
        assertEquals(25.0, NetWorthGroups.debtRatio(400.0, 100.0)!!, 0.001)
        assertEquals("Debt free", NetWorthGroups.band(0.0, 0.0))
        assertEquals("Comfortable", NetWorthGroups.band(25.0, 100.0))
        assertEquals("Moderate", NetWorthGroups.band(45.0, 100.0))
        assertEquals("Stretched", NetWorthGroups.band(80.0, 100.0))
        assertEquals("Owe more than you own", NetWorthGroups.band(150.0, 100.0))
        assertEquals("No assets recorded", NetWorthGroups.band(null, 50.0))
    }
}
