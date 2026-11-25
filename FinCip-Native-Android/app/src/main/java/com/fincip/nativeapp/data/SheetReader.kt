package com.fincip.nativeapp.data

import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

class SheetData(val name: String, val grid: List<List<String>>)

/** Reads .xlsx (own small SAX parser) and .xls (jxl) into plain string grids. Date cells come out as ISO yyyy-MM-dd. */
object SheetReader {
    const val MAX_ROWS = 200_000

    fun isZip(b: ByteArray) = b.size > 4 && b[0] == 'P'.code.toByte() && b[1] == 'K'.code.toByte()
    fun isOle(b: ByteArray) = b.size > 8 && (b[0].toInt() and 0xFF) == 0xD0 && (b[1].toInt() and 0xFF) == 0xCF && (b[2].toInt() and 0xFF) == 0x11 && (b[3].toInt() and 0xFF) == 0xE0

    fun read(bytes: ByteArray): List<SheetData>? = when {
        isZip(bytes) -> runCatching { readXlsx(bytes) }.getOrNull()
        isOle(bytes) -> runCatching { readXls(bytes) }.getOrNull()
        else -> null
    }

    // ---------------------------------------------------------------- XLSX
    private fun colIndex(ref: String): Int {
        var n = 0
        for (ch in ref) { if (ch in 'A'..'Z') n = n * 26 + (ch - 'A' + 1) else if (ch in 'a'..'z') n = n * 26 + (ch - 'a' + 1) else break }
        return n - 1
    }
    private fun rowIndex(ref: String): Int = ref.dropWhile { it.isLetter() }.toIntOrNull()?.minus(1) ?: -1

    private fun sax(bytes: ByteArray, h: DefaultHandler) {
        val f = SAXParserFactory.newInstance().apply { isNamespaceAware = false }
        runCatching { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        f.newSAXParser().parse(InputSource(ByteArrayInputStream(bytes)), h)
    }

    fun readXlsx(bytes: ByteArray): List<SheetData> {
        val files = HashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            var e = z.nextEntry
            while (e != null) {
                val n = e.name
                if (n.startsWith("xl/") && n.endsWith(".xml") || n.endsWith(".rels")) files[n] = z.readBytes()
                e = z.nextEntry
            }
        }
        val shared = ArrayList<String>()
        files["xl/sharedStrings.xml"]?.let { b ->
            sax(b, object : DefaultHandler() {
                var inSi = false; var inT = false; var skip = 0; val sb = StringBuilder()
                override fun startElement(u: String?, l: String?, q: String, a: Attributes) {
                    when (q) { "si" -> { inSi = true; sb.setLength(0) }; "t" -> inT = true; "rPh" -> skip++ }
                }
                override fun endElement(u: String?, l: String?, q: String) {
                    when (q) { "si" -> { inSi = false; shared.add(sb.toString()) }; "t" -> inT = false; "rPh" -> skip-- }
                }
                override fun characters(ch: CharArray, s: Int, len: Int) { if (inSi && inT && skip == 0) sb.append(ch, s, len) }
            })
        }
        // number formats -> which cell styles are dates
        val customFmt = HashMap<Int, String>(); val xfFmt = ArrayList<Int>()
        files["xl/styles.xml"]?.let { b ->
            sax(b, object : DefaultHandler() {
                var inCellXfs = false
                override fun startElement(u: String?, l: String?, q: String, a: Attributes) {
                    when (q) {
                        "numFmt" -> a.getValue("numFmtId")?.toIntOrNull()?.let { customFmt[it] = a.getValue("formatCode").orEmpty() }
                        "cellXfs" -> inCellXfs = true
                        "xf" -> if (inCellXfs) xfFmt.add(a.getValue("numFmtId")?.toIntOrNull() ?: 0)
                    }
                }
                override fun endElement(u: String?, l: String?, q: String) { if (q == "cellXfs") inCellXfs = false }
            })
        }
        fun isDateStyle(s: Int): Boolean {
            val id = xfFmt.getOrNull(s) ?: return false
            if (id in 14..22 || id in 27..36 || id in 45..47 || id in 50..58) return true
            val code = customFmt[id] ?: return false
            val stripped = code.replace(Regex("\"[^\"]*\""), "").replace(Regex("\\[[^]]*]"), "").replace(Regex("\\\\."), "")
            return Regex("[ymd]", RegexOption.IGNORE_CASE).containsMatchIn(stripped) && !stripped.equals("general", true)
        }
        // sheet names and targets
        val rels = HashMap<String, String>()
        files["xl/_rels/workbook.xml.rels"]?.let { b ->
            sax(b, object : DefaultHandler() {
                override fun startElement(u: String?, l: String?, q: String, a: Attributes) { if (q == "Relationship") rels[a.getValue("Id").orEmpty()] = a.getValue("Target").orEmpty() }
            })
        }
        val sheets = ArrayList<Pair<String, String>>()
        files["xl/workbook.xml"]?.let { b ->
            sax(b, object : DefaultHandler() {
                override fun startElement(u: String?, l: String?, q: String, a: Attributes) {
                    if (q == "sheet") {
                        val rid = a.getValue("r:id") ?: a.getValue("id").orEmpty()
                        val t = rels[rid] ?: return
                        sheets.add(a.getValue("name").orEmpty() to (if (t.startsWith("/")) t.drop(1) else "xl/" + t.removePrefix("./")))
                    }
                }
            })
        }
        return sheets.mapNotNull { (name, path) ->
            val data = files[path] ?: return@mapNotNull null
            val cells = HashMap<Long, String>()
            val merges = ArrayList<IntArray>()
            var maxR = -1; var maxC = -1
            sax(data, object : DefaultHandler() {
                var ref = ""; var type = ""; var style = 0; var inV = false; var inT = false; var inIs = false
                val v = StringBuilder(); val isb = StringBuilder()
                var autoRow = -1
                override fun startElement(u: String?, l: String?, q: String, a: Attributes) {
                    when (q) {
                        "row" -> autoRow = (a.getValue("r")?.toIntOrNull()?.minus(1)) ?: (autoRow + 1)
                        "c" -> { ref = a.getValue("r").orEmpty(); type = a.getValue("t").orEmpty(); style = a.getValue("s")?.toIntOrNull() ?: 0; v.setLength(0); isb.setLength(0) }
                        "v" -> { inV = true; v.setLength(0) }
                        "is" -> inIs = true
                        "t" -> inT = true
                        "mergeCell" -> {
                            val r = a.getValue("ref").orEmpty().split(":")
                            if (r.size == 2) merges.add(intArrayOf(rowIndex(r[0]), colIndex(r[0]), rowIndex(r[1]), colIndex(r[1])))
                        }
                    }
                }
                override fun endElement(u: String?, l: String?, q: String) {
                    when (q) {
                        "v" -> inV = false
                        "t" -> inT = false
                        "is" -> inIs = false
                        "c" -> {
                            val r = rowIndex(ref).takeIf { it >= 0 } ?: autoRow; val c = colIndex(ref)
                            if (r < 0 || c < 0 || r >= MAX_ROWS) return
                            val raw = v.toString()
                            val text = when (type) {
                                "s" -> raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                                "inlineStr" -> isb.toString()
                                "str", "e" -> raw
                                "b" -> if (raw == "1") "TRUE" else "FALSE"
                                else -> if (raw.isBlank()) "" else numberText(raw, isDateStyle(style))
                            }
                            if (text.isNotBlank()) { cells[r.toLong() shl 20 or c.toLong()] = text; if (r > maxR) maxR = r; if (c > maxC) maxC = c }
                        }
                    }
                }
                override fun characters(ch: CharArray, s: Int, len: Int) { if (inV) v.append(ch, s, len) else if (inIs && inT) isb.append(ch, s, len) }
            })
            merges.forEach { m ->
                val src = cells[m[0].toLong() shl 20 or m[1].toLong()] ?: return@forEach
                if ((m[2] - m[0] + 1).toLong() * (m[3] - m[1] + 1) > 5000) return@forEach
                for (r in m[0]..m[2]) for (c in m[1]..m[3]) cells.putIfAbsent(r.toLong() shl 20 or c.toLong(), src)
                if (m[3] > maxC) maxC = m[3]
            }
            val grid = (0..maxR).map { r -> (0..maxC).map { c -> cells[r.toLong() shl 20 or c.toLong()].orEmpty() } }
            SheetData(name, grid)
        }
    }

    private fun numberText(raw: String, date: Boolean): String {
        val d = raw.toDoubleOrNull() ?: return raw
        if (date && d >= 1 && d < 2958466) return LocalDate.of(1899, 12, 30).plusDays(Math.floor(d).toLong()).toString()
        return BigDecimal(d).round(MathContext(15)).stripTrailingZeros().toPlainString()
    }

    // ---------------------------------------------------------------- XLS (BIFF) via jxl
    fun readXls(bytes: ByteArray): List<SheetData> {
        val ws = jxl.WorkbookSettings().apply { setEncoding("Cp1252"); setGCDisabled(true); setSuppressWarnings(true) }
        val wb = jxl.Workbook.getWorkbook(ByteArrayInputStream(bytes), ws)
        try {
            return wb.sheets.map { sh ->
                val rows = minOf(sh.rows, MAX_ROWS); val cols = sh.columns
                val grid = MutableList(rows) { r ->
                    MutableList(cols) { c ->
                        val cell = sh.getCell(c, r)
                        when {
                            cell.type == jxl.CellType.EMPTY -> ""
                            cell is jxl.DateCell -> java.time.Instant.ofEpochMilli(cell.date.time).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                            cell is jxl.NumberCell -> BigDecimal(cell.value).round(MathContext(15)).stripTrailingZeros().toPlainString()
                            else -> cell.contents.orEmpty()
                        }
                    }
                }
                sh.mergedCells.forEach { rg ->
                    val tl = rg.topLeft; val br = rg.bottomRight
                    val src = grid.getOrNull(tl.row)?.getOrNull(tl.column).orEmpty()
                    if (src.isNotBlank() && (br.row - tl.row + 1).toLong() * (br.column - tl.column + 1) <= 5000)
                        for (r in tl.row..minOf(br.row, rows - 1)) for (c in tl.column..minOf(br.column, cols - 1)) if (grid[r][c].isBlank()) grid[r][c] = src
                }
                SheetData(sh.name, grid)
            }
        } finally { wb.close() }
    }

    /** Fallback for ".xls" files that are really an HTML table (several banks export this way). */
    fun readHtmlTable(text: String): SheetData? {
        if (!text.contains("<table", true)) return null
        val rows = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).findAll(text).map { m ->
            Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).findAll(m.groupValues[1])
                .map { it.groupValues[1].replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ").replace("&amp;", "&").replace(Regex("\\s+"), " ").trim() }.toList()
        }.filter { r -> r.any { it.isNotBlank() } }.toList()
        return if (rows.isEmpty()) null else SheetData("HTML table", rows)
    }
}
