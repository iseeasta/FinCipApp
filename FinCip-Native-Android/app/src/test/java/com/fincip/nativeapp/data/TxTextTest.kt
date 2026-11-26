package com.fincip.nativeapp.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TxTextTest {
    @Test fun upiNameIsExtracted() {
        val c = TxText.clean("UPI-HIMANSHU SHARMA-**********@PTYES-YESB0PTMUPI-123456789012-PAYMENT FROM PHONE")
        assertEquals("Himanshu Sharma", c.name); assertEquals("UPI", c.method)
    }
    @Test fun upiPhoneIdFallsBack() {
        val c = TxText.clean("UPI-9411692419PTYES-******2419@PTYES-YESB0PTMUPI-1234-PAYMENT FROM PHONE")
        assertEquals("UPI payment", c.name); assertEquals("UPI", c.method)
    }
    @Test fun plainTitleKept() { assertEquals("Swiggy order", TxText.clean("Swiggy order").name) }
    @Test fun shoutingGetsTitleCase() { assertEquals("Interest Paid Till", TxText.clean("INTEREST PAID TILL 30062025").name) }
    @Test fun emptyIsSafe() { assertEquals("Transaction", TxText.clean("  ").name) }
    @Test fun dayLabels() {
        val t = LocalDate.of(2026, 11, 26)
        assertEquals("24 Nov", TxText.dayLabel("2026-11-24", t)); assertEquals("3 Jan 2025", TxText.dayLabel("2025-01-03", t)); assertEquals("bad", TxText.dayLabel("bad", t))
    }
}
