package com.fincip.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetTableTest {
    /** Places a table with its top-left cell at (row, col) inside a bigger grid, with optional metadata lines above. */
    private fun grid(rowOff: Int, colOff: Int, table: List<List<String>>, meta: List<String> = emptyList(), after: List<List<String>> = emptyList()): List<List<String>> {
        val w = colOff + (table.maxOf { it.size }) + 2
        val g = MutableList(rowOff + table.size + after.size + 2) { MutableList(w) { "" } }
        meta.forEachIndexed { i, m -> if (i < rowOff) g[i][0] = m }
        table.forEachIndexed { r, row -> row.forEachIndexed { c, v -> g[rowOff + r][colOff + c] = v } }
        after.forEachIndexed { r, row -> row.forEachIndexed { c, v -> g[rowOff + table.size + 1 + r][colOff + c] = v } }
        return g
    }
    private val std = listOf(
        listOf("Date", "Narration", "Withdrawal Amt.", "Deposit Amt.", "Closing Balance"),
        listOf("01/11/2026", "UPI-SWIGGY", "250.00", "", "9,750.00"),
        listOf("02/11/2026", "SALARY", "", "50,000.00", "59,750.00"),
        listOf("03/11/2026", "ATM CASH", "2,000.00", "", "57,750.00")
    )

    private fun check(t: TableFound?) {
        assertNotNull(t); t!!
        assertEquals(listOf("Date", "Description", "Debit", "Credit", "Balance"), t.headers)
        assertEquals(3, t.rows.size)
        assertEquals("UPI-SWIGGY", t.rows[0][1]); assertEquals("50,000.00", t.rows[1][3]); assertEquals("57,750.00", t.rows[2][4])
    }

    @Test fun openingBalanceFromRunningBalance() { assertEquals(10000.0, SheetTable.find(grid(0, 0, std))!!.openingBalance!!, 0.001) }
    @Test fun openingBalanceWhenNewestFirst() { val rev = listOf(std[0]) + std.drop(1).reversed(); assertEquals(10000.0, SheetTable.find(grid(0, 0, rev))!!.openingBalance!!, 0.001) }
    @Test fun startsAtA1() = check(SheetTable.find(grid(0, 0, std)))
    @Test fun startsAtO15() = check(SheetTable.find(grid(14, 14, std, List(14) { "Account info $it" })))
    @Test fun startsAtAA20() = check(SheetTable.find(grid(19, 26, std, List(19) { "Name: X $it" })))
    @Test fun twentyMetaRows() {
        val meta = List(22) { if (it == 3) "Date range: 01/10/2026 to 30/11/2026" else "Account line $it" }
        check(SheetTable.find(grid(22, 0, std, meta)))
    }
    @Test fun emptyRowsBetween() {
        val t = listOf(std[0], std[1], listOf("", "", "", "", ""), std[2], listOf("", "", "", "", ""), listOf("", "", "", "", ""), std[3])
        check(SheetTable.find(grid(5, 2, t)))
    }
    @Test fun summaryAfterIsIgnored() {
        val after = listOf(listOf("Statement Summary", "", "", "", ""), listOf("Total Debits", "", "2250.00", "", ""), listOf("Total Credits", "", "", "50000.00", ""))
        check(SheetTable.find(grid(4, 1, std, after = after)))
    }
    @Test fun mergedTwoRowHeader() {
        val t = listOf(
            listOf("Txn Date", "Particulars", "Amount", "Amount", "Balance"),
            listOf("", "", "Debit", "Credit", ""),
            listOf("01/11/2026", "UPI-SWIGGY", "250.00", "", "9,750.00"),
            listOf("02/11/2026", "SALARY", "", "50,000.00", "59,750.00"),
            listOf("03/11/2026", "ATM CASH", "2,000.00", "", "57,750.00")
        )
        check(SheetTable.find(grid(8, 3, t)))
    }
    @Test fun singleAmountWithDrCrSuffix() {
        val t = listOf(
            listOf("Date", "Description", "Amount", "Balance"),
            listOf("01-Nov-2026", "SWIGGY", "250.00 Dr", "9750"),
            listOf("02-Nov-2026", "SALARY", "50,000.00 Cr", "59750"),
            listOf("03-Nov-2026", "ATM", "2,000.00 Dr", "57750")
        )
        val f = SheetTable.find(grid(6, 4, t))!!
        val d = f.headers.indexOf("Debit"); val c = f.headers.indexOf("Credit")
        assertEquals("250", f.rows[0][d]); assertEquals("", f.rows[0][c]); assertEquals("50000", f.rows[1][c]); assertEquals("2000", f.rows[2][d])
    }
    @Test fun singleAmountWithTypeColumn() {
        val t = listOf(
            listOf("Transaction Date", "Narration", "Amount", "Dr/Cr", "Balance"),
            listOf("2026-11-01", "A", "100", "DR", "900"), listOf("2026-11-02", "B", "500", "CR", "1400"), listOf("2026-11-03", "C", "50", "DR", "1350")
        )
        val f = SheetTable.find(grid(2, 0, t))!!
        assertEquals("100", f.rows[0][f.headers.indexOf("Debit")]); assertEquals("500", f.rows[1][f.headers.indexOf("Credit")])
        assertTrue(f.notes.any { it.contains("Running balance matches") })
    }
    @Test fun signedAmounts() {
        val t = listOf(listOf("Date", "Details", "Amount"), listOf("01/11/2026", "A", "-100.00"), listOf("02/11/2026", "B", "250.00"))
        val f = SheetTable.find(grid(0, 0, t))
        // signs alone: negative rows become debits; the positive one has no explicit direction and stays unsplit-ambiguous
        assertNotNull(f); assertEquals(2, f!!.rows.size)
    }
    @Test fun missingOptionalColumns() {
        val t = listOf(listOf("Date", "Narration", "Debit", "Credit"), listOf("01/11/2026", "A", "10", ""), listOf("02/11/2026", "B", "", "20"))
        val f = SheetTable.find(grid(3, 3, t))!!
        assertEquals(listOf("Date", "Description", "Debit", "Credit"), f.headers); assertEquals(2, f.rows.size)
    }
    @Test fun multilineNarrationIsJoined() {
        val t = listOf(std[0], std[1], listOf("", "REF 123456", "", "", ""), std[2])
        val f = SheetTable.find(grid(0, 0, t))!!
        assertEquals(2, f.rows.size); assertEquals("UPI-SWIGGY REF 123456", f.rows[0][1])
    }
    @Test fun notATransactionSheet() {
        assertNull(SheetTable.find(listOf(listOf("Name", "Age"), listOf("A", "5"))))
        assertNull(SheetTable.find(emptyList()))
    }
    @Test fun metadataDateIsNotATable() {
        val g = listOf(listOf("Statement of account", "", ""), listOf("Date: 01/11/2026", "Branch", "Noida")) + std
        check(SheetTable.find(g))
    }
    @Test fun manualHeaderRow() {
        val g = grid(6, 2, std)
        val f = SheetTable.extractAt(g, 6)!!
        assertEquals(3, f.rows.size)
        assertNull(SheetTable.extractAt(g, 999))
    }
    @Test fun csvOutputQuotes() {
        val f = SheetTable.find(grid(0, 0, listOf(std[0], listOf("01/11/2026", "A, B \"x\"", "5", "", "1"), std[2])))!!
        assertTrue(SheetTable.toCsv(f).contains("\"A, B \"\"x\"\"\""))
    }
    @Test fun dateFormats() {
        listOf("01/11/2026", "1-11-26", "2026-11-01", "01 Nov 2026", "01-Nov-26", "Nov 1, 2026", "01.11.2026 10:15:00").forEach { assertTrue(it, SheetTable.looksLikeDate(it)) }
        listOf("Balance", "12345", "Date: x", "").forEach { assertTrue(it, !SheetTable.looksLikeDate(it)) }
    }
}
