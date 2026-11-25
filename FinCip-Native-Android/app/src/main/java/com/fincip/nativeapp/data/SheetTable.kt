package com.fincip.nativeapp.data

/** Rule-based discovery of a transaction table inside an arbitrary worksheet grid. No AI, no network, no Android deps. */
class TableFound(
    val headerRow: Int, val firstCol: Int, val headers: List<String>, val rows: List<List<String>>,
    val notes: List<String>, val score: Int, val openingBalance: Double? = null
)

object SheetTable {
    private enum class Role { DATE, DESC, DEBIT, CREDIT, AMOUNT, BALANCE, REF, TYPE }

    private val synonyms: Map<Role, Set<String>> = mapOf(
        Role.DATE to setOf("date", "transactiondate", "txndate", "valuedate", "postingdate", "trandate", "transdate", "bookingdate", "txndt", "valuedt", "dateoftransaction", "tdate"),
        Role.DESC to setOf("description", "narration", "particulars", "transactiondetails", "remarks", "details", "transactionremarks", "transactiondescription", "narrative", "transactionparticulars", "paymentdetails"),
        Role.DEBIT to setOf("debit", "debits", "withdrawal", "withdrawals", "withdrawn", "dr", "amountdebited", "withdrawalamt", "withdrawalamount", "debitamount", "debitamt", "paidout", "moneyout", "withdrawalamtinr"),
        Role.CREDIT to setOf("credit", "credits", "deposit", "deposits", "deposited", "cr", "amountcredited", "depositamt", "depositamount", "creditamount", "creditamt", "paidin", "moneyin", "depositamtinr"),
        Role.AMOUNT to setOf("amount", "amountinr", "txnamount", "transactionamount", "amt", "amountrs"),
        Role.BALANCE to setOf("balance", "closingbalance", "availablebalance", "runningbalance", "balanceamt", "balanceinr", "bal", "closingbal"),
        Role.REF to setOf("refno", "reference", "referenceno", "referencenumber", "chqrefno", "chqrefnumber", "utr", "utrno", "transactionid", "txnid", "chequeno", "chqno", "instrumentno", "refnochqno", "chequenumber", "transactionref"),
        Role.TYPE to setOf("type", "drcr", "crdr", "transactiontype", "txntype", "dbcr", "crdb")
    )
    private val summaryWords = listOf("total", "summary", "closing balance", "opening balance", "statement", "legend", "generated", "disclaimer", "end of", "page ", "important", "note:", "grand")

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun roleOf(label: String): Role? {
        val n = norm(label)
        if (n.isEmpty()) return null
        synonyms.forEach { (r, set) -> if (n in set) return r }
        if (n.length > 32) return null
        return when {
            "narration" in n || "particulars" in n -> Role.DESC
            "withdrawal" in n || n.startsWith("debit") -> Role.DEBIT
            "deposit" in n || n.startsWith("credit") -> Role.CREDIT
            "balance" in n -> Role.BALANCE
            n.endsWith("date") && n.length <= 18 -> Role.DATE
            else -> null
        }
    }

    private val dateRegexes = listOf(
        Regex("""^\d{1,2}[/\-. ]\d{1,2}[/\-. ]\d{2,4}\b.*"""),
        Regex("""^\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2}\b.*"""),
        Regex("""^\d{1,2}[ \-/.]+[A-Za-z]{3,9}\.?[ \-/.,]+\d{2,4}\b.*"""),
        Regex("""^[A-Za-z]{3,9}\.? \d{1,2},? \d{4}\b.*""")
    )
    fun looksLikeDate(s: String): Boolean { val t = s.trim(); return t.length in 6..40 && dateRegexes.any { it.matches(t) } }

    /** Parses "1,234.50", "(500)", "₹ 1,200.00 Dr", "-45". Returns magnitude info via [amountOf]. */
    private fun amountOf(raw: String): Double? {
        var t = raw.trim()
        if (t.isEmpty()) return null
        var neg = false
        if (t.startsWith("(") && t.endsWith(")")) { neg = true; t = t.substring(1, t.length - 1) }
        t = t.replace(Regex("(?i)\\b(dr|cr)\\.?$"), "").replace(Regex("(?i)^(dr|cr)\\.?\\s"), "")
        t = t.replace(Regex("[₹$€£¥,\\s]|INR|Rs\\.?|USD|EUR|GBP", RegexOption.IGNORE_CASE), "")
        if (t.startsWith("-")) { neg = !neg; t = t.drop(1) }
        if (t.endsWith("-")) { neg = !neg; t = t.dropLast(1) }
        val v = t.toDoubleOrNull() ?: return null
        return if (neg) -v else v
    }
    private fun drCrOf(raw: String): String? {
        val t = raw.trim().lowercase()
        return when {
            Regex("(^|\\s)dr\\.?$|^dr\\b|^d$|debit|withdraw").containsMatchIn(t) && !t.contains("credit") -> "D"
            Regex("(^|\\s)cr\\.?$|^cr\\b|^c$|credit|deposit").containsMatchIn(t) -> "C"
            else -> null
        }
    }

    private fun cell(g: List<List<String>>, r: Int, c: Int): String = g.getOrNull(r)?.getOrNull(c)?.trim().orEmpty()
    private fun width(g: List<List<String>>) = g.maxOfOrNull { it.size } ?: 0

    private class Layout(val roles: Map<Role, Int>, val labels: List<String>)

    private fun layoutFor(g: List<List<String>>, r: Int, twoRows: Boolean): Layout {
        val w = width(g)
        val labels = (0 until w).map { c ->
            val a = cell(g, r, c); val b = if (twoRows) cell(g, r + 1, c) else ""
            when { a.isNotEmpty() && b.isNotEmpty() && !a.equals(b, true) -> "$a $b"; a.isNotEmpty() -> a; else -> b }
        }
        val roles = LinkedHashMap<Role, Int>()
        // for each role take the first column; for dates prefer a transaction/posting date over a value date
        labels.forEachIndexed { c, l ->
            val role = roleOf(l) ?: (if (twoRows) roleOf(cell(g, r + 1, c)) else null) ?: roleOf(cell(g, r, c)) ?: return@forEachIndexed
            val cur = roles[role]
            if (cur == null) roles[role] = c
            else if (role == Role.DATE && norm(labels[cur]).startsWith("value") && !norm(l).startsWith("value")) roles[role] = c
        }
        return Layout(roles, labels)
    }

    private fun moneyCols(l: Layout) = listOfNotNull(l.roles[Role.DEBIT], l.roles[Role.CREDIT], l.roles[Role.AMOUNT])

    private fun isTxnRow(g: List<List<String>>, r: Int, l: Layout): Boolean {
        val d = l.roles[Role.DATE] ?: return false
        if (!looksLikeDate(cell(g, r, d))) return false
        val m = moneyCols(l)
        return m.isEmpty() || m.any { amountOf(cell(g, r, it)) != null }
    }

    private fun scoreLayout(g: List<List<String>>, r: Int, headerEnd: Int, l: Layout): Int {
        val roles = l.roles.keys
        if (Role.DATE !in roles || (Role.DESC !in roles && moneyCols(l).isEmpty())) return -1
        if (moneyCols(l).isEmpty()) return -1
        var hits = 0; var misses = 0
        var rr = headerEnd + 1; var seen = 0
        while (rr < g.size && seen < 40) {
            if (g[rr].any { it.isNotBlank() }) { seen++; if (isTxnRow(g, rr, l)) hits++ else misses++ }
            rr++
        }
        if (hits == 0) return -1
        return roles.size * 10 + hits * 2 - misses
    }

    fun find(grid: List<List<String>>): TableFound? {
        var best: Triple<Int, Layout, Int>? = null  // headerRow, layout, headerEnd
        var bestScore = -1
        for (r in grid.indices) {
            val row = grid[r]
            if (row.count { it.isNotBlank() } < 2) continue
            val single = layoutFor(grid, r, false)
            if (single.roles.size >= 2) {
                val s = scoreLayout(grid, r, r, single); if (s > bestScore) { bestScore = s; best = Triple(r, single, r) }
            }
            // merged / two-row header: the next row must be a text row (no date in it)
            if (r + 1 < grid.size && grid[r + 1].count { it.isNotBlank() } >= 2 && grid[r + 1].none { looksLikeDate(it.trim()) }) {
                val dbl = layoutFor(grid, r, true)
                if (dbl.roles.size >= 2) {
                    val s = scoreLayout(grid, r, r + 1, dbl) - 1; if (s > bestScore) { bestScore = s; best = Triple(r, dbl, r + 1) }
                }
            }
        }
        val b = best ?: return null
        return build(grid, b.first, b.third, b.second, bestScore, strict = true)
    }

    /** Manual fallback: the user says which row is the header. Rows after it are taken as data. */
    fun extractAt(grid: List<List<String>>, headerRow: Int): TableFound? {
        if (headerRow !in grid.indices) return null
        val l = layoutFor(grid, headerRow, false)
        return build(grid, headerRow, headerRow, l, 0, strict = Role.DATE in l.roles)
    }

    private fun build(g: List<List<String>>, headerRow: Int, headerEnd: Int, l: Layout, score: Int, strict: Boolean): TableFound? {
        val cols = l.labels.indices.filter { l.labels[it].isNotBlank() || it in l.roles.values }
        if (cols.isEmpty()) return null
        val notes = mutableListOf<String>()
        val raw = mutableListOf<MutableList<String>>()   // values for `cols`
        val descIdx = l.roles[Role.DESC]?.let { cols.indexOf(it) } ?: -1
        var r = headerEnd + 1
        var stopped = false
        while (r < g.size && !stopped) {
            val row = g[r]
            val nonBlank = row.count { it.isNotBlank() }
            if (nonBlank == 0) { r++; continue }
            val txn = if (strict) isTxnRow(g, r, l) else nonBlank >= 2
            if (txn) {
                raw.add(cols.map { cell(g, r, it) }.toMutableList())
            } else if (strict) {
                val first = row.firstOrNull { it.isNotBlank() }?.trim()?.lowercase().orEmpty()
                val repeatedHeader = l.roles.values.count { roleOf(cell(g, r, it)) != null } >= 2
                val moneyBlank = moneyCols(l).all { cell(g, r, it).isBlank() }
                val dateBlank = cell(g, r, l.roles[Role.DATE]!!).isBlank()
                when {
                    raw.isNotEmpty() && summaryWords.any { first.startsWith(it) } && dateBlank -> { stopped = true; notes.add("Stopped at a summary section after row ${r + 1}.") }
                    repeatedHeader -> {}
                    raw.isNotEmpty() && dateBlank && moneyBlank && descIdx >= 0 -> {
                        val extra = cell(g, r, cols[descIdx]).ifBlank { row.firstOrNull { it.isNotBlank() }?.trim().orEmpty() }
                        if (extra.isNotBlank() && nonBlank <= 2) raw.last()[descIdx] = (raw.last()[descIdx] + " " + extra).trim()
                    }
                    else -> {}
                }
            }
            r++
        }
        if (raw.isEmpty()) return null
        return finish(l, cols, raw, headerRow, score, notes)
    }

    private fun finish(l: Layout, cols: List<Int>, raw: MutableList<MutableList<String>>, headerRow: Int, score: Int, notes: MutableList<String>): TableFound {
        fun idx(role: Role) = l.roles[role]?.let { cols.indexOf(it) } ?: -1
        val names = cols.map { c ->
            when (l.roles.entries.firstOrNull { it.value == c }?.key) {
                Role.DATE -> "Date"; Role.DESC -> "Description"; Role.DEBIT -> "Debit"; Role.CREDIT -> "Credit"
                Role.AMOUNT -> "Amount"; Role.BALANCE -> "Balance"; Role.REF -> "Reference"; Role.TYPE -> "Type"
                null -> l.labels[c].ifBlank { "Column ${c + 1}" }
            }
        }.toMutableList()
        val rows = raw.map { it.toMutableList() }.toMutableList()
        val iDebit = idx(Role.DEBIT); val iCredit = idx(Role.CREDIT); val iAmt = idx(Role.AMOUNT); val iType = idx(Role.TYPE)
        // Single amount column -> split into separate Debit / Credit when direction is explicit
        if (iDebit < 0 && iCredit < 0 && iAmt >= 0) {
            var explicit = 0; var ambiguous = 0
            val dir = rows.map { row ->
                val a = row[iAmt]
                val d = drCrOf(a.takeIf { it.any { ch -> ch.isLetter() } } ?: "") ?: (if (iType >= 0) drCrOf(row[iType]) else null)
                val num = amountOf(a)
                when { d != null && num != null -> { explicit++; d }; num != null && num < 0 -> { explicit++; "D" }; else -> { ambiguous++; null } }
            }
            if (explicit > 0 && explicit >= (explicit + ambiguous) * 0.9) {
                names.add("Debit"); names.add("Credit")
                rows.forEachIndexed { i, row ->
                    val v = amountOf(row[iAmt])?.let { kotlin.math.abs(it) }?.let { cleanNum(it) } ?: ""
                    row.add(if (dir[i] == "D") v else ""); row.add(if (dir[i] == "C") v else "")
                }
                names[iAmt] = "Amount (original)"
                notes.add("One amount column: direction taken from the sign or the Dr/Cr marker.")
                if (ambiguous > 0) notes.add("$ambiguous rows had no clear Dr/Cr, so they are left for you to review.")
            } else if (explicit == 0) notes.add("One amount column without Dr/Cr or signs: direction cannot be inferred, please check the preview.")
        }
        var opening: Double? = null
        // Balance reconciliation, a sanity check only
        val iBal = idx(Role.BALANCE); val nd = names.indexOf("Debit"); val nc = names.indexOf("Credit")
        if (iBal >= 0 && nd >= 0 && nc >= 0) {
            fun reconciles(order: List<MutableList<String>>): Pair<Int, Int> {
                var ok = 0; var tot = 0
                for (k in 1 until order.size) {
                    val pb = amountOf(order[k - 1][iBal]); val b = amountOf(order[k][iBal]) ?: continue
                    if (pb == null) continue
                    val dr = amountOf(order[k][nd]) ?: 0.0; val cr = amountOf(order[k][nc]) ?: 0.0
                    tot++; if (kotlin.math.abs(pb - dr + cr - b) <= 0.02) ok++
                }
                return ok to tot
            }
            val fwd = reconciles(rows); val rev = reconciles(rows.reversed())
            val best = if (rev.first > fwd.first) rev else fwd
            if (best.second >= 2 && best.first * 10 >= best.second * 9) {
                val chrono = if (rev.first > fwd.first) rows.reversed() else rows
                val f0 = chrono.firstOrNull(); val b0 = f0?.let { amountOf(it[iBal]) }
                if (f0 != null && b0 != null) opening = Math.round((b0 + (amountOf(f0[nd]) ?: 0.0) - (amountOf(f0[nc]) ?: 0.0)) * 100.0) / 100.0
            }
            if (best.second >= 2) notes.add(if (best.first * 10 >= best.second * 9) "Running balance matches on ${best.first} of ${best.second} rows." else "Running balance only matches on ${best.first} of ${best.second} rows. Check the debit/credit columns.")
        }
        return TableFound(headerRow, cols.first(), names, rows, notes, score, opening)
    }

    private fun cleanNum(v: Double): String = if (v % 1.0 == 0.0 && v < 1e15) v.toLong().toString() else java.math.BigDecimal(v).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()

    fun toCsv(t: TableFound): String {
        fun q(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        return (listOf(t.headers) + t.rows).joinToString("\n") { row -> row.joinToString(",") { q(it.replace("\n", " ")) } }
    }
}
