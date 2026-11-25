package com.fincip.nativeapp.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val SKIN = Color(0xFFFFE3D3)
private val SKIN_SHADE = Color(0xFFF3C8B6)
private val HAIR = Color(0xFF4A2A22)
private val HAIR_MID = Color(0xFF6B3B2E)
private val HAIR_HI = Color(0xFF8E5646)
private val PINK = Color(0xFFF7A8C4)
private val PINK_DEEP = Color(0xFFEC7FA5)
private val IRIS = Color(0xFF6A3A32)
private val INK = Color(0xFF2A1512)

/**
 * Draws FinKip Girl as layered vector shapes on a posed skeleton: hips, two legs, torso, two arms, head.
 * [u] is pixels per logical unit, (cx, groundY) is the point between her feet. Everything inside is in units.
 */
fun DrawScope.drawGirl(look: Look, pose: Pose, cx: Float, groundY: Float, u: Float, facing: Int, t: Float, blink: Boolean) {
    val top = Color(look.top.toInt()); val bottom = Color(look.bottom.toInt()); val sock = Color(look.sock.toInt())
    val shoe = Color(look.shoe.toInt()); val accent = Color(look.accent.toInt())
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun dir(a: Float) = Offset(sin(a * PI.toFloat() / 180f), cos(a * PI.toFloat() / 180f))
    fun line(a: Offset, b: Offset, c: Color, w: Float) = drawLine(c, a, b, strokeWidth = w * u, cap = StrokeCap.Round)
    fun heart(c: Offset, r: Float, col: Color) {
        drawCircle(col, r * u * .55f, c + o(-r * .5f, -r * .2f)); drawCircle(col, r * u * .55f, c + o(r * .5f, -r * .2f))
        drawPath(Path().apply { moveTo(c.x - r * u * 1.02f, c.y - r * u * .05f); lineTo(c.x, c.y + r * u * 1.05f); lineTo(c.x + r * u * 1.02f, c.y - r * u * .05f); close() }, col)
    }
    fun bow(c: Offset, w: Float, col: Color, deep: Color) {
        drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x - w * u, c.y - w * .62f * u); lineTo(c.x - w * u, c.y + w * .62f * u); close() }, col)
        drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x + w * u, c.y - w * .62f * u); lineTo(c.x + w * u, c.y + w * .62f * u); close() }, col)
        drawCircle(deep, w * .3f * u, c)
    }

    // soft contact shadow stays on the floor
    drawOval(Color.Black.copy(alpha = .12f), Offset(cx - 46f * u, groundY - 7f * u), Size(92f * u, 14f * u))

    scale(scaleX = if (facing < 0) -1f else 1f, scaleY = 1f, pivot = Offset(cx, groundY)) {
        translate(left = cx, top = groundY) {
            val hip = o(0f, -(pose.lift + pose.bob))
            rotate(pose.bodyRot, pivot = hip) {
                // ---------- legs
                val pants = look.bottomKind == "pants"
                val legCol = if (pants) bottom else SKIN
                fun leg(side: Float, a: Float, b: Float) {
                    val hp = hip + o(side * 13f, 0f)
                    val knee = hp + dir(a) * (56f * u)
                    val foot = knee + dir(a + b) * (54f * u)
                    line(hp, knee, legCol, 17f); line(knee, foot, legCol, 15f)
                    if (!pants) { val sockTop = foot - dir(a + b) * (24f * u); line(sockTop, foot, sock, 15.5f) }
                    else { val cuff = foot - dir(a + b) * (6f * u); line(cuff, foot, if (look.id == "sporty") Color.White else bottom.copy(alpha = .8f), 17f) }
                    val toe = foot + Offset(7f * u, 4f * u)
                    drawOval(shoe, foot + Offset(-10f * u, -2f * u), Size(26f * u, 13f * u))
                    drawOval(Color.White.copy(alpha = .85f), foot + Offset(-10f * u, 7f * u), Size(26f * u, 4f * u))
                    drawCircle(Color.White.copy(alpha = .7f), 2f * u, toe)
                }
                leg(-1f, pose.legL, pose.legLb); leg(1f, pose.legR, pose.legRb)

                rotate(pose.lean, pivot = hip) {
                    val sc = hip + o(0f, -92f)
                    val neck = sc + o(0f, -6f)
                    val headC = neck + o(0f, -50f)
                    val sway = sin(t * 1.3f)

                    // ---------- hair behind (volume + wavy curtains)
                    drawCircle(HAIR, 62f * u, headC + o(0f, 2f))
                    for (side in listOf(-1f, 1f)) {
                        val p = Path()
                        val n = 12
                        p.moveTo(headC.x + side * 30f * u, headC.y - 10f * u)
                        for (k in 0..n) {
                            val y = headC.y + (k * 13f - 10f) * u
                            val x = headC.x + side * (58f + 9f * sin(k * 0.95f + t * 1.1f + side) + k * 0.6f + sway * k * .4f) * u
                            p.lineTo(x, y)
                        }
                        for (k in n downTo 0) {
                            val y = headC.y + (k * 13f - 10f) * u
                            p.lineTo(headC.x + side * 20f * u, y)
                        }
                        p.close()
                        drawPath(p, HAIR)
                        // a lighter strand so the waves read
                        val s = Path(); s.moveTo(headC.x + side * 46f * u, headC.y + 10f * u)
                        for (k in 1..n) s.lineTo(headC.x + side * (46f + 7f * sin(k * 0.95f + t * 1.1f + side) + k * 0.5f) * u, headC.y + (k * 13f) * u)
                        drawPath(s, HAIR_HI.copy(alpha = .6f), style = Stroke(width = 4f * u, cap = StrokeCap.Round))
                        drawCircle(HAIR, 15f * u, Offset(headC.x + side * (66f + sway * 3f) * u, headC.y + (n * 13f - 4f) * u))
                    }

                    // ---------- far arm
                    val shL = sc + o(-25f, 2f)
                    val shR = sc + o(25f, 2f)
                    val long = look.longSleeves
                    val sleeveW = if (look.topKind == "sweater") 21f else if (look.topKind == "coat") 20f else 14f
                    fun arm(sh: Offset, a: Float, b: Float): Offset {
                        val elbow = sh + dir(a) * (50f * u)
                        val hand = elbow + dir(a + b) * (44f * u)
                        line(elbow, hand, if (long) top else SKIN, if (long) sleeveW - 2f else 12f)
                        line(sh, elbow, top, sleeveW)
                        drawCircle(SKIN, 7f * u, hand)
                        if (long && look.topKind == "sweater") drawCircle(Color.White.copy(alpha = .55f), 9f * u, elbow + (hand - elbow) * .78f)
                        return hand
                    }
                    arm(shL, pose.armL, pose.armLb)

                    // ---------- torso
                    val torso = Path().apply {
                        moveTo(hip.x - 26f * u, sc.y); lineTo(hip.x + 26f * u, sc.y)
                        lineTo(hip.x + 19f * u, hip.y - 20f * u); lineTo(hip.x + 22f * u, hip.y + 6f * u)
                        lineTo(hip.x - 22f * u, hip.y + 6f * u); lineTo(hip.x - 19f * u, hip.y - 20f * u); close()
                    }
                    drawPath(torso, SKIN)
                    drawRect(SKIN, hip + o(-6f, -98f), Size(12f * u, 14f * u))

                    // ---------- bottoms (skirts / pleats / dress)
                    fun tier(yTop: Float, half0: Float, half1: Float, h: Float, col: Color, scallops: Int) {
                        drawPath(Path().apply { moveTo(hip.x - half0 * u, hip.y + yTop * u); lineTo(hip.x + half0 * u, hip.y + yTop * u); lineTo(hip.x + half1 * u, hip.y + (yTop + h) * u); lineTo(hip.x - half1 * u, hip.y + (yTop + h) * u); close() }, col)
                        val step = 2f * half1 / scallops
                        for (i in 0 until scallops) drawCircle(col, step * u * .55f, Offset(hip.x + (-half1 + step * (i + .5f)) * u, hip.y + (yTop + h) * u))
                    }
                    val shade = Color(0xFFFFE3EC)
                    when (look.bottomKind) {
                        "ruffle" -> { tier(-4f, 24f, 40f, 34f, shade, 6); tier(10f, 28f, 48f, 34f, bottom, 7); tier(22f, 32f, 54f, 32f, shade, 8) }
                        "dressSkirt" -> { tier(-4f, 24f, 40f, 34f, bottom, 6); tier(10f, 28f, 48f, 34f, Color(0xFFF4A9C6), 7); tier(22f, 32f, 54f, 32f, bottom, 8) }
                        "plaid" -> {
                            tier(-2f, 24f, 38f, 38f, bottom, 0)
                            for (i in -3..3) drawLine(Color.White.copy(alpha = .16f), Offset(hip.x + i * 9f * u, hip.y), Offset(hip.x + i * 12f * u, hip.y + 36f * u), strokeWidth = 1.6f * u)
                            drawLine(Color(0xFFB5202E).copy(alpha = .35f), Offset(hip.x - 34f * u, hip.y + 16f * u), Offset(hip.x + 34f * u, hip.y + 16f * u), strokeWidth = 2f * u)
                        }
                        "pants" -> drawRect(bottom, hip + o(-22f, -8f), Size(44f * u, 22f * u))
                    }

                    // ---------- top
                    val chestBottom = when (look.topKind) { "sweater", "crop" -> -42f; "pajama", "shirt" -> -6f; "coat" -> 30f; else -> -34f }
                    val topPath = Path().apply {
                        moveTo(hip.x - 27f * u, sc.y - 1f * u); lineTo(hip.x + 27f * u, sc.y - 1f * u)
                        val w = if (look.topKind == "coat") 33f else if (look.topKind == "dress") 22f else 22f
                        lineTo(hip.x + w * u, hip.y + chestBottom * u); lineTo(hip.x - w * u, hip.y + chestBottom * u); close()
                    }
                    drawPath(topPath, top)
                    when (look.topKind) {
                        "sweater" -> { bow(hip + o(0f, -62f), 17f, Color.White, Color(0xFFF1F1F1)); drawArc(Color.White.copy(alpha = .8f), 200f, 140f, false, hip + o(-14f, -97f), Size(28f * u, 22f * u), style = Stroke(3f * u)) }
                        "pajama" -> for (i in 0 until 7) drawCircle(Color.White.copy(alpha = .75f), 2.6f * u, hip + o(-14f + (i % 4) * 9f, -80f + (i / 4) * 26f + (i % 2) * 8f))
                        "shirt" -> { drawPath(Path().apply { moveTo(hip.x - 10f * u, sc.y); lineTo(hip.x, sc.y + 16f * u); lineTo(hip.x - 22f * u, sc.y + 10f * u); close() }, Color(0xFFF1EDF0)); drawPath(Path().apply { moveTo(hip.x + 10f * u, sc.y); lineTo(hip.x, sc.y + 16f * u); lineTo(hip.x + 22f * u, sc.y + 10f * u); close() }, Color(0xFFF1EDF0)); bow(hip + o(0f, -72f), 13f, accent, Color(0xFF8E1620)) }
                        "crop" -> { drawRect(Color.White, hip + o(-22f, -66f), Size(44f * u, 5f * u)); drawLine(Color.White, hip + o(-22f, -44f), hip + o(22f, -44f), strokeWidth = 3f * u) }
                        "coat" -> { for (i in 0 until 3) drawCircle(Color(0xFF8A6A4A), 3f * u, hip + o(0f, -62f + i * 28f)); drawLine(Color(0xFFC9AE88), hip + o(0f, -92f), hip + o(0f, 30f), strokeWidth = 2f * u) }
                        "dress" -> bow(hip + o(0f, -34f), 14f, Color(0xFFF08FB1), PINK_DEEP)
                    }

                    // ---------- near arm
                    val handR = arm(shR, pose.armR, pose.armRb)

                    // ---------- props held in hand
                    when (pose.prop) {
                        "book" -> { drawRoundRectSimple(handR + o(-20f, -34f), Size(40f * u, 30f * u), Color(0xFFFFFFFF), 5f * u); drawLine(Color(0xFFE9A3BD), handR + o(0f, -34f), handR + o(0f, -4f), strokeWidth = 2f * u); drawRect(PINK, handR + o(-20f, -6f), Size(40f * u, 4f * u)) }
                        "phone" -> drawRoundRectSimple(handR + o(-7f, -22f), Size(14f * u, 24f * u), Color(0xFFE3D4F7), 3f * u)
                        "cup" -> { drawRoundRectSimple(handR + o(-8f, -16f), Size(16f * u, 18f * u), Color(0xFFFFFFFF), 4f * u); drawRect(PINK, handR + o(-8f, -9f), Size(16f * u, 5f * u)) }
                        "brush" -> line(handR, handR + o(10f, -22f), Color(0xFF7FD3E0), 4f)
                        "spoon" -> { line(handR, handR + o(14f, -24f), Color(0xFFC9B9A8), 3f); drawCircle(Color(0xFFC9B9A8), 5f * u, handR + o(14f, -24f)) }
                        "pen" -> line(handR, handR + o(8f, 10f), Color(0xFFF08FB1), 3f)
                    }
                    if (look.id == "default") { // bunny bag
                        val bp = hip + o(-30f, -8f)
                        line(sc + o(25f, 0f), bp, Color(0xFFF2B5C8), 3f)
                        drawCircle(Color.White, 11f * u, bp); drawOval(Color.White, bp + o(-9f, -26f), Size(7f * u, 17f * u)); drawOval(Color.White, bp + o(2f, -26f), Size(7f * u, 17f * u))
                        drawCircle(INK, 1.4f * u, bp + o(-4f, -2f)); drawCircle(INK, 1.4f * u, bp + o(4f, -2f)); drawCircle(PINK, 2f * u, bp + o(0f, 2f))
                    }
                    if (look.extra == "scarf") drawOval(Color(0xFFFFF5F0), neck + o(-24f, -8f), Size(48f * u, 18f * u))
                    if (look.topKind == "coat") drawOval(accent, neck + o(-24f, -8f), Size(48f * u, 18f * u))

                    // ---------- head
                    rotate(pose.headTilt, pivot = neck) {
                        val closed = pose.eyesClosed || blink
                        drawOval(SKIN, headC + o(-46f, -47f), Size(92f * u, 96f * u))
                        drawOval(Color(0xFFFFB7B7).copy(alpha = .5f), headC + o(-37f, 12f), Size(18f * u, 11f * u))
                        drawOval(Color(0xFFFFB7B7).copy(alpha = .5f), headC + o(19f, 12f), Size(18f * u, 11f * u))
                        // eyes
                        for (side in listOf(-1f, 1f)) {
                            val ec = headC + o(side * 18f, 4f)
                            val winkShut = pose.face == "wink" && side > 0
                            if (closed || winkShut) {
                                drawArc(INK, 10f, 160f, false, ec + o(-11f, -9f), Size(22f * u, 16f * u), style = Stroke(3.2f * u, cap = StrokeCap.Round))
                            } else {
                                val surprised = pose.face == "surprise"
                                drawOval(Color.White, ec + o(-12f, -15f), Size(24f * u, (if (surprised) 34f else 30f) * u))
                                drawOval(IRIS, ec + o(-10.5f, -13f), Size(21f * u, (if (surprised) 31f else 27f) * u))
                                drawOval(INK, ec + o(-6f, -8f), Size(12f * u, 17f * u))
                                drawCircle(Color.White, 3.8f * u, ec + o(-4f, -8f)); drawCircle(Color.White.copy(alpha = .85f), 1.9f * u, ec + o(4f, 4f))
                                drawArc(INK, 195f, 150f, false, ec + o(-13f, -17f), Size(26f * u, 20f * u), style = Stroke(3.4f * u, cap = StrokeCap.Round))
                            }
                            val brow = if (pose.face == "sad") side * -4f else if (pose.face == "think") side * 2f else 0f
                            drawLine(HAIR_MID, ec + o(-9f, -22f + brow), ec + o(9f, -23f - brow), strokeWidth = 2.4f * u, cap = StrokeCap.Round)
                        }
                        drawCircle(SKIN_SHADE, 1.6f * u, headC + o(0f, 16f))
                        // mouth
                        val m = headC + o(0f, 30f)
                        when (pose.face) {
                            "happy", "laugh" -> { drawArc(Color(0xFFB5465E), 0f, 180f, true, m + o(-10f, -7f), Size(20f * u, (if (pose.face == "laugh") 18f else 13f) * u)); drawArc(Color(0xFFFF8FA8), 20f, 140f, true, m + o(-6f, -1f), Size(12f * u, 6f * u)) }
                            "talk" -> drawOval(Color(0xFFB5465E), m + o(-5f, -5f), Size(10f * u, (5f + 5f * abs(sin(t * 10f))) * u))
                            "sad" -> drawArc(Color(0xFFB5465E), 200f, 140f, false, m + o(-9f, -2f), Size(18f * u, 12f * u), style = Stroke(2.8f * u, cap = StrokeCap.Round))
                            "surprise" -> drawOval(Color(0xFFB5465E), m + o(-5f, -6f), Size(10f * u, 13f * u))
                            "sleep" -> drawLine(Color(0xFFB5465E), m + o(-5f, 0f), m + o(5f, 0f), strokeWidth = 2.4f * u, cap = StrokeCap.Round)
                            else -> drawArc(Color(0xFFD5607A), 15f, 150f, false, m + o(-9f, -9f), Size(18f * u, 12f * u), style = Stroke(2.8f * u, cap = StrokeCap.Round))
                        }
                        if (pose.face == "sad") for (i in 0..1) { val f = ((t * 1.4f + i * .5f) % 1f); drawCircle(Color(0xFF8CC7FF).copy(alpha = 1f - f), 3.2f * u, headC + o(-24f + i * 48f, 16f + f * 36f)) }

                        // ---------- front hair: curtain bangs, side locks, crown
                        drawArc(HAIR, 180f, 180f, true, headC + o(-52f, -58f), Size(104f * u, 82f * u))
                        drawPath(Path().apply { moveTo(headC.x - 50f * u, headC.y - 8f * u); quadraticBezierTo(headC.x - 46f * u, headC.y - 56f * u, headC.x + 2f * u, headC.y - 54f * u); quadraticBezierTo(headC.x - 4f * u, headC.y - 26f * u, headC.x - 24f * u, headC.y - 6f * u); quadraticBezierTo(headC.x - 36f * u, headC.y + 4f * u, headC.x - 50f * u, headC.y - 8f * u); close() }, HAIR)
                        drawPath(Path().apply { moveTo(headC.x + 50f * u, headC.y - 8f * u); quadraticBezierTo(headC.x + 46f * u, headC.y - 56f * u, headC.x - 2f * u, headC.y - 54f * u); quadraticBezierTo(headC.x + 6f * u, headC.y - 28f * u, headC.x + 24f * u, headC.y - 8f * u); quadraticBezierTo(headC.x + 36f * u, headC.y + 4f * u, headC.x + 50f * u, headC.y - 8f * u); close() }, HAIR)
                        drawPath(Path().apply { moveTo(headC.x - 40f * u, headC.y - 36f * u); quadraticBezierTo(headC.x - 10f * u, headC.y - 56f * u, headC.x + 16f * u, headC.y - 48f * u) }, HAIR_HI.copy(alpha = .7f), style = Stroke(3.2f * u, cap = StrokeCap.Round))
                        for (side in listOf(-1f, 1f)) {
                            val lock = Path(); lock.moveTo(headC.x + side * 47f * u, headC.y - 6f * u)
                            for (k in 1..5) lock.lineTo(headC.x + side * (48f - k * 1.2f + 4f * sin(k * 1.1f + t + side)) * u, headC.y + (k * 11f) * u)
                            drawPath(lock, HAIR, style = Stroke(11f * u, cap = StrokeCap.Round))
                        }
                        // accessories
                        bow(headC + o(-34f, -40f), 13f, PINK, PINK_DEEP)
                        heart(headC + o(-44f, -18f), 7f, Color(0xFFF59AB8)); heart(headC + o(40f, -30f), 6f, Color(0xFFF59AB8))
                        if (look.extra == "headband") drawArc(Color(0xFFF08BB0), 200f, 140f, false, headC + o(-50f, -56f), Size(100f * u, 70f * u), style = Stroke(6f * u, cap = StrokeCap.Round))
                        if (look.extra == "beret") { drawOval(Color(0xFFF1E1C8), headC + o(-44f, -66f), Size(88f * u, 34f * u)); drawCircle(Color(0xFFD7BE9A), 4f * u, headC + o(0f, -66f)) }
                    }
                }
            }
            // z z z while asleep
            if (pose.face == "sleep") for (i in 0..2) {
                val f = ((t * .5f + i / 3f) % 1f)
                val zp = hip + o(-150f - f * 20f, -(pose.lift) - 20f - f * 80f)
                val sz = (7f + i * 3f) * u
                drawPath(Path().apply { moveTo(zp.x, zp.y); lineTo(zp.x + sz, zp.y); lineTo(zp.x, zp.y + sz); lineTo(zp.x + sz, zp.y + sz) }, Color(0xFF7C6BC4).copy(alpha = 1f - f), style = Stroke(2.6f * u, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.drawRoundRectSimple(topLeft: Offset, size: Size, color: Color, r: Float) {
    drawRoundRect(color, topLeft, size, androidx.compose.ui.geometry.CornerRadius(r, r))
}
