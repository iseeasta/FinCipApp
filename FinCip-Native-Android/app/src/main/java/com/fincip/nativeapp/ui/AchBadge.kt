package com.fincip.nativeapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BRONZE = Color(0xFFC9803F)
private val SILVER = Color(0xFFD5DCE6)
private val GOLD = Color(0xFFFFC93C)

/**
 * Badge art. The design comes from the achievement (glyph x shape x hue, all unique), the detail from the stage:
 * stage 1 is a flat medal, stage 2 adds gradient, ring, highlights and stars, stage 3 adds wings, rim gems, glow and a crown gem.
 */
@Composable
fun AchBadge(glyph: Int, shape: Int, hue: Int, tier: Int, size: Dp = 64.dp, locked: Boolean = false, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawBadge(glyph, shape, hue, tier.coerceIn(1, 3), locked) }
}

fun DrawScope.drawBadge(glyph: Int, shape: Int, hue: Int, tier: Int, locked: Boolean) {
    val u = size.minDimension / 100f
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun sz(w: Float, h: Float) = Size(w * u, h * u)
    val base = if (locked) Color(0xFF9AA0AA) else Color.hsv(hue.toFloat(), .62f, .92f)
    val dark = if (locked) Color(0xFF6B717C) else Color.hsv(hue.toFloat(), .75f, .5f)
    val light = if (locked) Color(0xFFD3D7DE) else Color.hsv(hue.toFloat(), .25f, 1f)
    val metal = if (locked) Color(0xFFB7BCC6) else when (tier) { 1 -> BRONZE; 2 -> SILVER; else -> GOLD }
    val metalDark = metal.copy(red = metal.red * .65f, green = metal.green * .65f, blue = metal.blue * .65f)
    val cx = 50f; val cy = 52f
    val rim = when (tier) { 1 -> 40f; 2 -> 42f; else -> 37f }
    val face = rim - when (tier) { 1 -> 5f; else -> 6f }

    fun poly(n: Int, r: Float, rot: Float, stretch: Float = 1f): Path = Path().apply {
        for (i in 0 until n) { val a = rot + i * 2 * PI.toFloat() / n; val x = (cx + r * cos(a) * stretch) * u; val y = (cy + r * sin(a)) * u; if (i == 0) moveTo(x, y) else lineTo(x, y) }; close()
    }
    fun shapePath(r: Float): Path = when (shape) {
        0 -> Path().apply { addOval(androidx.compose.ui.geometry.Rect(o(cx - r, cy - r), sz(2 * r, 2 * r))) }
        1 -> Path().apply { moveTo((cx - r) * u, (cy - r * .8f) * u); lineTo((cx + r) * u, (cy - r * .8f) * u); lineTo((cx + r) * u, (cy + r * .1f) * u); cubicTo((cx + r) * u, (cy + r * .7f) * u, (cx + r * .4f) * u, (cy + r * .95f) * u, cx * u, (cy + r * 1.1f) * u); cubicTo((cx - r * .4f) * u, (cy + r * .95f) * u, (cx - r) * u, (cy + r * .7f) * u, (cx - r) * u, (cy + r * .1f) * u); close() }
        2 -> poly(6, r * 1.05f, (PI / 6).toFloat())
        3 -> Path().apply { val n = 24; for (i in 0 until n) { val a = i * 2 * PI.toFloat() / n; val rr = if (i % 2 == 0) r * 1.08f else r * .9f; val x = (cx + rr * cos(a)) * u; val y = (cy + rr * sin(a)) * u; if (i == 0) moveTo(x, y) else lineTo(x, y) }; close() }
        4 -> poly(4, r * 1.2f, (PI / 4 * 2).toFloat(), .86f)
        5 -> poly(8, r * 1.04f, (PI / 8).toFloat())
        6 -> Path().apply { for (i in 0 until 8) { val a = i * 2 * PI.toFloat() / 8; addOval(androidx.compose.ui.geometry.Rect(o(cx + r * .62f * cos(a) - r * .42f, cy + r * .62f * sin(a) - r * .42f), sz(r * .84f, r * .84f))) }; addOval(androidx.compose.ui.geometry.Rect(o(cx - r * .8f, cy - r * .8f), sz(r * 1.6f, r * 1.6f))) }
        7 -> Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(o(cx - r, cy - r), sz(2 * r, 2 * r)), CornerRadius(r * .5f * u))) }
        8 -> Path().apply { moveTo(cx * u, (cy - r * 1.18f) * u); cubicTo((cx + r * .5f) * u, (cy - r * .5f) * u, (cx + r) * u, (cy + r * .1f) * u, (cx + r * .9f) * u, (cy + r * .45f) * u); cubicTo((cx + r * .7f) * u, (cy + r * 1.0f) * u, (cx - r * .7f) * u, (cy + r * 1.0f) * u, (cx - r * .9f) * u, (cy + r * .45f) * u); cubicTo((cx - r) * u, (cy + r * .1f) * u, (cx - r * .5f) * u, (cy - r * .5f) * u, cx * u, (cy - r * 1.18f) * u); close() }
        else -> poly(5, r * 1.08f, (-PI / 2).toFloat())
    }
    fun sparkle(x: Float, y: Float, r: Float, col: Color = Color.White) {
        val p = Path().apply { moveTo(x * u, (y - r) * u); quadraticBezierTo(x * u, y * u, (x + r) * u, y * u); quadraticBezierTo(x * u, y * u, x * u, (y + r) * u); quadraticBezierTo(x * u, y * u, (x - r) * u, y * u); quadraticBezierTo(x * u, y * u, x * u, (y - r) * u); close() }
        drawPath(p, col)
    }

    // ---- tier 3: glow and wings sit behind the medal
    if (tier >= 3 && !locked) {
        drawCircle(Brush.radialGradient(listOf(GOLD.copy(alpha = .45f), Color.Transparent), o(cx, cy), 50f * u), 50f * u, o(cx, cy))
        for (s in listOf(-1f, 1f)) for (k in 0 until 4) {
            val p = Path().apply { moveTo((cx + s * 30f) * u, (cy - 6f + k * 7f) * u); quadraticBezierTo((cx + s * (44f + k * 1.5f)) * u, (cy - 20f + k * 8f) * u, (cx + s * (48f - k * 2f)) * u, (cy - 12f + k * 10f) * u); quadraticBezierTo((cx + s * 40f) * u, (cy - 2f + k * 7f) * u, (cx + s * 30f) * u, (cy + 1f + k * 7f) * u); close() }
            drawPath(p, if (k % 2 == 0) GOLD else Color(0xFFFFE08A))
        }
    }
    if (tier >= 3 && locked) for (s in listOf(-1f, 1f)) drawCircle(metal.copy(alpha = .5f), 4f * u, o(cx + s * 44f, cy))

    // ---- rim, face
    drawPath(shapePath(rim), Brush.linearGradient(listOf(lerpC(metal, Color.White, .45f), metal, metalDark), o(cx - rim, cy - rim), o(cx + rim, cy + rim)))
    val facePath = shapePath(face)
    if (tier == 1) drawPath(facePath, base) else drawPath(facePath, Brush.linearGradient(listOf(light, base, dark), o(0f, cy - face), o(0f, cy + face)))
    if (tier >= 2) drawPath(shapePath(face - 4.5f), Color.White.copy(alpha = .55f), style = Stroke(1.1f * u))
    if (tier >= 2) drawArc(Color.White.copy(alpha = .28f), 200f, 70f, false, o(cx - face * .8f, cy - face * .85f), sz(face * 1.6f, face * 1.5f), style = Stroke(3f * u, cap = StrokeCap.Round))
    if (tier >= 3) for (i in 0 until 8) { val a = i * 2 * PI.toFloat() / 8 - PI.toFloat() / 2; drawCircle(if (locked) metal else if (i % 2 == 0) Color(0xFFFF5C8A) else Color(0xFF5CD6FF), 1.9f * u, o(cx + (rim - 1.8f) * cos(a), cy + (rim - 1.8f) * sin(a))) }
    if (tier >= 3 && !locked) { poly4Gem(cx, cy - rim - 1f, 5.2f, u, this); sparkle(14f, 20f, 5f); sparkle(88f, 26f, 4f); sparkle(84f, 84f, 3f) }
    else if (tier >= 2 && !locked) { sparkle(20f, 24f, 3.8f); sparkle(82f, 30f, 3f) }

    // ---- glyph
    drawGlyph(glyph, tier, locked, cx, cy + 1f, u, base, dark, light)
}

private fun lerpC(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

private fun poly4Gem(x: Float, y: Float, r: Float, u: Float, s: DrawScope) = with(s) {
    val p = Path().apply { moveTo(x * u, (y - r) * u); lineTo((x + r) * u, y * u); lineTo(x * u, (y + r * 1.2f) * u); lineTo((x - r) * u, y * u); close() }
    drawPath(p, Brush.linearGradient(listOf(Color(0xFFFF8FB8), Color(0xFFE5326E)), Offset((x - r) * u, (y - r) * u), Offset((x + r) * u, (y + r) * u)))
    drawPath(p, Color.White.copy(alpha = .8f), style = Stroke(.7f * u))
}

private fun DrawScope.drawGlyph(g: Int, tier: Int, locked: Boolean, cx: Float, cy: Float, u: Float, base: Color, dark: Color, light: Color) {
    val ink = if (locked) Color(0xFF4A4F59) else Color.White
    val acc = if (locked) Color(0xFF4A4F59) else dark
    val warm = if (locked) Color(0xFFC3C7CE) else Color(0xFFFFD36B)
    fun o(x: Float, y: Float) = Offset((cx + x) * u, (cy + y) * u)
    fun sz(w: Float, h: Float) = Size(w * u, h * u)
    fun circ(x: Float, y: Float, r: Float, c: Color) = drawCircle(c, r * u, o(x, y))
    fun rr(x: Float, y: Float, w: Float, h: Float, rad: Float, c: Color) = drawRoundRect(c, o(x, y), sz(w, h), CornerRadius(rad * u))
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, c: Color) = drawLine(c, o(x1, y1), o(x2, y2), w * u, StrokeCap.Round)
    fun path(c: Color, style: androidx.compose.ui.graphics.drawscope.DrawStyle = androidx.compose.ui.graphics.drawscope.Fill, f: Path.(Float, Float) -> Unit) { drawPath(Path().apply { f(cx * u, cy * u) }, c, style = style) }
    fun poly(c: Color, vararg p: Float) = path(c) { px, py -> moveTo(px + p[0] * u, py + p[1] * u); var i = 2; while (i < p.size) { lineTo(px + p[i] * u, py + p[i + 1] * u); i += 2 }; close() }
    fun spark(x: Float, y: Float, r: Float) = path(if (locked) Color.Transparent else Color.White) { px, py -> moveTo(px + x * u, py + (y - r) * u); quadraticBezierTo(px + x * u, py + y * u, px + (x + r) * u, py + y * u); quadraticBezierTo(px + x * u, py + y * u, px + x * u, py + (y + r) * u); quadraticBezierTo(px + x * u, py + y * u, px + (x - r) * u, py + y * u); quadraticBezierTo(px + x * u, py + y * u, px + x * u, py + (y - r) * u); close() }
    val st = Stroke(1.6f * u, cap = StrokeCap.Round)

    when (g) {
        0 -> { circ(0f, 0f, 15f, warm); circ(0f, 0f, 12f, lerpC(warm, Color.White, .25f)); drawCircle(acc.copy(alpha = .6f), 10f * u, o(0f, 0f), style = Stroke(1f * u))
            ln(-5f, -5f, 5f, -5f, 1.8f, acc); ln(-5f, -1f, 5f, -1f, 1.8f, acc); drawArc(acc, -90f, 180f, false, o(-5f, -5f), sz(10f, 8f), style = Stroke(1.8f * u)); ln(-3f, -1f, 4f, 8f, 1.8f, acc)
            if (tier >= 2) drawArc(Color.White.copy(alpha = .7f), 200f, 60f, false, o(-12f, -12f), sz(24f, 24f), style = Stroke(1.6f * u, cap = StrokeCap.Round)); if (tier >= 3) { spark(13f, -13f, 5f); circ(0f, 0f, 15f, Color.Transparent) } }
        1 -> { path(ink) { px, py -> moveTo(px - 12f * u, py + 13f * u); cubicTo(px - 16f * u, py - 6f * u, px + 2f * u, py - 17f * u, px + 15f * u, py - 14f * u); cubicTo(px + 16f * u, py + 2f * u, px + 2f * u, py + 15f * u, px - 12f * u, py + 13f * u); close() }
            ln(-11f, 12f, 9f, -9f, 1.6f, acc); if (tier >= 2) { ln(-5f, 6f, 2f, 7f, 1.2f, acc); ln(-1f, 1f, 6f, 1f, 1.2f, acc) }; if (tier >= 3) { circ(-14f, -10f, 2.4f, Color(0xFF8EE0FF)); spark(-13f, -14f, 4f) } }
        2 -> { path(warm) { px, py -> moveTo(px, py - 17f * u); cubicTo(px + 4f * u, py - 8f * u, px + 15f * u, py - 4f * u, px + 12f * u, py + 8f * u); cubicTo(px + 10f * u, py + 17f * u, px - 10f * u, py + 17f * u, px - 12f * u, py + 8f * u); cubicTo(px - 14f * u, py - 2f * u, px - 3f * u, py - 6f * u, px, py - 17f * u); close() }
            path(Color(0xFFFF7A3D)) { px, py -> moveTo(px, py - 7f * u); cubicTo(px + 6f * u, py, px + 9f * u, py + 6f * u, px + 5f * u, py + 12f * u); cubicTo(px + 2f * u, py + 15f * u, px - 6f * u, py + 14f * u, px - 6f * u, py + 8f * u); cubicTo(px - 6f * u, py + 3f * u, px - 1f * u, py, px, py - 7f * u); close() }
            if (tier >= 2) circ(0f, 9f, 3.4f, Color(0xFFFFF0B0)); if (tier >= 3) { spark(-13f, -10f, 3.6f); spark(14f, -12f, 3f) } }
        3 -> { circ(0f, 0f, 16f, ink); circ(0f, 0f, 12f, acc); circ(0f, 0f, 8f, ink); circ(0f, 0f, 4f, if (locked) acc else Color(0xFFFF4D6D)); ln(0f, 0f, 14f, -14f, 1.6f, warm); poly(warm, 11f, -17f, 17f, -17f, 17f, -11f); if (tier >= 2) drawCircle(ink.copy(alpha = .5f), 19f * u, o(0f, 0f), style = Stroke(1f * u)); if (tier >= 3) spark(-15f, 13f, 4f) }
        4 -> { rr(-15f, 2f, 7f, 12f, 1.5f, ink.copy(alpha = .85f)); rr(-5f, -5f, 7f, 19f, 1.5f, ink); rr(5f, -11f, 7f, 25f, 1.5f, ink.copy(alpha = .85f)); if (tier >= 2) rr(15f, -15f, 5f, 29f, 1.5f, ink)
            drawPath(Path().apply { moveTo((cx - 16f) * u, (cy - 2f) * u); lineTo((cx - 4f) * u, (cy - 9f) * u); lineTo((cx + 4f) * u, (cy - 5f) * u); lineTo((cx + 16f) * u, (cy - 17f) * u) }, warm, style = Stroke(2f * u, cap = StrokeCap.Round)); poly(warm, 18f, -21f, 12f, -19f, 17f, -12f); if (tier >= 3) spark(-12f, -15f, 4.5f) }
        5 -> { path(ink) { px, py -> moveTo(px, py - 10f * u); quadraticBezierTo(px - 9f * u, py - 15f * u, px - 18f * u, py - 11f * u); lineTo(px - 18f * u, py + 11f * u); quadraticBezierTo(px - 9f * u, py + 7f * u, px, py + 12f * u); quadraticBezierTo(px + 9f * u, py + 7f * u, px + 18f * u, py + 11f * u); lineTo(px + 18f * u, py - 11f * u); quadraticBezierTo(px + 9f * u, py - 15f * u, px, py - 10f * u); close() }
            ln(0f, -9f, 0f, 11f, 1.4f, acc); if (tier >= 2) for (i in 0 until 3) { ln(-14f, -6f + i * 5f, -4f, -6f + i * 5f + 1f, 1f, acc.copy(alpha = .7f)); ln(4f, -5f + i * 5f, 14f, -6f + i * 5f, 1f, acc.copy(alpha = .7f)) }; if (tier >= 3) poly(Color(0xFFFF5C8A), 8f, -12f, 13f, -12f, 13f, 3f, 10.5f, 0f, 8f, 3f) }
        6 -> { circ(-8f, -6f, 9f, warm); circ(-8f, -6f, 4f, base); ln(-2f, 0f, 14f, 16f, 3.4f, warm); ln(9f, 11f, 13f, 7f, 2.6f, warm); ln(13f, 15f, 17f, 11f, 2.6f, warm); if (tier >= 2) drawArc(Color.White.copy(alpha = .7f), 190f, 70f, false, o(-15f, -13f), sz(14f, 14f), style = Stroke(1.4f * u, cap = StrokeCap.Round)); if (tier >= 3) spark(8f, -12f, 5f) }
        7 -> { drawArc(ink, 180f, 180f, false, o(-9f, -17f), sz(18f, 20f), style = Stroke(4f * u)); rr(-14f, -5f, 28f, 21f, 4f, warm); circ(0f, 3f, 3.4f, acc); rr(-1.2f, 4f, 2.4f, 7f, 1f, acc); if (tier >= 2) rr(-11f, -2f, 22f, 3f, 1.5f, Color.White.copy(alpha = .35f)); if (tier >= 3) spark(15f, -9f, 4.5f) }
        8 -> { poly(warm, -17f, 11f, -17f, -8f, -8f, 2f, 0f, -14f, 8f, 2f, 17f, -8f, 17f, 11f); rr(-17f, 11f, 34f, 5f, 2f, lerpC(warm, Color.Black, .15f)); if (tier >= 2) { circ(-17f, -9f, 2.6f, ink); circ(0f, -15f, 2.6f, ink); circ(17f, -9f, 2.6f, ink) }; if (tier >= 3) { poly(Color(0xFFFF5C8A), 0f, 1f, 4f, 5f, 0f, 10f, -4f, 5f); circ(-9f, 13.5f, 1.4f, ink); circ(9f, 13.5f, 1.4f, ink) } }
        9 -> { path(ink) { px, py -> moveTo(px, py - 18f * u); cubicTo(px + 10f * u, py - 8f * u, px + 10f * u, py + 6f * u, px + 6f * u, py + 12f * u); lineTo(px - 6f * u, py + 12f * u); cubicTo(px - 10f * u, py + 6f * u, px - 10f * u, py - 8f * u, px, py - 18f * u); close() }
            circ(0f, -5f, 4f, acc); circ(0f, -5f, 2.4f, Color(0xFF9EE4FF)); poly(warm, -6f, 6f, -14f, 14f, -6f, 12f); poly(warm, 6f, 6f, 14f, 14f, 6f, 12f); path(Color(0xFFFF7A3D)) { px, py -> moveTo(px - 4f * u, py + 12f * u); lineTo(px, py + 20f * u); lineTo(px + 4f * u, py + 12f * u); close() }
            if (tier >= 2) { ln(-2f, -12f, -2f, 6f, 1f, Color.White.copy(alpha = .0f)); spark(-15f, -8f, 3f) }; if (tier >= 3) { spark(15f, -12f, 4f); spark(-13f, 10f, 3f) } }
        10 -> { path(Color(0xFFFF5C8A).takeIf { !locked } ?: ink) { px, py -> moveTo(px, py + 15f * u); cubicTo(px - 22f * u, py - 2f * u, px - 12f * u, py - 18f * u, px, py - 7f * u); cubicTo(px + 12f * u, py - 18f * u, px + 22f * u, py - 2f * u, px, py + 15f * u); close() }
            if (tier >= 2) drawArc(Color.White.copy(alpha = .75f), 200f, 60f, false, o(-14f, -12f), sz(10f, 10f), style = Stroke(1.6f * u, cap = StrokeCap.Round)); if (tier >= 3) { circ(0f, 0f, 5f, warm); ln(-2f, 0f, 2f, 0f, 1.2f, acc); spark(15f, -13f, 4f) } }
        11 -> { poly(ink, -12f, -8f, -6f, -15f, 6f, -15f, 12f, -8f, 0f, 15f); poly(light.copy(alpha = .9f), -12f, -8f, 12f, -8f, 0f, 15f); ln(-12f, -8f, 12f, -8f, 1f, acc); ln(-6f, -15f, -3f, -8f, 1f, acc); ln(6f, -15f, 3f, -8f, 1f, acc); ln(-3f, -8f, 0f, 15f, 1f, acc); ln(3f, -8f, 0f, 15f, 1f, acc); if (tier >= 3) { spark(-15f, -14f, 4.5f); spark(15f, 8f, 3.4f) } }
        12 -> { circ(0f, 0f, 16f, ink); circ(0f, 0f, 13f, acc.copy(alpha = .25f)); ln(0f, 0f, 0f, -9f, 2f, acc); ln(0f, 0f, 7f, 3f, 2f, acc); circ(0f, 0f, 1.8f, acc); if (tier >= 2) for (i in 0 until 12) { val a = i * PI.toFloat() / 6; ln(12f * cos(a), 12f * sin(a), 13.8f * cos(a), 13.8f * sin(a), 1f, acc) }; if (tier >= 3) { circ(-12f, -14f, 4f, warm); circ(12f, -14f, 4f, warm) } }
        13 -> { rr(-16f, -13f, 32f, 29f, 4f, ink); rr(-16f, -13f, 32f, 9f, 4f, warm); ln(-8f, -17f, -8f, -9f, 2.2f, acc); ln(8f, -17f, 8f, -9f, 2.2f, acc); for (r in 0 until 2) for (c in 0 until 4) circ(-10f + c * 6.6f, 2f + r * 7f, 1.6f, acc.copy(alpha = .6f)); if (tier >= 2) circ(3.4f, 9f, 3.2f, warm); if (tier >= 3) { ln(-4f, 8f, -1f, 12f, 2f, Color(0xFF2FBF71)); ln(-1f, 12f, 6f, 4f, 2f, Color(0xFF2FBF71)) } }
        14 -> { ln(-11f, -17f, -11f, 17f, 2.4f, ink); path(warm) { px, py -> moveTo(px - 10f * u, py - 16f * u); cubicTo(px - 2f * u, py - 20f * u, px + 4f * u, py - 11f * u, px + 15f * u, py - 15f * u); lineTo(px + 15f * u, py + 2f * u); cubicTo(px + 4f * u, py + 6f * u, px - 2f * u, py - 3f * u, px - 10f * u, py + 1f * u); close() }
            if (tier >= 2) ln(-8f, -9f, 12f, -9f, 1f, Color.White.copy(alpha = .6f)); if (tier >= 3) spark(3f, -8f, 4f) }
        15 -> { poly(ink, -14f, -6f, 14f, -6f, 17f, 16f, -17f, 16f); drawArc(ink, 180f, 180f, false, o(-8f, -16f), sz(16f, 18f), style = Stroke(2.6f * u)); circ(-6f, -2f, 1.6f, acc); circ(6f, -2f, 1.6f, acc); if (tier >= 2) rr(-5f, 4f, 10f, 6f, 1.5f, warm); if (tier >= 3) spark(16f, -12f, 4.5f) }
        16 -> { rr(-18f, -12f, 36f, 24f, 4f, ink); rr(-18f, -6f, 36f, 6f, 0f, acc); rr(-14f, 4f, 9f, 6f, 1.5f, warm); if (tier >= 2) { ln(1f, 7f, 14f, 7f, 1.4f, acc.copy(alpha = .6f)) }; if (tier >= 3) { circ(12f, -9f, 2.4f, Color(0xFFFF5C8A)); circ(15f, -9f, 2.4f, warm); spark(-14f, -16f, 4f) } }
        17 -> { rr(-17f, -9f, 34f, 25f, 5f, ink); rr(-17f, -9f, 34f, 9f, 5f, light.copy(alpha = .8f)); rr(6f, 1f, 14f, 9f, 3f, warm); circ(11f, 5.5f, 1.8f, acc); if (tier >= 2) ln(-12f, -14f, 12f, -12f, 1.4f, acc.copy(alpha = .0f)); if (tier >= 3) { circ(-4f, -12f, 5f, warm); circ(-4f, -12f, 3f, lerpC(warm, Color.White, .4f)) } }
        18 -> { poly(ink, -18f, -5f, 0f, -17f, 18f, -5f); for (i in 0 until 4) rr(-14f + i * 8f, -3f, 5f, 15f, 1f, ink); rr(-18f, 13f, 36f, 4f, 1.5f, ink); circ(0f, -8f, 2.6f, warm); if (tier >= 2) ln(-14f, -5f, 14f, -5f, 1f, acc.copy(alpha = .5f)); if (tier >= 3) spark(0f, -20f, 5f) }
        19 -> { drawOval(ink, o(-16f, -8f), sz(30f, 22f)); circ(-13f, -6f, 4.4f, ink); drawOval(warm.copy(alpha = .0f), o(10f, -4f), sz(8f, 8f)); rr(10f, -3f, 9f, 8f, 3f, lerpC(ink, base, .35f)); circ(-6f, -3f, 1.6f, acc); rr(-12f, 11f, 5f, 7f, 2f, ink); rr(4f, 11f, 5f, 7f, 2f, ink); rr(-5f, -6f, 8f, 2f, 1f, acc); drawArc(ink, 0f, 270f, false, o(-20f, -1f), sz(8f, 8f), style = Stroke(1.6f * u)); if (tier >= 2) circ(-1f, -16f, 4.4f, warm); if (tier >= 3) spark(16f, -14f, 4f) }
        20 -> { poly(warm, 3f, -18f, -11f, 3f, -1f, 3f, -4f, 18f, 11f, -3f, 1f, -3f); if (tier >= 2) drawPath(Path().apply { moveTo((cx + 3f) * u, (cy - 18f) * u); lineTo((cx - 11f) * u, (cy + 3f) * u); lineTo((cx - 1f) * u, (cy + 3f) * u) }, Color.White.copy(alpha = .7f), style = Stroke(1f * u)); if (tier >= 3) { drawCircle(ink.copy(alpha = .55f), 19f * u, o(0f, 0f), style = Stroke(1f * u)); spark(-15f, -12f, 4f) } }
        21 -> { circ(0f, 0f, 16f, ink); drawOval(acc.copy(alpha = .4f), o(-7f, -16f), sz(14f, 32f)); ln(-16f, 0f, 16f, 0f, 1.2f, acc.copy(alpha = .5f)); drawArc(acc.copy(alpha = .5f), 200f, 140f, false, o(-14f, -10f), sz(28f, 20f), style = Stroke(1.1f * u)); if (tier >= 2) circ(-5f, 6f, 4f, Color(0xFF58C27D).takeIf { !locked } ?: acc); if (tier >= 3) { drawOval(warm, o(-22f, -5f), sz(44f, 10f), style = Stroke(1.6f * u)); spark(18f, -14f, 4f) } }
        22 -> { rr(-12f, -15f, 24f, 30f, 3f, ink); circ(-12f, -15f, 3f, light); circ(-12f, 15f, 3f, light); for (i in 0 until 4) ln(-6f, -8f + i * 6f, 6f, -8f + i * 6f, 1.2f, acc.copy(alpha = .7f)); if (tier >= 2) circ(8f, 12f, 4.4f, Color(0xFFFF5C8A).takeIf { !locked } ?: acc); if (tier >= 3) { ln(6f, 15f, 4f, 20f, 1.6f, Color(0xFFFF5C8A)); ln(10f, 15f, 12f, 20f, 1.6f, Color(0xFFFF5C8A)) } }
        23 -> { path(warm) { px, py -> moveTo(px - 12f * u, py - 15f * u); lineTo(px + 12f * u, py - 15f * u); cubicTo(px + 12f * u, py + 4f * u, px + 5f * u, py + 7f * u, px, py + 7f * u); cubicTo(px - 5f * u, py + 7f * u, px - 12f * u, py + 4f * u, px - 12f * u, py - 15f * u); close() }
            drawArc(warm, 90f, 180f, false, o(-20f, -13f), sz(10f, 12f), style = Stroke(2f * u)); drawArc(warm, -90f, 180f, false, o(10f, -13f), sz(10f, 12f), style = Stroke(2f * u)); rr(-2.4f, 6f, 4.8f, 6f, 0f, warm); rr(-9f, 12f, 18f, 4.4f, 1.6f, warm); if (tier >= 2) ln(-7f, -11f, -7f, -3f, 1.6f, Color.White.copy(alpha = .6f)); if (tier >= 3) poly(ink, 0f, -12f, 2.6f, -7f, 8f, -6f, 4f, -2f, 5f, 4f, 0f, 1f, -5f, 4f, -4f, -2f, -8f, -6f, -2.6f, -7f) }
        else -> { poly(warm, 0f, -17f, 4.5f, -6f, 16f, -6f, 7f, 2f, 10f, 14f, 0f, 7f, -10f, 14f, -7f, 2f, -16f, -6f, -4.5f, -6f); if (tier >= 2) poly(Color.White.copy(alpha = .35f), 0f, -17f, 4.5f, -6f, 0f, -4f, -4.5f, -6f); if (tier >= 3) { circ(0f, 0f, 20f, Color.Transparent); spark(-18f, -12f, 3.4f); spark(18f, 10f, 3.4f) } }
    }
}
