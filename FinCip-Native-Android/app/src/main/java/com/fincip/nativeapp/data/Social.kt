package com.fincip.nativeapp.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * What you choose to share so others can be placed near or far from you: the shape of your spending by category,
 * which planning tools you use, and your level. No amounts, no transactions, no names of payees.
 */
data class Signature(val cats: Map<String, Double>, val tags: Set<String>, val level: Int) {
    fun toJson() = JsonObject().apply {
        add("cats", JsonObject().also { o -> cats.forEach { (k, v) -> o.addProperty(k, Math.round(v * 1000) / 1000.0) } })
        add("tags", JsonArray().also { a -> tags.sorted().forEach { a.add(it) } }); addProperty("level", level)
    }
    companion object {
        fun fromJson(o: JsonObject?): Signature {
            if (o == null) return Signature(emptyMap(), emptySet(), 1)
            val c = if (o.has("cats") && o.get("cats").isJsonObject) o.getAsJsonObject("cats").entrySet().mapNotNull { (k, v) -> runCatching { k to v.asDouble }.getOrNull() }.toMap() else emptyMap()
            val t = if (o.has("tags") && o.get("tags").isJsonArray) o.getAsJsonArray("tags").mapNotNull { runCatching { it.asString }.getOrNull() }.toSet() else emptySet()
            return Signature(c, t, if (o.has("level")) runCatching { o.get("level").asInt }.getOrDefault(1) else 1)
        }
    }
}

object Social {
    /** Builds your signature from your own data. Category shares are of spending count, top eight only. */
    fun signature(ws: JsonObject, level: Int): Signature {
        val spend = transactions(ws).filter { isSpend(it) }
        val counts = spend.map { it.string("category").trim().lowercase() }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
        val total = counts.values.sum().coerceAtLeast(1).toDouble()
        val top = counts.entries.sortedByDescending { it.value }.take(8).associate { it.key to it.value / total }
        val tags = buildSet {
            if (ws.arrayOrEmpty("budgets").size() > 0) add("budgets"); if (ws.arrayOrEmpty("goals").size() > 0) add("goals")
            if (ws.arrayOrEmpty("investments").size() > 0) add("investing"); if (ws.arrayOrEmpty("netWorthItems").size() > 0) add("net-worth")
            if (ws.arrayOrEmpty("recurringRules").size() > 0) add("recurring"); if (ws.arrayOrEmpty("receipts").size() > 0) add("receipts")
            if (ws.arrayOrEmpty("sharedMembers").size() > 0) add("household"); if (ws.arrayOrEmpty("smartRules").size() > 0) add("automation")
            if (ws.arrayOrEmpty("financeProducts").size() > 0) add("products")
            ws.arrayOrEmpty("goals").objects().forEach { if (it.string("type") == "debt") add("debt-payoff") else add("saving") }
        }
        return Signature(top, tags, level)
    }

    /** 0..1. Mix of spending-pattern overlap, shared tools, and level closeness. Symmetric. */
    fun similarity(a: Signature, b: Signature): Double {
        val keys = a.cats.keys + b.cats.keys
        val cos = if (a.cats.isEmpty() || b.cats.isEmpty()) 0.0 else {
            var dot = 0.0; var na = 0.0; var nb = 0.0
            keys.forEach { k -> val x = a.cats[k] ?: 0.0; val y = b.cats[k] ?: 0.0; dot += x * y; na += x * x; nb += y * y }
            if (na == 0.0 || nb == 0.0) 0.0 else dot / (sqrt(na) * sqrt(nb))
        }
        val union = (a.tags + b.tags).size
        val jac = if (union == 0) 0.0 else (a.tags intersect b.tags).size.toDouble() / union
        val lvl = 1.0 - (abs(a.level - b.level).toDouble() / 10.0).coerceAtMost(1.0)
        val haveCats = a.cats.isNotEmpty() && b.cats.isNotEmpty()
        return if (haveCats) 0.5 * cos + 0.3 * jac + 0.2 * lvl else 0.6 * jac + 0.4 * lvl
    }

    /** What two people share, for the "why is this planet close" card. */
    fun commonGround(a: Signature, b: Signature): List<String> {
        val cats = a.cats.keys.intersect(b.cats.keys).sortedByDescending { minOf(a.cats[it] ?: 0.0, b.cats[it] ?: 0.0) }.take(3).map { "both spend on $it" }
        val tags = (a.tags intersect b.tags).sorted().take(3).map { "both use $it" }
        return cats + tags
    }

    data class Orbit(val id: String, val radius: Double, val angle0: Double, val speed: Double, val similarity: Double)

    /** Closer to the sun means more in common. Rings never overlap: each is at least [gap] further than the last. */
    fun orbits(people: List<Pair<String, Double>>, rMin: Double, rMax: Double, gap: Double): List<Orbit> {
        val sorted = people.sortedByDescending { it.second }
        val out = ArrayList<Orbit>()
        var last = rMin - gap
        sorted.forEach { (id, sim) ->
            val wanted = rMin + (1.0 - sim.coerceIn(0.0, 1.0)) * (rMax - rMin)
            val r = maxOf(wanted, last + gap).also { last = it }
            val h = (id.hashCode().toLong() and 0x7fffffff)
            out.add(Orbit(id, r, (h % 6283) / 1000.0, 0.35 / sqrt(r / rMin).coerceAtLeast(1.0).pow(1.5), sim))
        }
        return out
    }

    private const val QR_PREFIX = "fincip://friend/"
    fun qrPayload(token: String) = QR_PREFIX + token
    /** Returns the token from a scanned string, or null if it isn't a FinCip friend code. */
    fun parseQr(text: String?): String? {
        val t = text?.trim() ?: return null
        if (!t.startsWith(QR_PREFIX)) return null
        val tok = t.removePrefix(QR_PREFIX)
        return if (tok.length in 16..64 && tok.all { it.isLetterOrDigit() }) tok else null
    }
}
