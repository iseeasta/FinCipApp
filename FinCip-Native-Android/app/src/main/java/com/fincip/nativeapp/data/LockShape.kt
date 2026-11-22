package com.fincip.nativeapp.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

/** Encoding and fuzzy matching for the gesture-based lock styles (drawing and rhythm). */
object LockShape {
    fun encodeDrawing(raw: List<Pair<Float, Float>>, n: Int = 32): String? {
        if (raw.size < 2) return null
        val d = FloatArray(raw.size)
        for (i in 1 until raw.size) d[i] = d[i - 1] + hypot(raw[i].first - raw[i - 1].first, raw[i].second - raw[i - 1].second)
        val total = d.last()
        if (total < 60f) return null
        val out = ArrayList<Pair<Float, Float>>()
        var seg = 1
        for (k in 0 until n) {
            val t = total * k / (n - 1)
            while (seg < raw.size - 1 && d[seg] < t) seg++
            val span = (d[seg] - d[seg - 1]).takeIf { it > 0f } ?: 1f
            val u = ((t - d[seg - 1]) / span).coerceIn(0f, 1f)
            out.add(Pair(raw[seg - 1].first + (raw[seg].first - raw[seg - 1].first) * u, raw[seg - 1].second + (raw[seg].second - raw[seg - 1].second) * u))
        }
        val cx = out.map { it.first }.average().toFloat()
        val cy = out.map { it.second }.average().toFloat()
        val w = out.maxOf { it.first } - out.minOf { it.first }
        val h = out.maxOf { it.second } - out.minOf { it.second }
        val dim = maxOf(w, h).coerceAtLeast(1f)
        return out.joinToString(";") { String.format(Locale.US, "%.3f,%.3f", (it.first - cx) / dim, (it.second - cy) / dim) }
    }

    private fun parse(s: String): List<Pair<Float, Float>> = s.split(";").mapNotNull { part ->
        val p = part.split(",")
        val x = p.getOrNull(0)?.toFloatOrNull()
        val y = p.getOrNull(1)?.toFloatOrNull()
        if (x != null && y != null) Pair(x, y) else null
    }

    fun sameDrawing(a: String, b: String): Boolean {
        val pa = parse(a); val pb = parse(b)
        if (pa.size < 2 || pa.size != pb.size) return false
        val mean = pa.indices.sumOf { hypot(pa[it].first - pb[it].first, pa[it].second - pb[it].second).toDouble() } / pa.size
        return mean < 0.17
    }

    fun encodeRhythm(times: List<Long>): String? {
        if (times.size < 4) return null
        val iv = times.zipWithNext { a, b -> (b - a).toFloat() }
        val total = iv.sum()
        if (total <= 0f) return null
        return iv.joinToString(",") { String.format(Locale.US, "%.4f", it / total) }
    }

    fun sameRhythm(a: String, b: String): Boolean {
        val x = a.split(",").mapNotNull { it.toFloatOrNull() }
        val y = b.split(",").mapNotNull { it.toFloatOrNull() }
        return x.size >= 2 && x.size == y.size && x.indices.all { abs(x[it] - y[it]) < 0.12f }
    }
}
