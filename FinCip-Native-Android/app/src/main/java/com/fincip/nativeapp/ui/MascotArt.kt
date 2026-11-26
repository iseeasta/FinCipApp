package com.fincip.nativeapp.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fincip.nativeapp.data.MascotLook
import com.fincip.nativeapp.data.MascotOptions
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** The look currently chosen by the user, shared so the logo, avatars and cards can all show the same mascot. */
object MascotRuntime { var look by mutableStateOf<MascotLook?>(null) }

private val SKIN = Color(0xFFFFE4D6)
private val SKIN_SH = Color(0xFFF4C9B8)
private val INK = Color(0xFF2A1A33)

private fun bgColors(id: String): Pair<Color, Color> = when (id) {
    "dusk" -> Color(0xFFC9A7FF) to Color(0xFF5B49E0)
    "night" -> Color(0xFF5A4BC8) to Color(0xFF1F1A5C)
    "ocean" -> Color(0xFF9ED8FF) to Color(0xFF4D7BFF)
    "mint" -> Color(0xFFC6F3E2) to Color(0xFF52C7A5)
    "rose" -> Color(0xFFFFC9DD) to Color(0xFFE56FA6)
    else -> Color(0xFFD9CCFF) to Color(0xFF8A6BFF)
}

/** Everything is drawn in a 100 x 100 unit square, scaled to the canvas. Layers go bottom to top. */
fun DrawScope.drawMascot(look: MascotLook, t: Float = 0f, blink: Boolean = false, transparentBg: Boolean = false) {
    val u = size.minDimension / 100f
    val ch = MascotOptions.character(look.character)
    val hair = Color(look.hairArgb.toInt()); val hairLight = lerp(hair, Color.White, .28f); val hairDark = lerp(hair, Color.Black, .25f)
    val outfit = Color(look.outfitArgb.toInt()); val outfitDark = lerp(outfit, Color.Black, .22f); val outfitLight = lerp(outfit, Color.White, .35f)
    val onOutfit = if (outfit.red * .3f + outfit.green * .59f + outfit.blue * .11f > .6f) INK else Color.White
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun sz(w: Float, h: Float) = Size(w * u, h * u)
    fun star(cx: Float, cy: Float, r: Float, a: Float) {
        val p = Path().apply { moveTo(cx * u, (cy - r) * u); quadraticBezierTo(cx * u, cy * u, (cx + r) * u, cy * u); quadraticBezierTo(cx * u, cy * u, cx * u, (cy + r) * u); quadraticBezierTo(cx * u, cy * u, (cx - r) * u, cy * u); quadraticBezierTo(cx * u, cy * u, cx * u, (cy - r) * u); close() }
        drawPath(p, Color.White.copy(alpha = a))
    }
    fun poly(vararg pts: Float, c: Color) { val p = Path(); p.moveTo(pts[0] * u, pts[1] * u); var i = 2; while (i < pts.size) { p.lineTo(pts[i] * u, pts[i + 1] * u); i += 2 }; p.close(); drawPath(p, c) }
    fun coin(cx: Float, cy: Float, r: Float) {
        drawCircle(Color(0xFFE7A92B), r * u, o(cx, cy)); drawCircle(Color(0xFFFFD36B), r * .86f * u, o(cx, cy)); drawCircle(Color(0xFFE7A92B), r * .66f * u, o(cx, cy), style = Stroke(.9f * u))
        val w = Stroke(1.3f * u, cap = StrokeCap.Round); val k = r * .36f
        drawLine(Color(0xFF9A6410), o(cx - k, cy - k * .9f), o(cx + k, cy - k * .9f), strokeWidth = w.width, cap = StrokeCap.Round)
        drawLine(Color(0xFF9A6410), o(cx - k, cy - k * .25f), o(cx + k, cy - k * .25f), strokeWidth = w.width, cap = StrokeCap.Round)
        drawArc(Color(0xFF9A6410), -90f, 180f, false, o(cx - k, cy - k * .95f), sz(k * 1.6f, k * 1.35f), style = w)
        drawLine(Color(0xFF9A6410), o(cx - k * .5f, cy - k * .2f), o(cx + k * .6f, cy + k * 1.3f), strokeWidth = w.width, cap = StrokeCap.Round)
    }

    // ---------- 1. background
    if (!transparentBg) {
        val (a, b) = bgColors(look.bg)
        drawRect(Brush.linearGradient(listOf(a, b), start = o(0f, 0f), end = o(100f, 100f)), Offset.Zero, size)
        drawCircle(Color.White.copy(alpha = .16f), 40f * u, o(50f, 44f))
        // finance motif: rising bars and an arrow behind the character
        for (i in 0 until 4) drawRoundRect(Color.White.copy(alpha = .18f), o(70f + i * 7f, 80f - i * 9f), sz(4.5f, 30f + i * 9f), androidx.compose.ui.geometry.CornerRadius(2f * u))
        val arrow = Path().apply { moveTo(66f * u, 40f * u); quadraticBezierTo(80f * u, 36f * u, 90f * u, 20f * u) }
        drawPath(arrow, Color.White.copy(alpha = .5f), style = Stroke(2f * u, cap = StrokeCap.Round))
        poly(94f, 14f, 83f, 17f, 91f, 25f, c = Color.White.copy(alpha = .55f))
        star(14f, 20f, 5f, .55f + .4f * sin(t * 2f)); star(86f, 52f, 3.4f, .5f + .4f * sin(t * 2.4f + 1f)); star(20f, 70f, 3f, .45f + .4f * sin(t * 1.8f + 2f))
    }

    val bob = sin(t * 1.6f) * .8f
    translate(top = bob * u) {
        val head = o(50f, 46f)
        val long = look.hair in setOf("long", "twin", "pony")

        // ---------- 2. hair behind the head
        when (look.hair) {
            "long" -> { drawPath(Path().apply { moveTo(21f * u, 40f * u); cubicTo(14f * u, 70f * u, 20f * u, 92f * u, 30f * u, 100f * u); lineTo(70f * u, 100f * u); cubicTo(80f * u, 92f * u, 86f * u, 70f * u, 79f * u, 40f * u); close() }, hair)
                drawPath(Path().apply { moveTo(24f * u, 60f * u); cubicTo(22f * u, 78f * u, 26f * u, 90f * u, 32f * u, 98f * u) }, hairLight.copy(alpha = .5f), style = Stroke(2.2f * u, cap = StrokeCap.Round)) }
            "bob" -> drawPath(Path().apply { moveTo(20f * u, 40f * u); cubicTo(14f * u, 62f * u, 20f * u, 76f * u, 30f * u, 76f * u); lineTo(70f * u, 76f * u); cubicTo(80f * u, 76f * u, 86f * u, 62f * u, 80f * u, 40f * u); close() }, hair)
            "twin" -> for (s in listOf(-1f, 1f)) { val x = 50f + s * 40f; drawPath(Path().apply { moveTo((50f + s * 24f) * u, 40f * u); cubicTo((x + s * 8f) * u, 52f * u, (x + s * 6f) * u, 82f * u, (x - s * 4f) * u, 96f * u); cubicTo((x - s * 14f) * u, 80f * u, (50f + s * 22f) * u, 70f * u, (50f + s * 24f) * u, 40f * u); close() }, hair); drawCircle(Color(0xFFFF8FC0), 3.4f * u, o(50f + s * 29f, 44f)) }
            "pony" -> drawPath(Path().apply { moveTo(70f * u, 22f * u); cubicTo(96f * u, 22f * u, 100f * u, 60f * u, 84f * u, 88f * u); cubicTo(80f * u, 66f * u, 74f * u, 44f * u, 70f * u, 22f * u); close() }, hair)
            "bun" -> { drawCircle(hair, 11f * u, o(50f, 10f)); drawCircle(hairLight.copy(alpha = .45f), 4f * u, o(46f, 7f)) }
            "curly" -> for (i in 0 until 9) { val a = PI.toFloat() * (1.05f + i * .11f); drawCircle(hair, 9f * u, o(50f + 31f * cos(a), 46f + 31f * sin(a) * 1.05f)) }
            "messy", "spiky", "side" -> drawCircle(hair, 28f * u, o(50f, 44f))
        }

        // ---------- 3. body and outfit
        val body = Path().apply { moveTo(8f * u, 101f * u); cubicTo(8f * u, 84f * u, 26f * u, 77f * u, 38f * u, 75f * u); lineTo(62f * u, 75f * u); cubicTo(74f * u, 77f * u, 92f * u, 84f * u, 92f * u, 101f * u); close() }
        if (look.outfit == "cape" || look.outfit == "royal") drawPath(Path().apply { moveTo(2f * u, 101f * u); cubicTo(0f * u, 80f * u, 22f * u, 72f * u, 38f * u, 72f * u); lineTo(62f * u, 72f * u); cubicTo(78f * u, 72f * u, 100f * u, 80f * u, 98f * u, 101f * u); close() }, if (look.outfit == "cape") Color(0xFF3B2F8F) else Color(0xFF4A3B1E))
        if (look.outfit == "hoodie") drawPath(Path().apply { moveTo(26f * u, 78f * u); cubicTo(30f * u, 64f * u, 70f * u, 64f * u, 74f * u, 78f * u); lineTo(74f * u, 86f * u); lineTo(26f * u, 86f * u); close() }, outfitDark)
        drawRect(SKIN_SH, o(43f, 62f), sz(14f, 18f))     // neck
        drawPath(body, outfit)
        drawPath(Path().apply { moveTo(8f * u, 101f * u); cubicTo(8f * u, 90f * u, 14f * u, 84f * u, 22f * u, 80f * u) }, outfitLight.copy(alpha = .55f), style = Stroke(2f * u, cap = StrokeCap.Round))
        when (look.outfit) {
            "hoodie" -> { drawPath(Path().apply { moveTo(38f * u, 75f * u); quadraticBezierTo(50f * u, 90f * u, 62f * u, 75f * u) }, SKIN, style = androidx.compose.ui.graphics.drawscope.Fill)
                drawLine(onOutfit.copy(alpha = .8f), o(44f, 82f), o(43f, 94f), strokeWidth = 1.6f * u, cap = StrokeCap.Round); drawLine(onOutfit.copy(alpha = .8f), o(56f, 82f), o(57f, 94f), strokeWidth = 1.6f * u, cap = StrokeCap.Round)
                drawRoundRect(outfitDark, o(34f, 92f), sz(32f, 9f), androidx.compose.ui.geometry.CornerRadius(3f * u)) }
            "sweater" -> { drawPath(Path().apply { moveTo(36f * u, 75f * u); quadraticBezierTo(50f * u, 88f * u, 64f * u, 75f * u) }, SKIN)
                drawArc(outfitDark, 0f, 180f, false, o(36f, 68f), sz(28f, 14f), style = Stroke(3.4f * u)); for (i in 0 until 4) drawLine(outfitDark.copy(alpha = .5f), o(20f + i * 20f, 88f), o(20f + i * 20f, 101f), strokeWidth = 1.2f * u) }
            "suit" -> { drawPath(Path().apply { moveTo(38f * u, 75f * u); lineTo(50f * u, 100f * u); lineTo(62f * u, 75f * u); close() }, Color(0xFFF5F3FF))
                poly(38f, 75f, 50f, 100f, 30f, 101f, 24f, 82f, c = outfitDark); poly(62f, 75f, 50f, 100f, 70f, 101f, 76f, 82f, c = outfitDark)
                poly(48.5f, 78f, 51.5f, 78f, 53f, 98f, 50f, 100f, 47f, 98f, c = Color(0xFFE5484D)) }
            "casual" -> { drawPath(Path().apply { moveTo(38f * u, 75f * u); quadraticBezierTo(50f * u, 85f * u, 62f * u, 75f * u) }, SKIN); drawCircle(onOutfit.copy(alpha = .85f), 4.5f * u, o(50f, 92f)); drawCircle(outfit, 2.6f * u, o(50f, 92f)) }
            "jacket" -> { poly(38f, 75f, 47f, 101f, 8f, 101f, 18f, 84f, c = outfitDark); poly(62f, 75f, 53f, 101f, 92f, 101f, 82f, 84f, c = outfitDark)
                poly(40f, 75f, 60f, 75f, 53f, 101f, 47f, 101f, c = Color(0xFFF5F3FF)); drawLine(Color(0xFFE7A92B), o(50f, 82f), o(50f, 101f), strokeWidth = 1.2f * u) }
            "anime" -> { poly(38f, 75f, 50f, 92f, 62f, 75f, c = Color(0xFFF5F3FF)); poly(24f, 82f, 38f, 75f, 50f, 92f, 36f, 98f, c = outfitDark); poly(76f, 82f, 62f, 75f, 50f, 92f, 64f, 98f, c = outfitDark)
                poly(50f, 91f, 42f, 99f, 50f, 96f, 58f, 99f, c = Color(0xFFFF6F91)); drawCircle(Color(0xFFFF6F91), 2.4f * u, o(50f, 92f)) }
            "winter" -> { drawOval(Color(0xFFFFF5F0), o(28f, 70f), sz(44f, 16f)); drawOval(Color(0xFFFF8FA0), o(34f, 74f), sz(18f, 24f)); for (i in 0 until 3) drawCircle(Color(0xFF8A6A4A), 2f * u, o(50f, 90f + i * 5f)) }
            "royal" -> { poly(38f, 75f, 50f, 98f, 62f, 75f, c = Color(0xFFFFE9A8)); drawLine(Color(0xFFE7A92B), o(50f, 80f), o(50f, 101f), strokeWidth = 1.4f * u); star(30f, 90f, 4f, .95f); star(70f, 90f, 4f, .95f); drawArc(Color(0xFFE7A92B), 0f, 180f, false, o(34f, 68f), sz(32f, 14f), style = Stroke(2.6f * u)) }
            "cape" -> { drawArc(Color(0xFFFFD36B), 0f, 180f, false, o(36f, 68f), sz(28f, 14f), style = Stroke(2.6f * u)); coin(50f, 88f, 5f) }
        }

        // ---------- 4. head
        drawOval(SKIN, o(24f, 17f), sz(52f, 57f))
        drawOval(SKIN_SH.copy(alpha = .5f), o(30f, 60f), sz(40f, 12f))
        for (s in listOf(-1f, 1f)) drawOval(SKIN, o(50f + s * 26.5f - 3f, 48f), sz(6f, 9f))
        val male = !ch.female

        // ---------- 5. eyes and expression
        val shut = blink
        for (s in listOf(-1f, 1f)) {
            val ex = 50f + s * 12.5f; val ey = 51f
            val closed = shut || look.expression == "smile" || look.expression == "joy" || (look.expression == "wink" && s > 0)
            if (closed) {
                drawArc(INK, 200f, 140f, false, o(ex - 6.5f, ey - 5.5f), sz(13f, 11f), style = Stroke(1.9f * u, cap = StrokeCap.Round))
                if (look.expression == "smile" || look.expression == "joy") drawArc(INK, 20f, 140f, false, o(ex - 6.5f, ey - 5.5f), sz(13f, 11f), style = Stroke(.01f))
            } else {
                val rx = if (male) 5.3f else 6.2f; val ry = if (male) 6.4f else 7.6f
                drawOval(Color.White, o(ex - rx, ey - ry), sz(rx * 2f, ry * 2f))
                drawOval(Color(0xFF7A55E8), o(ex - rx * .82f, ey - ry * .86f), sz(rx * 1.64f, ry * 1.72f))
                drawOval(Color(0xFF4B2CB0), o(ex - rx * .5f, ey - ry * .4f), sz(rx * 1.0f, ry * 1.05f))
                drawOval(INK, o(ex - rx * .36f, ey - ry * .26f), sz(rx * .72f, ry * .75f))
                drawCircle(Color.White, 1.9f * u, o(ex - rx * .3f, ey - ry * .55f)); drawCircle(Color.White.copy(alpha = .85f), .9f * u, o(ex + rx * .3f, ey + ry * .3f))
                drawArc(INK, 195f, 150f, false, o(ex - rx - .6f, ey - ry - 1.4f), sz(rx * 2f + 1.2f, ry * 1.5f), style = Stroke(2.3f * u, cap = StrokeCap.Round))
            }
            val brow = if (look.expression == "worried") -s * 2f else 0f
            drawLine(hairDark, o(ex - 6f, ey - 10.5f + brow), o(ex + 6f, ey - 11.5f - brow), strokeWidth = (if (male) 1.9f else 1.3f) * u, cap = StrokeCap.Round)
        }
        drawOval(Color(0xFFFF9FB5).copy(alpha = .45f), o(27f, 58f), sz(10f, 6f)); drawOval(Color(0xFFFF9FB5).copy(alpha = .45f), o(63f, 58f), sz(10f, 6f))
        drawCircle(SKIN_SH, .9f * u, o(50f, 58f))
        val m = o(50f, 65f)
        when (look.expression) {
            "happy", "wink" -> { drawArc(Color(0xFF8E3248), 0f, 180f, true, m + Offset(-5f * u, -3f * u), sz(10f, 8f)); drawArc(Color(0xFFFF8FA8), 20f, 140f, true, m + Offset(-3f * u, 1.4f * u), sz(6f, 3f)) }
            "joy" -> { drawArc(Color(0xFF8E3248), 0f, 180f, true, m + Offset(-6f * u, -3.5f * u), sz(12f, 10f)); drawArc(Color(0xFFFF8FA8), 20f, 140f, true, m + Offset(-3.6f * u, 1.8f * u), sz(7.2f, 3.6f)) }
            "worried" -> drawArc(Color(0xFFB5465E), 200f, 140f, false, m + Offset(-4f * u, -1f * u), sz(8f, 5f), style = Stroke(1.5f * u, cap = StrokeCap.Round))
            else -> drawArc(Color(0xFFC95E78), 15f, 150f, false, m + Offset(-4f * u, -4.5f * u), sz(8f, 5.5f), style = Stroke(1.6f * u, cap = StrokeCap.Round))
        }

        // ---------- 6. front hair
        val fringe = Path()
        when (look.hair) {
            "messy" -> { fringe.moveTo(21f * u, 48f * u); fringe.cubicTo(16f * u, 24f * u, 30f * u, 10f * u, 50f * u, 12f * u); fringe.cubicTo(70f * u, 10f * u, 86f * u, 24f * u, 79f * u, 48f * u)
                fringe.lineTo(73f * u, 36f * u); fringe.lineTo(66f * u, 46f * u); fringe.lineTo(58f * u, 33f * u); fringe.lineTo(50f * u, 45f * u); fringe.lineTo(42f * u, 33f * u); fringe.lineTo(34f * u, 46f * u); fringe.lineTo(27f * u, 36f * u); fringe.close()
                for (k in 0 until 4) poly(26f + k * 16f, 16f, 30f + k * 16f, 2f, 36f + k * 16f, 15f, c = hair) }
            "spiky" -> { fringe.moveTo(21f * u, 46f * u); fringe.cubicTo(18f * u, 20f * u, 32f * u, 10f * u, 50f * u, 11f * u); fringe.cubicTo(68f * u, 10f * u, 82f * u, 20f * u, 79f * u, 46f * u); fringe.lineTo(70f * u, 34f * u); fringe.lineTo(62f * u, 44f * u); fringe.lineTo(54f * u, 30f * u); fringe.lineTo(46f * u, 44f * u); fringe.lineTo(36f * u, 32f * u); fringe.lineTo(30f * u, 44f * u); fringe.close()
                for (k in 0 until 5) poly(22f + k * 14f, 18f, 28f + k * 14f, -3f - (k % 2) * 3f, 34f + k * 14f, 14f, c = hair) }
            "side" -> { fringe.moveTo(21f * u, 48f * u); fringe.cubicTo(16f * u, 22f * u, 32f * u, 10f * u, 52f * u, 11f * u); fringe.cubicTo(72f * u, 12f * u, 84f * u, 26f * u, 79f * u, 48f * u); fringe.cubicTo(74f * u, 38f * u, 62f * u, 30f * u, 44f * u, 36f * u); fringe.cubicTo(38f * u, 40f * u, 30f * u, 44f * u, 26f * u, 56f * u); fringe.close() }
            "curly" -> { fringe.moveTo(21f * u, 44f * u); fringe.cubicTo(18f * u, 22f * u, 32f * u, 12f * u, 50f * u, 13f * u); fringe.cubicTo(68f * u, 12f * u, 82f * u, 22f * u, 79f * u, 44f * u); fringe.cubicTo(70f * u, 30f * u, 60f * u, 34f * u, 50f * u, 36f * u); fringe.cubicTo(40f * u, 34f * u, 30f * u, 30f * u, 21f * u, 44f * u); fringe.close()
                for (k in 0 until 5) drawCircle(hair, 6f * u, o(28f + k * 11f, 20f + abs(k - 2) * 2f)) }
            else -> { fringe.moveTo(20f * u, 52f * u); fringe.cubicTo(14f * u, 22f * u, 32f * u, 9f * u, 50f * u, 10f * u); fringe.cubicTo(68f * u, 9f * u, 86f * u, 22f * u, 80f * u, 52f * u)
                fringe.cubicTo(78f * u, 40f * u, 72f * u, 34f * u, 62f * u, 31f * u); fringe.cubicTo(58f * u, 38f * u, 54f * u, 42f * u, 50f * u, 36f * u); fringe.cubicTo(46f * u, 42f * u, 42f * u, 38f * u, 38f * u, 31f * u); fringe.cubicTo(28f * u, 34f * u, 22f * u, 40f * u, 20f * u, 52f * u); fringe.close() }
        }
        drawPath(fringe, hair)
        drawPath(Path().apply { moveTo(30f * u, 24f * u); quadraticBezierTo(46f * u, 12f * u, 62f * u, 17f * u) }, hairLight.copy(alpha = .6f), style = Stroke(2.6f * u, cap = StrokeCap.Round))
        if (long || look.hair == "bob") for (s in listOf(-1f, 1f)) drawPath(Path().apply { moveTo((50f + s * 29f) * u, 40f * u); cubicTo((50f + s * 32f) * u, 62f * u, (50f + s * 30f) * u, 78f * u, (50f + s * 34f) * u, 94f * u); lineTo((50f + s * 24f) * u, 94f * u); cubicTo((50f + s * 22f) * u, 74f * u, (50f + s * 22f) * u, 60f * u, (50f + s * 25f) * u, 44f * u); close() }, hair)

        // ---------- 7. accessories
        val acc = look.accessories
        if ("catEars" in acc) for (s in listOf(-1f, 1f)) { poly(50f + s * 12f, 18f, 50f + s * 30f, 14f, 50f + s * 26f, -2f, c = hairDark); poly(50f + s * 17f, 15f, 50f + s * 26f, 13f, 50f + s * 24f, 4f, c = Color(0xFFFF9EC7)) }
        if ("beanie" in acc) { drawArc(outfitDark, 180f, 180f, true, o(22f, 2f), sz(56f, 40f)); drawRoundRect(outfit, o(21f, 23f), sz(58f, 8f), androidx.compose.ui.geometry.CornerRadius(4f * u)); drawCircle(outfitLight, 4.5f * u, o(50f, 3f)) }
        if ("cap" in acc) { drawArc(outfit, 180f, 180f, true, o(23f, 4f), sz(54f, 36f)); drawRoundRect(outfitDark, o(48f, 24f), sz(34f, 5f), androidx.compose.ui.geometry.CornerRadius(2.5f * u)); drawCircle(onOutfit.copy(alpha = .8f), 2.2f * u, o(50f, 14f)) }
        if ("bow" in acc) { val c = o(66f, 22f); poly(66f, 22f, 56f, 15f, 56f, 29f, c = Color(0xFFFF8FC0)); poly(66f, 22f, 76f, 15f, 76f, 29f, c = Color(0xFFFF8FC0)); drawCircle(Color(0xFFE5609F), 3f * u, c) }
        if ("starClip" in acc) { rotate(15f, pivot = o(34f, 26f)) { star(34f, 26f, 6.5f, 1f); drawCircle(Color(0xFFFFD36B), 1.6f * u, o(34f, 26f)) } }
        if ("clipArrow" in acc) { rotate(-35f, pivot = o(34f, 26f)) { drawRoundRect(Color(0xFFB9A7FF), o(27f, 24f), sz(14f, 4.6f), androidx.compose.ui.geometry.CornerRadius(2.3f * u)); poly(41f, 20.5f, 49f, 26.3f, 41f, 32f, c = Color(0xFFB9A7FF)) } }
        if ("headphones" in acc) {
            drawArc(Color(0xFFC9BDFF), 190f, 160f, false, o(20f, 6f), sz(60f, 62f), style = Stroke(5.2f * u, cap = StrokeCap.Round))
            for (s in listOf(-1f, 1f)) { drawRoundRect(Color(0xFFB9A7FF), o(50f + s * 30.5f - 6.5f, 42f), sz(13f, 22f), androidx.compose.ui.geometry.CornerRadius(6f * u)); drawCircle(Color(0xFFE9E3FF), 4.2f * u, o(50f + s * 30.5f, 53f)); poly(50f + s * 30.5f - 2f, 55f, 50f + s * 30.5f + 2f, 55f, 50f + s * 30.5f, 50.5f, c = Color(0xFF7C5CFF)) }
        }
        if ("glasses" in acc) {
            for (s in listOf(-1f, 1f)) { drawCircle(Color.White.copy(alpha = .16f), 10.4f * u, o(50f + s * 12.5f, 51f)); drawCircle(INK, 10.4f * u, o(50f + s * 12.5f, 51f), style = Stroke(1.5f * u)) }
            drawLine(INK, o(46.5f, 50f), o(53.5f, 50f), strokeWidth = 1.4f * u); for (s in listOf(-1f, 1f)) drawLine(INK, o(50f + s * 22.5f, 50f), o(50f + s * 27f, 48f), strokeWidth = 1.3f * u)
        }
        if ("chart" in acc) { drawCircle(Color.White, 6.4f * u, o(72f, 86f)); drawCircle(Color(0xFF7C5CFF), 6.4f * u, o(72f, 86f), style = Stroke(1.2f * u)); for (i in 0 until 3) drawRect(Color(0xFF7C5CFF), o(68.4f + i * 2.6f, 89.2f - (i + 1) * 2.2f), sz(1.8f, (i + 1) * 2.2f)) }
        if ("coin" in acc) coin(86f, 87f, 9f)
    }
}

/** The mascot as a view. [animated] adds a gentle bob, blinking and twinkling stars; turn it off for lists to save battery. */
@Composable
fun MascotView(look: MascotLook, size: Dp, modifier: Modifier = Modifier, animated: Boolean = true, rounded: Boolean = true, expression: String? = null, transparentBg: Boolean = false, clipCircle: Boolean = false) {
    val shown = remember(look, expression) { if (expression != null) look.copy(expression = expression) else look }
    val clip = if (clipCircle) Modifier.clip(androidx.compose.foundation.shape.CircleShape) else if (rounded) Modifier.clip(RoundedCornerShape(size * .22f)) else Modifier
    if (!animated) { Canvas(modifier.then(clip).androidx_size(size)) { drawMascot(shown, 0f, false, transparentBg) }; return }
    val tr = rememberInfiniteTransition(label = "mascot")
    val t by tr.animateFloat(0f, 20f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val blink = (t % 4.2f) > 4.05f
    Canvas(modifier.then(clip).androidx_size(size)) { drawMascot(shown, t, blink, transparentBg) }
}

private fun Modifier.androidx_size(s: Dp) = this.then(Modifier.size(s))
