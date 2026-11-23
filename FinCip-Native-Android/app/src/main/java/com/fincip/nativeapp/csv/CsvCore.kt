package com.fincip.nativeapp.csv

import java.nio.charset.Charset

/** Plain-Kotlin CSV reader/writer (no Android classes) so it can be unit-tested on the JVM. */
object Csv {
    /** Decodes bytes: honours a BOM, tries strict UTF-8, then falls back to Windows-1252 (common in bank exports). */
    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        val strict = Charsets.UTF_8.newDecoder()
        return try { strict.decode(java.nio.ByteBuffer.wrap(bytes)).toString() } catch (_: Exception) { String(bytes, Charset.forName("windows-1252")) }
    }

    /** Picks the delimiter that gives the most consistent column count over the first lines. */
    fun detectDelimiter(text: String): Char {
        val sample = text.lineSequence().filter { it.isNotBlank() }.take(40).toList()
        if (sample.isEmpty()) return ','
        var best = ','; var bestScore = -1.0
        for (d in charArrayOf(',', ';', '\t', '|')) {
            val counts = sample.map { countOutsideQuotes(it, d) }
            val common = counts.groupBy { it }.maxByOrNull { it.value.size } ?: continue
            if (common.key == 0) continue
            val score = common.value.size.toDouble() / counts.size * 10 + common.key.coerceAtMost(30) * 0.01
            if (score > bestScore) { bestScore = score; best = d }
        }
        return best
    }

    private fun countOutsideQuotes(line: String, d: Char): Int { var q = false; var n = 0; for (c in line) { if (c == '"') q = !q else if (c == d && !q) n++ }; return n }

    /** RFC-4180 parser: quoted fields, doubled quotes, embedded newlines, \r\n or \n. Blank lines are dropped. */
    fun parse(text: String, delimiter: Char = detectDelimiter(text)): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>(); val cell = StringBuilder()
        var inQuotes = false; var i = 0; val n = text.length
        fun endCell() { row.add(cell.toString()); cell.setLength(0) }
        fun endRow() { endCell(); if (row.size > 1 || row[0].isNotBlank()) rows.add(row); row = ArrayList() }
        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') { if (i + 1 < n && text[i + 1] == '"') { cell.append('"'); i++ } else inQuotes = false } else cell.append(c)
            } else when {
                c == '"' && cell.isBlank() -> { cell.setLength(0); inQuotes = true }
                c == delimiter -> endCell()
                c == '\r' -> { if (i + 1 < n && text[i + 1] == '\n') i++; endRow() }
                c == '\n' -> endRow()
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    /** Quotes every field that needs it. A leading = + - @ is prefixed with a tab-less apostrophe-free guard only for text columns (see [guard]). */
    fun write(rows: List<List<String>>, delimiter: Char = ','): String {
        val sb = StringBuilder()
        for (r in rows) { sb.append(r.joinToString(delimiter.toString()) { quote(it, delimiter) }); sb.append("\r\n") }
        return sb.toString()
    }

    fun quote(v: String, delimiter: Char = ','): String =
        if (v.any { it == '"' || it == delimiter || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    /** Spreadsheet formula-injection guard for free text (titles/notes), not for numbers. */
    fun guard(v: String): String = if (v.isNotEmpty() && v[0] in "=@\t\r" ) "'$v" else v
}
