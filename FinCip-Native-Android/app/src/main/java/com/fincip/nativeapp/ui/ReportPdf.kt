package com.fincip.nativeapp.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.fincip.nativeapp.csv.ExTxn
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Multi-page financial report. Page 1 = summary + analysis, then a paginated transaction table with a repeating header. */
object ReportPdf {
    private const val W = 595; private const val H = 842; private const val M = 36f
    private const val ROWS = 40
    private val forest = Color.parseColor("#1F5C3A"); private val lime = Color.parseColor("#B9F227"); private val ink = Color.parseColor("#16231B")
    private val mint = Color.parseColor("#DDEFD6"); private val coral = Color.parseColor("#D9706A"); private val grey = Color.parseColor("#6B756E"); private val line = Color.parseColor("#DAD6C8")

    private fun p(size: Float, color: Int = ink, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    private fun fit(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        val n = paint.breakText(text, true, width - paint.measureText("…"), null)
        return text.take(n.coerceAtLeast(0)) + "…"
    }
    private fun num(v: Double) = "%,.2f".format(Locale.US, v)

    fun write(out: OutputStream, txns: List<ExTxn>, periodLabel: String, filterNote: String, base: String, conv: (ExTxn) -> Double) {
        val doc = PdfDocument()
        try {
            val income = txns.filter { it.type == "income" }.sumOf(conv)
            val refunds = txns.filter { it.type == "refund" }.sumOf(conv)
            val spend = txns.filter { it.type != "income" && it.type != "transfer" && it.type != "refund" }
            val expense = spend.sumOf(conv) - refunds
            val maxExp = spend.maxByOrNull(conv); val maxInc = txns.filter { it.type == "income" }.maxByOrNull(conv)
            val tablePages = if (txns.isEmpty()) 0 else (txns.size + ROWS - 1) / ROWS
            val total = 1 + tablePages
            val exported = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

            fun footer(c: Canvas, n: Int) {
                c.drawLine(M, H - 38f, W - M, H - 38f, Paint().apply { color = line; strokeWidth = 1f })
                c.drawText("FinCip · Financial report · exported $exported", M, H - 22f, p(8f, grey))
                val t = "Page $n of $total"; val pp = p(8f, grey); c.drawText(t, W - M - pp.measureText(t), H - 22f, pp)
            }

            // ---- page 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, 1).create()); var c = page.canvas
            c.drawRect(0f, 0f, W.toFloat(), 150f, Paint().apply { color = forest })
            c.drawCircle(W - 40f, -10f, 120f, Paint().apply { color = Color.parseColor("#2D7A4F"); isAntiAlias = true })
            c.drawRoundRect(RectF(M, 36f, M + 30f, 66f), 9f, 9f, Paint().apply { color = lime; isAntiAlias = true })
            c.drawText("FinCip", M + 40f, 58f, p(22f, Color.WHITE, true))
            c.drawText("Financial report", M, 100f, p(28f, Color.WHITE, true))
            c.drawText(periodLabel, M, 122f, p(12f, mint))
            c.drawText("Exported $exported", W - M - p(10f).measureText("Exported $exported"), 58f, p(10f, mint))
            var y = 176f
            // summary tiles
            val tiles = listOf(
                Triple("Total income", "$base ${num(income)}", forest), Triple("Total expenses", "$base ${num(expense)}", coral),
                Triple("Net balance", "$base ${num(income - expense)}", if (income - expense >= 0) forest else coral),
                Triple("Transactions", txns.size.toString(), ink),
                Triple("Largest expense", maxExp?.let { "$base ${num(conv(it))}" } ?: "—", ink), Triple("Largest income", maxInc?.let { "$base ${num(conv(it))}" } ?: "—", ink)
            )
            val tw = (W - 2 * M - 20f) / 3f
            tiles.forEachIndexed { i, (label, value, col) ->
                val x = M + (i % 3) * (tw + 10f); val ty = y + (i / 3) * 64f
                c.drawRoundRect(RectF(x, ty, x + tw, ty + 54f), 10f, 10f, Paint().apply { color = Color.parseColor("#F4F1E6"); isAntiAlias = true })
                c.drawText(label.uppercase(), x + 10f, ty + 18f, p(7.5f, grey, true))
                c.drawText(fit(value, p(14f, col, true), tw - 20f), x + 10f, ty + 40f, p(14f, col, true))
            }
            y += 140f
            if (filterNote.isNotBlank()) { c.drawText(fit("Filters: $filterNote", p(8.5f, grey), W - 2 * M), M, y - 8f, p(8.5f, grey)) }

            // monthly summary + chart (last 12 months present)
            val byMonth = txns.groupBy { YearMonth.from(it.date) }.toSortedMap()
            val months = byMonth.keys.toList().takeLast(12)
            c.drawText("Income vs expense by month", M, y, p(13f, ink, true)); y += 12f
            val chartH = 110f; val chartW = W - 2 * M
            val peak = months.maxOfOrNull { ym -> maxOf(byMonth[ym]!!.filter { it.type == "income" }.sumOf(conv), byMonth[ym]!!.filter { it.type !in setOf("income", "transfer", "refund") }.sumOf(conv)) }?.takeIf { it > 0 } ?: 1.0
            c.drawLine(M, y + chartH, M + chartW, y + chartH, Paint().apply { color = line; strokeWidth = 1f })
            if (months.isNotEmpty()) {
                val slot = chartW / months.size
                months.forEachIndexed { i, ym ->
                    val list = byMonth[ym]!!
                    val inc = list.filter { it.type == "income" }.sumOf(conv); val exp = list.filter { it.type !in setOf("income", "transfer", "refund") }.sumOf(conv) - list.filter { it.type == "refund" }.sumOf(conv)
                    val bw = (slot * .32f).coerceAtMost(18f); val x0 = M + i * slot + slot / 2
                    c.drawRoundRect(RectF(x0 - bw - 1, y + chartH - (inc / peak * (chartH - 8)).toFloat(), x0 - 1, y + chartH), 2f, 2f, Paint().apply { color = forest; isAntiAlias = true })
                    c.drawRoundRect(RectF(x0 + 1, y + chartH - (exp.coerceAtLeast(0.0) / peak * (chartH - 8)).toFloat(), x0 + bw + 1, y + chartH), 2f, 2f, Paint().apply { color = coral; isAntiAlias = true })
                    val lbl = ym.format(DateTimeFormatter.ofPattern(if (months.size > 8) "MMM" else "MMM yy", Locale.ENGLISH)); val lp = p(7f, grey)
                    c.drawText(lbl, x0 - lp.measureText(lbl) / 2, y + chartH + 11f, lp)
                }
            } else c.drawText("No transactions in this selection.", M, y + 30f, p(10f, grey))
            c.drawRect(W - M - 120f, y - 12f, W - M - 112f, y - 4f, Paint().apply { color = forest }); c.drawText("Income", W - M - 108f, y - 4f, p(8f, grey))
            c.drawRect(W - M - 60f, y - 12f, W - M - 52f, y - 4f, Paint().apply { color = coral }); c.drawText("Expense", W - M - 48f, y - 4f, p(8f, grey))
            y += chartH + 34f

            // category breakdown
            c.drawText("Spending by category", M, y, p(13f, ink, true)); y += 14f
            val cats = spend.groupBy { it.category.ifBlank { "Other" } }.mapValues { it.value.sumOf(conv) }.entries.sortedByDescending { it.value }
            val top = cats.take(7); val rest = cats.drop(7).sumOf { it.value }
            val rows = top.map { it.key to it.value } + if (rest > 0) listOf("Other categories" to rest) else emptyList()
            val totalSpend = rows.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
            rows.forEach { (name, v) ->
                if (y > H - 70f) return@forEach
                c.drawText(fit(name, p(9f), 110f), M, y + 9f, p(9f))
                val bx = M + 120f; val bwMax = W - 2 * M - 120f - 120f
                c.drawRoundRect(RectF(bx, y, bx + bwMax, y + 11f), 5f, 5f, Paint().apply { color = Color.parseColor("#EEEADB"); isAntiAlias = true })
                c.drawRoundRect(RectF(bx, y, bx + (bwMax * (v / totalSpend)).toFloat().coerceAtLeast(3f), y + 11f), 5f, 5f, Paint().apply { color = forest; isAntiAlias = true })
                val t = "$base ${num(v)}  ·  ${(v / totalSpend * 100).toInt()}%"; c.drawText(t, W - M - p(8.5f).measureText(t), y + 9f, p(8.5f, grey))
                y += 19f
            }
            footer(c, 1); doc.finishPage(page)

            // ---- table pages
            val cols = floatArrayOf(M, M + 60f, M + 235f, M + 325f, M + 390f, M + 455f) // date, desc, category, income, expense, method
            val widths = floatArrayOf(56f, 170f, 86f, 62f, 62f, W - M - (M + 455f))
            for (pg in 0 until tablePages) {
                page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pg + 2).create()); c = page.canvas
                c.drawText("Transactions", M, 50f, p(16f, forest, true))
                c.drawText("$periodLabel · $base", W - M - p(9f, grey).measureText("$periodLabel · $base"), 50f, p(9f, grey))
                var ty = 68f
                c.drawRect(M - 4f, ty, W - M + 4f, ty + 20f, Paint().apply { color = forest })
                val hp = p(8f, Color.WHITE, true)
                listOf("DATE", "DESCRIPTION", "CATEGORY", "INCOME", "EXPENSE", "METHOD").forEachIndexed { i, t ->
                    val x = if (i in 3..4) cols[i] + widths[i] - hp.measureText(t) - 4f else cols[i]; c.drawText(t, x, ty + 13f, hp)
                }
                ty += 20f
                val slice = txns.subList(pg * ROWS, minOf(txns.size, (pg + 1) * ROWS))
                slice.forEachIndexed { r, t ->
                    if (r % 2 == 1) c.drawRect(M - 4f, ty, W - M + 4f, ty + 18f, Paint().apply { color = Color.parseColor("#F7F4EA") })
                    val bp = p(8.5f)
                    c.drawText(t.date.toString(), cols[0], ty + 12f, bp)
                    c.drawText(fit(t.title, bp, widths[1] - 6f), cols[1], ty + 12f, bp)
                    c.drawText(fit(if (t.type == "transfer") "Transfer" else t.category, p(8.5f, grey), widths[2] - 6f), cols[2], ty + 12f, p(8.5f, grey))
                    val amt = num(t.amount) + if (t.currency != base) " ${t.currency}" else ""
                    val inflow = t.type == "income" || t.type == "refund"
                    val txt = fit(amt, p(8.5f, if (inflow) forest else coral, true), widths[3] - 4f)
                    val ap = p(8.5f, if (inflow) forest else coral, true)
                    if (t.type == "transfer") { val tp = p(8.5f, grey); c.drawText("↔ " + num(t.amount), cols[4] + widths[4] - tp.measureText("↔ " + num(t.amount)) - 4f, ty + 12f, tp) }
                    else if (inflow) c.drawText(txt, cols[3] + widths[3] - ap.measureText(txt) - 4f, ty + 12f, ap)
                    else c.drawText(txt, cols[4] + widths[4] - ap.measureText(txt) - 4f, ty + 12f, ap)
                    c.drawText(fit(t.account, p(8f, grey), widths[5]), cols[5], ty + 12f, p(8f, grey))
                    ty += 18f
                }
                footer(c, pg + 2); doc.finishPage(page)
            }
            if (tablePages == 0) { /* page 1 already says there is nothing to list */ }
            doc.writeTo(out)
        } finally { doc.close() }
    }
}
