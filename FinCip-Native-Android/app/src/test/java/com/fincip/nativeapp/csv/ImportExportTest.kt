package com.fincip.nativeapp.csv

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ImportExportTest {
    private fun imp(csv: String, existing: ExistingIndex = ExistingIndex(emptyList())) = ImportEngine.parse(csv, existing)

    @Test fun phonepeStyle() {
        val p = imp("Date,Transaction Details,Type,Amount\n"Nov 02, 2026",Paid to Swiggy,DEBIT,₹450.50\n"Nov 03, 2026",Received from Rahul,CREDIT,\"₹1,200.00\"\n")
        assertEquals(2, p.fresh.size)
        val a = p.fresh[0]; assertEquals("2026-11-02", a.date); assertEquals("expense", a.type); assertEquals(450.50, a.amount, 0.0); assertEquals("Food", a.category)
        val b = p.fresh[1]; assertEquals("income", b.type); assertEquals(1200.0, b.amount, 0.0)
    }

    @Test fun gpayStyleStatusSkipsFailed() {
        val p = imp("Date,Time,Description,Status,Amount\n05 Oct 2026,10:00,Paid to Uber,Completed,-230\n06 Oct 2026,11:00,Paid to Shop,Failed,-99\n")
        assertEquals(1, p.fresh.size); assertEquals(1, p.skipped); assertEquals("expense", p.fresh[0].type)
    }

    @Test fun paytmStyleSignedWithRs() {
        val p = imp("Date,Time,Transaction Details,Your Account,Amount,UPI Ref No.\n12/03/2026,10:10:10,Paid to Zomato,HDFC,- Rs.320.00,607123456789\n13/03/2026,10:10:10,Received from Amit,HDFC,+ Rs.500.00,607123456790\n")
        assertEquals(2, p.fresh.size)
        assertEquals("expense", p.fresh[0].type); assertEquals(320.0, p.fresh[0].amount, 0.0); assertEquals("607123456789", p.fresh[0].ref)
        assertEquals("income", p.fresh[1].type); assertEquals("2026-03-12", p.fresh[0].date)
    }

    @Test fun bankStatementWithPreambleAndDebitCredit() {
        val csv = "HDFC BANK LTD\nAccount Statement for 123456\n\nDate,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance\n" +
            "01/01/25,UPI-SWIGGY-123,0001,01/01/25,500.00,,10000.00\n02/01/25,SALARY ACME,0002,02/01/25,,\"2,000.00\",12000.00\n"
        val p = imp(csv)
        assertEquals(2, p.headerRow) // blank line dropped; header found below the bank preamble
        assertEquals(2, p.fresh.size)
        assertEquals("expense", p.fresh[0].type); assertEquals(500.0, p.fresh[0].amount, 0.0)
        assertEquals("income", p.fresh[1].type); assertEquals(2000.0, p.fresh[1].amount, 0.0); assertEquals("Salary", p.fresh[1].category)
        assertEquals("2025-01-01", p.fresh[0].date)
    }

    @Test fun debitOnlyIsNeverIncomeEvenIfPositive() {
        val p = imp("Date,Description,Debit,Credit\n2025-06-01,Rent,15000,\n2025-06-02,Refund from shop,,300\n")
        assertEquals("expense", p.fresh[0].type); assertEquals("refund", p.fresh[1].type)
    }

    @Test fun customColumnsAndSemicolons() {
        val p = imp("When;What;How much\n2024-02-29;Coffee, large;-3,50\n")
        assertEquals(1, p.fresh.size); assertEquals("2024-02-29", p.fresh[0].date); assertEquals(3.5, p.fresh[0].amount, 0.0); assertEquals("Coffee, large", p.fresh[0].title)
    }

    @Test fun dateFormatsAmbiguityAndDayFirst() {
        assertEquals(LocalDate.of(2026, 4, 3), ImportEngine.parseDate("03/04/2026", DateOrder.DAY_FIRST))
        assertEquals(LocalDate.of(2026, 3, 4), ImportEngine.parseDate("03/04/2026", DateOrder.MONTH_FIRST))
        assertEquals(LocalDate.of(2026, 12, 25), ImportEngine.parseDate("12/25/2026", DateOrder.DAY_FIRST)) // only valid reading
        assertEquals(LocalDate.of(2025, 1, 5), ImportEngine.parseDate("5-Jan-2025", DateOrder.DAY_FIRST))
        assertEquals(LocalDate.of(2025, 1, 5), ImportEngine.parseDate("Jan 5, 2025", DateOrder.DAY_FIRST))
        assertEquals(LocalDate.of(2025, 1, 5), ImportEngine.parseDate("2025-01-05T10:20:30", DateOrder.DAY_FIRST))
        assertEquals(LocalDate.of(2025, 1, 5), ImportEngine.parseDate("20250105", DateOrder.DAY_FIRST))
        assertNull(ImportEngine.parseDate("31/02/2025", DateOrder.DAY_FIRST))
        val (o, guessed) = ImportEngine.detectDateOrder(sequenceOf("13/01/2025", "02/02/2025")); assertEquals(DateOrder.DAY_FIRST, o); assertFalse(guessed)
        val (o2, _) = ImportEngine.detectDateOrder(sequenceOf("01/13/2025", "02/02/2025")); assertEquals(DateOrder.MONTH_FIRST, o2)
    }

    @Test fun amountParsing() {
        assertEquals(1234.56, ImportEngine.parseAmount("₹1,234.56")!!.first, 0.0)
        assertEquals(-100.0, ImportEngine.parseAmount("(100.00)")!!.first, 0.0)
        assertEquals(-100.0, ImportEngine.parseAmount("100.00-")!!.first, 0.0)
        assertEquals(1234.56, ImportEngine.parseAmount("1.234,56")!!.first, 0.0)
        assertEquals("expense", ImportEngine.parseAmount("250.00 Dr")!!.second)
        assertEquals("income", ImportEngine.parseAmount("250.00 Cr")!!.second)
        assertEquals(99.99, ImportEngine.parseAmount("Rs. 99.99")!!.first, 0.0)
        assertNull(ImportEngine.parseAmount("abc"))
    }

    @Test fun duplicatesAgainstExistingAndRepeatedImport() {
        val csv = "Date,Description,Amount\n2025-01-01,Tea,-20\n2025-01-01,Tea,-20\n2025-01-02,Bus,-15\n"
        val first = imp(csv)
        assertEquals(3, first.fresh.size) // two identical coffees on one day are legitimate
        assertTrue(first.rows[1].issue.isNotBlank())
        val idx = ExistingIndex(first.fresh.map { Triple(it.date, "%.2f-".format(java.util.Locale.US, it.amount), it.title) })
        val again = imp(csv, idx)
        assertEquals(0, again.fresh.size); assertEquals(3, again.duplicates)
        // existing has only one tea -> only one of the two is a duplicate
        val partial = imp(csv, ExistingIndex(listOf(Triple("2025-01-01", "20.00-", "Tea"))))
        assertEquals(1, partial.duplicates); assertEquals(2, partial.fresh.size)
    }

    @Test fun duplicateByReference() {
        val p = imp("Date,Description,Amount,Transaction ID\n2025-01-01,A,-1,X1\n", ExistingIndex(emptyList(), listOf("X1")))
        assertEquals(1, p.duplicates)
    }

    @Test fun transfersAndRefundsAreNotIncomeOrExpense() {
        val p = imp("Date,Description,Debit,Credit\n2025-03-01,Self transfer to savings,1000,\n2025-03-02,Credit card payment HDFC,5000,\n2025-03-03,Amazon refund,,799\n2025-03-04,Salary March,,50000\n")
        assertEquals(listOf("transfer", "transfer", "refund", "income"), p.fresh.map { it.type })
        val s = summarize(p.fresh.map { ExTxn("i", LocalDate.parse(it.date), it.amount, it.type, it.title, it.category, "", "INR", "", "", "") })
        assertEquals(50000.0, s.income, 0.0); assertEquals(-799.0, s.expense, 0.0); assertEquals(2, s.transfers)
    }

    @Test fun malformedRowsAreReportedNotCrashing() {
        val p = imp("Date,Description,Amount\n2025-01-01,Good,-10\nnot-a-date,Bad date,-5\n2025-01-03,No amount,\n,,\n2025-01-04,\"Unclosed,-7\n")
        assertEquals(1, p.fresh.size)
        assertTrue(p.errors >= 2)
        assertTrue(p.rows.filter { it.state == RowState.ERROR }.all { it.issue.isNotBlank() })
    }

    @Test fun missingOptionalColumnsAndHeaderless() {
        val p = imp("Date,Amount\n2025-05-05,-12.5\n")
        assertEquals(1, p.fresh.size); assertEquals("Imported transaction", p.fresh[0].title)
        val h = imp("2025-05-05,Lunch,-100\n2025-05-06,Dinner,-200\n")
        assertEquals(2, h.fresh.size); assertEquals(100.0, h.fresh[0].amount, 0.0)
    }

    @Test fun signlessAmountsAssumedExpenseWithWarning() {
        val p = imp("Date,Description,Amount\n2025-01-01,A,10\n2025-01-02,B,20\n")
        assertTrue(p.fresh.all { it.type == "expense" }); assertTrue(p.warnings.any { it.contains("no sign", true) })
    }

    @Test fun typeColumnWins() {
        val p = imp("Date,Description,Amount,Type\n2025-01-01,A,10,Income\n2025-01-02,B,20,Expense\n")
        assertEquals(listOf("income", "expense"), p.fresh.map { it.type })
    }

    @Test fun historicalYearStaysInItsMonths() {
        val sb = StringBuilder("Date,Description,Amount\n")
        for (m in 1..12) for (d in listOf(3, 17)) sb.append("2025-%02d-%02d,Item $m-$d,-%d\n".format(m, d, 100 + m))
        val p = imp(sb.toString())
        assertEquals(24, p.fresh.size)
        val byMonth = p.fresh.groupBy { YearMonth.parse(it.date.substring(0, 7)) }
        assertEquals(12, byMonth.size)
        for (m in 1..12) assertEquals(2, byMonth[YearMonth.of(2025, m)]!!.size)
        assertTrue(byMonth.keys.none { it.year == 2026 }) // current month (Nov 2026) untouched
        assertEquals("2025-01-03", p.minDate); assertEquals("2025-12-17", p.maxDate)
    }

    @Test fun previousYearsAndFebruary2024() {
        val p = imp("Date,Description,Amount\n2024-02-29,Leap day,-1\n2025-01-31,Jan,-2\n2025-12-31,Dec,-3\n")
        assertEquals(listOf("2024-02-29", "2025-01-31", "2025-12-31"), p.fresh.map { it.date })
    }

    @Test fun largeFileTenThousandRows() {
        val sb = StringBuilder("Date,Narration,Withdrawal Amt.,Deposit Amt.,Ref\n")
        for (i in 0 until 12000) { val d = LocalDate.of(2025, 1, 1).plusDays((i % 365).toLong()); if (i % 5 == 0) sb.append("$d,Salary part $i,,${100 + i % 7}.25,R$i\n") else sb.append("$d,UPI Payment $i,${10 + i % 50}.10,,R$i\n") }
        val t0 = System.nanoTime(); val p = imp(sb.toString()); val ms = (System.nanoTime() - t0) / 1_000_000
        assertEquals(12000, p.fresh.size); assertEquals(0, p.errors)
        assertTrue("took ${ms}ms", ms < 8000)
        assertEquals(2400, p.fresh.count { it.type == "income" })
    }

    @Test fun utf8QuotesCommasAndCrLf() {
        val bytes = "Date,Description,Amount\r\n2025-01-01,\"Café “Ünï”, \"\"quoted\"\" \n2nd line\",-5.00\r\n".toByteArray(Charsets.UTF_8)
        val p = imp(Csv.decode(bytes))
        assertEquals(1, p.fresh.size); assertTrue(p.fresh[0].title.contains("Café")); assertTrue(p.fresh[0].title.contains("\"quoted\""))
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Date,Amount\n2025-01-01,-1\n".toByteArray()
        assertEquals(1, imp(Csv.decode(bom)).fresh.size)
        val win = "Date,Description,Amount\n2025-01-01,Café,-1\n".toByteArray(charset("windows-1252"))
        assertEquals("Café", imp(Csv.decode(win)).fresh[0].title)
    }

    // ---------------- export ----------------
    private val sample = listOf(
        ExTxn("1", LocalDate.of(2025, 1, 15), 450.5, "expense", "Swiggy, \"Order\"", "Food", "HDFC", "INR", "dinner", "", "REF1"),
        ExTxn("2", LocalDate.of(2025, 2, 1), 50000.0, "income", "Salary", "Salary", "HDFC", "INR", "", "", "REF2"),
        ExTxn("3", LocalDate.of(2026, 11, 2), 0.1, "expense", "=cmd()", "Other", "Cash", "INR", "", "", "")
    )

    @Test fun everyProfileExportsAllRowsInColumnOrder() {
        for (pr in listOf(Profiles.universal) + Profiles.all.filter { it.enabled }) {
            val out = pr.build(sample)
            val rows = Csv.parse(out, pr.delimiter)
            assertEquals("${pr.name}: rows", 1 + pr.headerLines.size + sample.size, rows.size)
            assertEquals("${pr.name}: header order", pr.headers, rows[pr.headerLines.size])
            assertTrue("${pr.name}: width", rows.all { it.size == pr.headers.size })
            // no missing / duplicate transactions: every amount appears exactly once
            assertTrue(pr.fileName(ExportFilter(), sample).startsWith("FinCip_"))
            assertFalse("${pr.name}: formula guard", out.contains("\n=cmd") || out.contains(",=cmd"))
        }
    }

    @Test fun debitCreditRepresentation() {
        val bank = Csv.parse(Profiles.hdfc.build(sample))
        assertEquals(listOf("15/01/25", "Swiggy, \"Order\" | dinner", "REF1", "15/01/25", "450.50", "", ""), bank[1])
        assertEquals("", bank[2][4]); assertEquals("50000.00", bank[2][5])
        val pp = Csv.parse(Profiles.phonepe.build(sample))
        assertEquals(listOf("Date", "Transaction Details", "Type", "Amount"), pp[0])
        assertEquals(listOf("Jan 15, 2025", "Paid to Swiggy, \"Order\"", "DEBIT", "450.50"), pp[1])
        assertEquals("CREDIT", pp[2][2])
        assertEquals("0.10", pp[3][3]) // precision kept
    }

    @Test fun trackerFormats() {
        val mm = Profiles.moneyManager.build(sample)
        assertTrue(mm.startsWith("Date\tAccount\tCategory\tSubcategory\tNote\tAmount\tIncome/Expense\tDescription"))
        assertTrue(mm.contains("01/15/2025")); assertEquals("tsv", Profiles.moneyManager.extension)
        val sp = Csv.parse(Profiles.spendee.build(sample))
        assertEquals("income", sp[2][3]); assertEquals("expense", sp[1][3])
        val wl = Csv.parse(Profiles.walletBudgetBakers.build(sample))
        assertEquals("-450.50", wl[1][3]); assertEquals("50000.00", wl[2][3])
    }

    @Test fun honestLabels() {
        assertEquals(Compat.OFFICIAL, Profiles.spendee.compat); assertEquals(Compat.OFFICIAL, Profiles.moneyManager.compat)
        assertEquals(Compat.BEST_EFFORT, Profiles.walletBudgetBakers.compat)
        assertFalse(Profiles.walnut.enabled)
        for (p in listOf(Profiles.phonepe, Profiles.gpay, Profiles.paytm, Profiles.bhim, Profiles.hdfc, Profiles.sbi, Profiles.icici, Profiles.axis, Profiles.kotak, Profiles.idfc, Profiles.cred))
            assertEquals(p.name, Compat.STATEMENT, p.compat)
        assertTrue(Compat.STATEMENT.note.contains("external transaction import not officially supported"))
        assertTrue(Profiles.all.count { it.enabled } >= 12)
    }

    @Test fun filtersAndNaming() {
        assertEquals(1, ExportFilter.month(YearMonth.of(2025, 1)).apply(sample).size)
        assertEquals(2, ExportFilter.year(2025).apply(sample).size)
        assertEquals(1, ExportFilter(kind = Kind.INCOME).apply(sample).size)
        assertEquals(2, ExportFilter(kind = Kind.EXPENSE).apply(sample).size)
        assertEquals(1, ExportFilter(categories = setOf("Food")).apply(sample).size)
        assertEquals(1, ExportFilter(accounts = setOf("Cash")).apply(sample).size)
        assertEquals(1, ExportFilter(from = LocalDate.of(2025, 1, 10), to = LocalDate.of(2025, 1, 20)).apply(sample).size)
        assertEquals("FinCip_Universal_Transactions_2025.csv", Profiles.universal.fileName(ExportFilter.year(2025), sample))
        assertEquals("FinCip_PhonePe_Export_2025-01.csv", Profiles.phonepe.fileName(ExportFilter.month(YearMonth.of(2025, 1)), sample))
        assertEquals("FinCip_Universal_Transactions_2025-2026.csv", Profiles.universal.fileName(ExportFilter(), sample))
    }

    @Test fun roundTripUniversalExportThenImport() {
        val out = Profiles.universal.build(sample.take(2))
        val back = imp(out)
        assertEquals(2, back.fresh.size)
        assertEquals(listOf("2025-01-15", "2025-02-01"), back.fresh.map { it.date })
        assertEquals(450.5, back.fresh[0].amount, 0.0); assertEquals("expense", back.fresh[0].type); assertEquals("income", back.fresh[1].type)
        assertEquals("Swiggy, \"Order\"", back.fresh[0].title)
    }
}
