package com.fincip.nativeapp.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Turns raw bank narrations into something readable: "UPI-HIMANSHU SHARMA-******@YBL-..." becomes name "Himanshu Sharma", method "UPI". */
object TxText {
    class Clean(val name: String, val method: String)

    private val methods = listOf("UPI", "NEFT", "IMPS", "RTGS", "ATM", "POS", "ACH", "NACH", "ECS", "CHQ", "CHEQUE", "EMI")

    private fun titleCase(s: String): String = s.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }
        .joinToString(" ") { w -> w.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }

    private fun looksLikeId(p: String): Boolean = p.count { it.isDigit() } >= 5 || p.contains('@') || p.contains('*') || p.length > 28

    fun clean(raw: String): Clean {
        val text = raw.trim()
        if (text.isEmpty()) return Clean("Transaction", "")
        val parts = text.split('-').map { it.trim() }.filter { it.isNotEmpty() }
        val first = parts.firstOrNull()?.uppercase(Locale.ROOT).orEmpty()
        val method = methods.firstOrNull { first == it || first.startsWith("$it ") } ?: ""
        if (method.isNotEmpty() && parts.size >= 2) {
            val who = parts.drop(1).firstOrNull { !looksLikeId(it) && it.any { c -> c.isLetter() } && it.uppercase(Locale.ROOT) !in methods }
            return Clean(if (who != null) titleCase(who).take(34) else "$method payment", method)
        }
        // not a structured narration: keep the words, drop long reference numbers
        val words = text.split(Regex("\\s+")).filter { w -> w.count { it.isDigit() } < 6 && !w.contains('*') }
        val nice = words.joinToString(" ").ifBlank { text }
        return Clean(if (nice == nice.uppercase(Locale.ROOT)) titleCase(nice).take(38) else nice.take(38), method)
    }

    /** "2026-11-24" -> "24 Nov" (with the year when it is not this year). */
    fun dayLabel(iso: String, today: LocalDate = LocalDate.now()): String {
        val d = runCatching { LocalDate.parse(iso.take(10)) }.getOrNull() ?: return iso
        val fmt = if (d.year == today.year) "d MMM" else "d MMM yyyy"
        return d.format(DateTimeFormatter.ofPattern(fmt, Locale.ENGLISH))
    }
}
