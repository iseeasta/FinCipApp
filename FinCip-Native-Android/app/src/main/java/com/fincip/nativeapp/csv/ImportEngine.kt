package com.fincip.nativeapp.csv

import java.time.LocalDate
import java.util.Locale

enum class Role { DATE, TIME, DESC, AMOUNT, DEBIT, CREDIT, TYPE, CATEGORY, ACCOUNT, BALANCE, CURRENCY, REF, NOTE, STATUS, TAGS, IGNORE }
enum class RowState { NEW, DUPLICATE, SKIPPED, ERROR }
enum class DateOrder { DAY_FIRST, MONTH_FIRST, YEAR_FIRST }

data class ImportRow(
    val rowNo: Int, val date: String, val amount: Double, val type: String, val title: String, val category: String,
    val account: String, val currency: String, val ref: String, val note: String, val tags: String,
    val state: RowState, val issue: String = "", val classifiedBy: String = ""
)

data class ImportPreview(
    val delimiter: Char, val headerRow: Int, val headers: List<String>, val roles: List<Role>,
    val dateOrder: DateOrder, val dateOrderGuessed: Boolean, val rows: List<ImportRow>, val warnings: List<String>, val totalDataRows: Int
) {
    val fresh get() = rows.filter { it.state == RowState.NEW }
    val duplicates get() = rows.count { it.state == RowState.DUPLICATE }
    val skipped get() = rows.count { it.state == RowState.SKIPPED }
    val errors get() = rows.count { it.state == RowState.ERROR }
    val minDate get() = fresh.minOfOrNull { it.date }
    val maxDate get() = fresh.maxOfOrNull { it.date }
}

/** What the engine needs to know about data already in the app, to detect duplicates. */
class ExistingIndex(entries: Iterable<Triple<String, String, String>>, refs: Iterable<String> = emptyList()) {
    // fingerprint -> remaining copies (multiset), so repeated identical legit entries are not all lost
    val counts = HashMap<String, Int>().also { m -> entries.forEach { m.merge(fingerprint(it.first, it.second, it.third), 1, Int::plus) } }
    val refSet = refs.filter { it.isNotBlank() }.toHashSet()
    companion object {
        fun fingerprint(date: String, amountAndType: String, title: String) = "$date|$amountAndType|${normalizeTitle(title)}"
        /** Key shared by the importer and the app's stored transactions (amount at 2 dp plus direction). */
        fun keyFor(date: String, amount: Double, type: String, title: String): Triple<String, String, String> =
            Triple(date, "%.2f%s".format(Locale.US, amount, if (type == "income" || type == "refund") "+" else if (type == "transfer") "~" else "-"), title)
        fun normalizeTitle(t: String) = t.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim()
    }
}

object ImportEngine {
    private val synonyms: Map<Role, List<String>> = mapOf(
        Role.DATE to listOf("date", "txn date", "transaction date", "tran date", "value date", "value dt", "posting date", "booking date", "posted date", "trans date", "when", "day", "datetime", "date time", "completed date", "started date"),
        Role.TIME to listOf("time", "txn time", "transaction time"),
        Role.DESC to listOf("description", "narration", "particulars", "details", "transaction details", "transaction remarks", "remarks", "memo", "payee", "merchant", "title", "name", "paid to", "to/from", "transaction description", "reference", "note text", "counterparty", "item", "what", "spent on"),
        Role.AMOUNT to listOf("amount", "amount (inr)", "transaction amount", "amt", "value", "sum", "total", "amount inr", "rs", "inr", "how much"),
        Role.DEBIT to listOf("debit", "withdrawal", "withdrawal amt", "withdrawal amount", "withdrawals", "dr", "debit amount", "paid out", "money out", "outflow", "expense amount", "withdrawal amt."),
        Role.CREDIT to listOf("credit", "deposit", "deposit amt", "deposit amount", "deposits", "cr", "credit amount", "paid in", "money in", "inflow", "income amount", "deposit amt."),
        Role.TYPE to listOf("type", "transaction type", "dr/cr", "cr/dr", "debit/credit", "income/expense", "txn type", "direction", "nature"),
        Role.CATEGORY to listOf("category", "tag", "label", "group", "purpose", "category name"),
        Role.ACCOUNT to listOf("account", "bank", "wallet", "payment method", "payment mode", "mode", "your account", "payment type", "source", "paid via", "instrument"),
        Role.BALANCE to listOf("balance", "closing balance", "running balance", "bal", "available balance"),
        Role.CURRENCY to listOf("currency", "ccy", "currency code"),
        Role.REF to listOf("ref no", "ref no.", "reference no", "reference number", "transaction id", "txn id", "utr", "upi ref no", "upi ref no.", "cheque no", "chq./ref.no.", "chq / ref no", "order id", "id", "chq no", "chqno", "ref", "utr no", "upi transaction id", "transaction reference", "reference id", "ref id", "txn ref"),
        Role.NOTE to listOf("note", "notes", "comment", "comments", "other transaction details"),
        Role.STATUS to listOf("status", "transaction status"),
        Role.TAGS to listOf("tags", "labels")
    )

    /** Best-effort guess of where the file came from, for the preview header. */
    fun guessSource(headers: List<String>, text: String): String {
        val h = headers.joinToString("|") { norm(it) }; val head = text.take(2000).lowercase(Locale.ROOT)
        return when {
            head.contains("phonepe") -> "PhonePe"
            head.contains("google pay") || head.contains("gpay") -> "Google Pay"
            head.contains("paytm") || h.contains("upi ref no") && h.contains("your account") -> "Paytm"
            head.contains("bhim") -> "BHIM UPI"
            head.contains("hdfc") || h.contains("chq./ref.no.") && h.contains("narration") -> "HDFC Bank"
            head.contains("icici") || h.contains("transaction remarks") -> "ICICI Bank"
            head.contains("axis bank") || h.contains("particulars") && h.contains("chqno") -> "Axis Bank"
            head.contains("state bank") || head.contains("sbi") -> "State Bank of India"
            head.contains("kotak") -> "Kotak Bank"
            head.contains("idfc") -> "IDFC FIRST Bank"
            h.contains("withdrawal") || h.contains("deposit") || h.contains("balance") -> "Bank statement"
            h.contains("fincip_id") || h.contains("fincip id") -> "FinCip export"
            else -> "Not identified"
        }
    }

    private fun norm(h: String) = h.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9/ ().]+"), " ").replace(Regex("\\s+"), " ").trim()

    private fun roleFor(header: String): Role? {
        val h = norm(header); if (h.isBlank()) return null
        val cleaned = h.replace(Regex("\\((inr|rs|usd|eur)[ .]*\\)"), "").replace(Regex("\\s+"), " ").trim().trimEnd('.')
        // exact first, then prefix/contains in a sensible priority order
        for ((role, list) in synonyms) if (list.any { it.trimEnd('.') == cleaned }) return role
        val order = listOf(Role.DEBIT, Role.CREDIT, Role.BALANCE, Role.REF, Role.DATE, Role.TIME, Role.STATUS, Role.TYPE, Role.CATEGORY, Role.DESC, Role.AMOUNT, Role.ACCOUNT, Role.CURRENCY, Role.NOTE)
        for (role in order) if (synonyms[role]!!.any { s -> s.length > 3 && cleaned.contains(s) }) return role
        return null
    }

    /** Statements often have bank-name/address lines above the table, so scan for the row that looks most like a header. */
    fun findHeaderRow(rows: List<List<String>>): Int {
        var best = -1; var bestScore = 1
        for (i in 0 until minOf(rows.size, 40)) {
            val roles = rows[i].mapNotNull { roleFor(it) }.toSet()
            var score = roles.size
            if (Role.DATE !in roles) score -= 2
            if (Role.AMOUNT !in roles && Role.DEBIT !in roles && Role.CREDIT !in roles) score -= 2
            if (score > bestScore) { bestScore = score; best = i }
        }
        return best
    }

    fun parse(
        text: String, existing: ExistingIndex = ExistingIndex(emptyList()), defaultCurrency: String = "INR",
        forcedRoles: List<Role>? = null, forcedDateOrder: DateOrder? = null, onProgress: ((Int, Int) -> Unit)? = null
    ): ImportPreview {
        val delimiter = Csv.detectDelimiter(text)
        val all = Csv.parse(text, delimiter)
        val warnings = ArrayList<String>()
        if (all.isEmpty()) return ImportPreview(delimiter, -1, emptyList(), emptyList(), DateOrder.DAY_FIRST, false, emptyList(), listOf("The file is empty."), 0)

        var headerIdx = findHeaderRow(all)
        var headers: List<String>
        var data: List<List<String>>
        var roles: MutableList<Role>
        if (headerIdx >= 0) {
            headers = all[headerIdx]; data = all.subList(headerIdx + 1, all.size)
            roles = MutableList(headers.size) { roleFor(headers[it]) ?: Role.IGNORE }
        } else {
            // headerless file: invent generic headers and sniff the columns from content
            headers = List(all.maxOf { it.size }) { "Column ${it + 1}" }; data = all
            roles = sniffRoles(data, headers.size)
            warnings.add("No header row found — columns were guessed from their content. Check the preview.")
        }
        // two columns with the same role (e.g. "Value Date" and "Txn Date"): keep the first, ignore later ones except where a pair is meaningful
        val seen = HashSet<Role>()
        for (i in roles.indices) { val r = roles[i]; if (r != Role.IGNORE && !seen.add(r)) roles[i] = Role.IGNORE }
        if (forcedRoles != null) { roles = MutableList(headers.size) { forcedRoles.getOrElse(it) { Role.IGNORE } } }
        if (Role.DATE !in roles) sniffDateColumn(data, roles)?.let { roles[it] = Role.DATE; warnings.add("Date column guessed from its content: ${headers.getOrElse(it) { "#${it + 1}" }}.") }
        if (Role.AMOUNT !in roles && Role.DEBIT !in roles && Role.CREDIT !in roles) sniffAmountColumn(data, roles)?.let { roles[it] = Role.AMOUNT; warnings.add("Amount column guessed from its content.") }
        if (Role.DESC !in roles) sniffDescColumn(data, roles)?.let { roles[it] = Role.DESC }

        val dateCol = roles.indexOf(Role.DATE)
        if (dateCol < 0) return ImportPreview(delimiter, headerIdx, headers, roles, DateOrder.DAY_FIRST, false, emptyList(), warnings + "No date column could be found, so nothing was imported.", data.size)
        if (Role.AMOUNT !in roles && Role.DEBIT !in roles && Role.CREDIT !in roles) return ImportPreview(delimiter, headerIdx, headers, roles, DateOrder.DAY_FIRST, false, emptyList(), warnings + "No amount column could be found, so nothing was imported.", data.size)

        val (order, guessed) = if (forcedDateOrder != null) forcedDateOrder to false else detectDateOrder(data.asSequence().map { it.getOrNull(dateCol).orEmpty() })
        if (guessed) warnings.add("Dates like 03/04/2024 are ambiguous; assumed day/month/year (Indian convention). Change it in the preview if wrong.")

        val col = { r: Role -> roles.indexOf(r) }
        val iTime = col(Role.TIME); val iDesc = col(Role.DESC); val iAmt = col(Role.AMOUNT); val iDeb = col(Role.DEBIT); val iCre = col(Role.CREDIT)
        val iType = col(Role.TYPE); val iCat = col(Role.CATEGORY); val iAcc = col(Role.ACCOUNT); val iCur = col(Role.CURRENCY); val iRef = col(Role.REF)
        val iNote = col(Role.NOTE); val iStat = col(Role.STATUS); val iTags = col(Role.TAGS)

        // If there is no type/debit/credit signal and every amount is positive, the sign carries no information.
        val amounts = if (iAmt >= 0) data.mapNotNull { parseAmount(it.getOrNull(iAmt).orEmpty())?.first } else emptyList()
        val signless = iDeb < 0 && iCre < 0 && iType < 0 && amounts.isNotEmpty() && amounts.none { it < 0 }
        if (signless) warnings.add("Amounts have no sign, debit/credit or type column — every row was treated as an expense. Edit types in the preview if some are income.")

        val remaining = HashMap(existing.counts)
        val seenRefs = HashSet<String>()
        val out = ArrayList<ImportRow>(data.size)
        val seenInFile = HashMap<String, Int>()
        data.forEachIndexed { idx, r ->
            if (onProgress != null && idx % 2000 == 0) onProgress(idx, data.size)
            val rowNo = headerIdx + 2 + idx
            fun cell(i: Int) = if (i >= 0) r.getOrNull(i).orEmpty().trim() else ""
            if (r.all { it.isBlank() }) return@forEachIndexed
            val rawDate = cell(dateCol)
            if (rawDate.isBlank() && cell(iDesc).isBlank() && cell(iAmt).isBlank()) return@forEachIndexed
            val status = cell(iStat).lowercase(Locale.ROOT)
            val title0 = cell(iDesc).ifBlank { cell(iNote) }.ifBlank { "Imported transaction" }
            val date = parseDate(rawDate, order)
            // amount and direction
            var amount = 0.0; var type = ""; var classifiedBy = ""
            if (iDeb >= 0 || iCre >= 0) {
                val d = parseAmount(cell(iDeb))?.first ?: 0.0; val c = parseAmount(cell(iCre))?.first ?: 0.0
                if (d != 0.0 && c == 0.0) { amount = kotlin.math.abs(d); type = "expense"; classifiedBy = "debit column" }
                else if (c != 0.0 && d == 0.0) { amount = kotlin.math.abs(c); type = "income"; classifiedBy = "credit column" }
                else if (d != 0.0 && c != 0.0) { amount = kotlin.math.abs(c - d); type = if (c > d) "income" else "expense"; classifiedBy = "net of debit and credit" }
            }
            if (amount == 0.0 && iAmt >= 0) {
                val pa = parseAmount(cell(iAmt))
                if (pa != null) {
                    amount = kotlin.math.abs(pa.first)
                    val explicit = if (iType >= 0) typeFromWord(cell(iType)) else null
                    val drcr = pa.second
                    type = when {
                        explicit != null -> { classifiedBy = "type column"; explicit }
                        drcr != null -> { classifiedBy = "Dr/Cr marker"; drcr }
                        pa.first < 0 -> { classifiedBy = "negative sign"; "expense" }
                        signless -> { classifiedBy = "assumed"; "expense" }
                        else -> { classifiedBy = "positive sign"; "income" }
                    }
                }
            }
            val issues = ArrayList<String>()
            if (date == null) issues.add("Unreadable date “$rawDate”")
            if (amount == 0.0 && type.isEmpty()) issues.add("No readable amount")
            if (status.isNotEmpty() && (status.contains("fail") || status.contains("declin") || status.contains("cancel") || status.contains("reject") || status.contains("pending"))) {
                out.add(ImportRow(rowNo, date?.toString() ?: "", amount, type.ifEmpty { "expense" }, title0, "Other", cell(iAcc), cell(iCur).ifBlank { defaultCurrency }, cell(iRef), cell(iNote), cell(iTags), RowState.SKIPPED, "Status: ${cell(iStat)}", "status"))
                return@forEachIndexed
            }
            if (issues.isNotEmpty() || date == null) {
                out.add(ImportRow(rowNo, date?.toString() ?: "", amount, type.ifEmpty { "expense" }, title0, "Other", cell(iAcc), cell(iCur).ifBlank { defaultCurrency }, cell(iRef), cell(iNote), cell(iTags), RowState.ERROR, issues.joinToString("; ")))
                return@forEachIndexed
            }
            // semantic classification from the description (transfer / refund / salary ...), only overriding when strongly indicated
            val kind = Classifier.kind(title0)
            if (kind == "transfer") { type = "transfer"; classifiedBy = "description: own-account transfer" }
            else if (kind == "refund" && type == "income") { type = "refund"; classifiedBy = "description: refund" }
            val catFromFile = cell(iCat)
            val category = catFromFile.ifBlank { Classifier.category(title0, type) }
            val cur = cell(iCur).ifBlank { defaultCurrency }.uppercase(Locale.ROOT).take(3).ifBlank { defaultCurrency }
            val ref = cell(iRef)
            val iso = date.toString()
            val key = ExistingIndex.keyFor(iso, amount, type.ifEmpty { "expense" }, title0); val fp = ExistingIndex.fingerprint(key.first, key.second, key.third)
            var state = RowState.NEW; var issue = ""
            if (ref.isNotBlank() && (ref in existing.refSet)) { state = RowState.DUPLICATE; issue = "Same reference number already in FinCip" }
            else if (ref.isNotBlank() && !seenRefs.add(ref + "|" + iso)) { state = RowState.DUPLICATE; issue = "Repeated reference number inside the file" }
            else {
                val left = remaining[fp] ?: 0
                if (left > 0) { remaining[fp] = left - 1; state = RowState.DUPLICATE; issue = "Matching transaction already in FinCip" }
                else { val n = (seenInFile[fp] ?: 0) + 1; seenInFile[fp] = n; if (n > 1) issue = "Looks identical to another row in this file (kept — could be a repeat purchase)" }
            }
            if (amount == 0.0 && state == RowState.NEW) { issue = "Zero amount" }
            out.add(ImportRow(rowNo, iso, amount, type.ifEmpty { "expense" }, Csv.guardIn(title0), category, cell(iAcc), cur, ref, cell(iNote), cell(iTags), state, issue, classifiedBy))
        }
        onProgress?.invoke(data.size, data.size)
        return ImportPreview(delimiter, headerIdx, headers, roles, order, guessed, out, warnings, data.size)
    }

    // ---------- content sniffing ----------
    private fun sniffRoles(data: List<List<String>>, n: Int): MutableList<Role> {
        val roles = MutableList(n) { Role.IGNORE }
        sniffDateColumn(data, roles)?.let { roles[it] = Role.DATE }
        sniffAmountColumn(data, roles)?.let { roles[it] = Role.AMOUNT }
        sniffDescColumn(data, roles)?.let { roles[it] = Role.DESC }
        return roles
    }
    private fun sample(data: List<List<String>>, c: Int) = data.asSequence().take(200).map { it.getOrNull(c).orEmpty().trim() }.filter { it.isNotEmpty() }.toList()
    private fun sniffDateColumn(data: List<List<String>>, roles: List<Role>): Int? {
        val n = data.maxOfOrNull { it.size } ?: return null
        return (0 until n).filter { roles.getOrNull(it) == Role.IGNORE || it >= roles.size }.maxByOrNull { c -> val s = sample(data, c); if (s.size < 2) 0.0 else s.count { parseDate(it, DateOrder.DAY_FIRST) != null }.toDouble() / s.size }
            ?.takeIf { c -> val s = sample(data, c); s.size >= 2 && s.count { parseDate(it, DateOrder.DAY_FIRST) != null }.toDouble() / s.size > .8 }
    }
    private fun sniffAmountColumn(data: List<List<String>>, roles: List<Role>): Int? {
        val n = data.maxOfOrNull { it.size } ?: return null
        return (0 until n).filter { roles.getOrNull(it) == Role.IGNORE || it >= roles.size }.maxByOrNull { c -> val s = sample(data, c); if (s.size < 2) 0.0 else s.count { parseAmount(it) != null && parseDate(it, DateOrder.DAY_FIRST) == null }.toDouble() / s.size }
            ?.takeIf { c -> val s = sample(data, c); s.size >= 2 && s.count { parseAmount(it) != null && parseDate(it, DateOrder.DAY_FIRST) == null }.toDouble() / s.size > .8 }
    }
    private fun sniffDescColumn(data: List<List<String>>, roles: List<Role>): Int? {
        val n = data.maxOfOrNull { it.size } ?: return null
        return (0 until n).filter { roles.getOrNull(it) == Role.IGNORE || it >= roles.size }.maxByOrNull { c -> sample(data, c).filter { s -> s.any { it.isLetter() } }.sumOf { it.length } }
    }

    // ---------- dates ----------
    private val monthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val numeric = Regex("^(\\d{1,4})[./\\-](\\d{1,2})[./\\-](\\d{1,4})(?:[ T,]+.*)?$")
    private val named1 = Regex("^(\\d{1,2})(?:st|nd|rd|th)?[ \\-/,.]+([A-Za-z]{3,9})\\.?[ \\-/,.]+(\\d{2,4})(?:[ T,]+.*)?$")
    private val named2 = Regex("^([A-Za-z]{3,9})\\.?[ \\-/,.]+(\\d{1,2})(?:st|nd|rd|th)?[ \\-/,.]+(\\d{2,4})(?:[ T,]+.*)?$")
    private val compact = Regex("^(\\d{4})(\\d{2})(\\d{2})(?:\\D.*)?$")

    fun detectDateOrder(values: Sequence<String>): Pair<DateOrder, Boolean> {
        var dayFirst = 0; var monthFirst = 0; var yearFirst = 0
        for (v in values.take(5000)) {
            val m = numeric.matchEntire(v.trim()) ?: continue
            val a = m.groupValues[1]; val b = m.groupValues[2].toInt(); val c = m.groupValues[3]
            if (a.length == 4) { yearFirst++; continue }
            val ai = a.toInt()
            if (ai > 12 && b <= 12) dayFirst++ else if (b > 12 && ai <= 12) monthFirst++
        }
        return when {
            yearFirst > 0 && dayFirst == 0 && monthFirst == 0 -> DateOrder.YEAR_FIRST to false
            dayFirst > 0 && monthFirst == 0 -> DateOrder.DAY_FIRST to false
            monthFirst > 0 && dayFirst == 0 -> DateOrder.MONTH_FIRST to false
            dayFirst > 0 && monthFirst > 0 -> DateOrder.DAY_FIRST to true
            else -> DateOrder.DAY_FIRST to true
        }
    }

    fun parseDate(raw: String, order: DateOrder): LocalDate? {
        val s = raw.trim().trim('"'); if (s.isEmpty()) return null
        return try {
            compact.matchEntire(s)?.let { m -> return mk(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
            numeric.matchEntire(s)?.let { m ->
                val a = m.groupValues[1]; val b = m.groupValues[2].toInt(); val c = m.groupValues[3]
                return if (a.length == 4) mk(a.toInt(), b, c.toInt())
                else {
                    val ai = a.toInt(); val ci = fullYear(c.toInt(), c.length)
                    when {
                        c.length == 4 || c.length == 2 -> when {
                            order == DateOrder.MONTH_FIRST && ai <= 12 -> mk(ci, ai, b)
                            ai > 12 -> mk(ci, b, ai)
                            b > 12 -> mk(ci, ai, b)
                            else -> mk(ci, b, ai)
                        }
                        else -> null
                    }
                }
            }
            named1.matchEntire(s)?.let { m -> val mo = monthIdx(m.groupValues[2]) ?: return null; return mk(fullYear(m.groupValues[3].toInt(), m.groupValues[3].length), mo, m.groupValues[1].toInt()) }
            named2.matchEntire(s)?.let { m -> val mo = monthIdx(m.groupValues[1]) ?: return null; return mk(fullYear(m.groupValues[3].toInt(), m.groupValues[3].length), mo, m.groupValues[2].toInt()) }
            null
        } catch (_: Exception) { null }
    }
    private fun mk(y: Int, m: Int, d: Int): LocalDate? = try { if (y < 1900 || y > 2100) null else LocalDate.of(y, m, d) } catch (_: Exception) { null }
    private fun fullYear(y: Int, len: Int) = if (len == 2) (if (y >= 70) 1900 + y else 2000 + y) else y
    private fun monthIdx(name: String): Int? { val i = monthNames.indexOf(name.lowercase(Locale.ROOT).take(3)); return if (i < 0) null else i + 1 }

    // ---------- amounts ----------
    private val DR_END = Regex("(^|[\\s\\d.)])(dr|debit)\\.?$"); private val DR_START = Regex("^(dr|debit)[\\s.:]")
    private val CR_END = Regex("(^|[\\s\\d.)])(cr|credit)\\.?$"); private val CR_START = Regex("^(cr|credit)[\\s.:]")
    private val WORDS = Regex("(?i)\\b(rs\\.?|inr|usd|eur|gbp|cr|dr|debit|credit)\\b"); private val SYMS = Regex("[₹$€£¥\\s\\u00A0]")
    /** Returns (signed value, "income"/"expense" if a Dr/Cr marker was present). */
    fun parseAmount(raw: String): Pair<Double, String?>? {
        var s = raw.trim(); if (s.isEmpty()) return null
        var marker: String? = null; var neg = false
        val low = s.lowercase(Locale.ROOT)
        if (DR_END.containsMatchIn(low) || DR_START.containsMatchIn(low)) marker = "expense"
        else if (CR_END.containsMatchIn(low) || CR_START.containsMatchIn(low)) marker = "income"
        s = s.replace(WORDS, "").replace(SYMS, "")
        if (s.startsWith("(") && s.endsWith(")")) { neg = true; s = s.substring(1, s.length - 1) }
        if (s.endsWith("-")) { neg = true; s = s.dropLast(1) }
        if (s.startsWith("-") || s.startsWith("−")) { neg = true; s = s.substring(1) }
        if (s.startsWith("+")) s = s.substring(1)
        if (s.isEmpty() || !s.all { it.isDigit() || it == '.' || it == ',' }) return null
        val lastDot = s.lastIndexOf('.'); val lastComma = s.lastIndexOf(',')
        s = when {
            lastComma > lastDot && s.length - lastComma - 1 in 1..2 && lastDot < 0 -> s.replace(".", "").replace(',', '.')
            lastComma > lastDot && lastDot >= 0 -> s.replace(".", "").replace(',', '.')
            else -> s.replace(",", "")
        }
        val v = s.toDoubleOrNull() ?: return null
        return (if (neg) -v else v) to marker
    }

    private fun typeFromWord(w: String): String? {
        val t = w.lowercase(Locale.ROOT).trim()
        if (t.isEmpty()) return null
        return when {
            t in setOf("cr", "credit", "income", "deposit", "received", "in", "credited", "inflow", "money in", "salary", "+") || t.startsWith("credit") || t.startsWith("income") || t.startsWith("receiv") -> "income"
            t in setOf("dr", "debit", "expense", "expenses", "withdrawal", "paid", "sent", "out", "debited", "outflow", "money out", "purchase", "payment", "-") || t.startsWith("debit") || t.startsWith("expens") || t.startsWith("withdraw") -> "expense"
            t.startsWith("transfer") -> "transfer"
            t.startsWith("refund") -> "refund"
            else -> null
        }
    }
}

private fun Csv.guardIn(v: String) = v.removePrefix("'").takeIf { v.startsWith("'=") || v.startsWith("'@") } ?: v

/** Keyword classification. Deliberately conservative: unknown stays "Other". */
object Classifier {
    private val transferWords = listOf("self transfer", "own account", "to self", "transfer to savings", "transfer from savings", "fund transfer to own", "between accounts", "own a/c", "wallet top up", "add money to wallet", "added to wallet", "credit card payment", "cc payment", "bill payment credit card", "creditcard payment", "autopay credit card", "neft to own", "internal transfer")
    private val refundWords = listOf("refund", "reversal", "reversed", "chargeback", "cashback reversal", "returned", "money back")
    fun kind(title: String): String? {
        val t = title.lowercase(Locale.ROOT)
        if (transferWords.any { t.contains(it) }) return "transfer"
        if (refundWords.any { t.contains(it) }) return "refund"
        return null
    }
    private val rules: List<Pair<String, List<String>>> = listOf(
        "Food" to listOf("swiggy", "zomato", "restaurant", "cafe", "coffee", "starbucks", "mcdonald", "dominos", "pizza", "kfc", "burger", "bakery", "food", "dining", "eatery", "biryani", "blinkit", "zepto", "bigbasket", "grocery", "instamart", "dmart", "supermarket", "reliance fresh", "milk"),
        "Transport" to listOf("uber", "ola ", "ola cabs", "rapido", "metro", "irctc", "redbus", "fuel", "petrol", "diesel", "hp pay", "indian oil", "bharat petroleum", "fastag", "parking", "toll", "cab", "auto "),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "mall", "store", "shop", "decathlon", "ikea", "croma", "reliance digital", "lifestyle", "zara", "h&m"),
        "Bills" to listOf("electricity", "bescom", "tata power", "adani", "water bill", "gas bill", "airtel", "jio", "vodafone", "vi prepaid", "bsnl", "broadband", "recharge", "dth", "postpaid", "mseb", "bill payment", "insurance premium", "lic "),
        "Subscriptions" to listOf("netflix", "spotify", "prime video", "hotstar", "disney", "youtube premium", "apple.com/bill", "google play", "icloud", "subscription", "sonyliv", "zee5", "audible", "chatgpt", "openai", "notion"),
        "Entertainment" to listOf("bookmyshow", "pvr", "inox", "cinema", "movie", "steam", "playstation", "xbox", "game"),
        "Health" to listOf("pharmacy", "apollo", "medplus", "1mg", "netmeds", "hospital", "clinic", "doctor", "diagnostic", "pharmeasy", "dental", "lab "),
        "Education" to listOf("school", "college", "university", "course", "udemy", "coursera", "byju", "unacademy", "tuition", "fees", "exam"),
        "Travel" to listOf("makemytrip", "goibibo", "airline", "indigo", "air india", "vistara", "hotel", "oyo", "airbnb", "booking.com", "cleartrip", "yatra"),
        "Home" to listOf("rent", "maintenance", "society", "furniture", "urban company", "housing", "plumber", "electrician"),
        "EMI" to listOf("emi", "loan", "bajaj finance", "home loan", "car loan", "instalment", "installment"),
        "Investment" to listOf("zerodha", "groww", "upstox", "mutual fund", "sip ", "nippon", "hdfc mf", "coin by", "kuvera", "ppf", "nps", "stock", "demat", "gold bond"),
        "Personal care" to listOf("salon", "spa", "barber", "grooming", "cult.fit", "gym", "urbanclap")
    )
    private val incomeRules: List<Pair<String, List<String>>> = listOf(
        "Salary" to listOf("salary", "payroll", "sal cr", "wages"),
        "Freelance" to listOf("freelance", "consulting", "invoice", "client payment"),
        "Business" to listOf("sales", "business", "merchant settlement"),
        "Investment" to listOf("dividend", "interest", "maturity", "redemption", "int.pd", "int pd"),
        "Gift" to listOf("gift"),
        "Refund" to listOf("refund", "reversal", "cashback")
    )
    fun category(title: String, type: String): String {
        val t = " " + title.lowercase(Locale.ROOT) + " "
        if (type == "refund") return "Refund"
        if (type == "transfer") return "Transfer"
        if (type == "income") { incomeRules.firstOrNull { (_, ws) -> ws.any { t.contains(it) } }?.let { return it.first }; return "Other" }
        return rules.firstOrNull { (_, ws) -> ws.any { t.contains(it) } }?.first ?: "Other"
    }
}
