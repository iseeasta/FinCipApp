package com.fincip.nativeapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Every choice a user can make for their companion. Indices point into the option lists below. */
data class PetLook(
    val girl: Boolean = true,
    val skin: Int = 1,
    val hairStyle: Int = 0,
    val hairColor: Int = 1,
    val outfitStyle: Int = 0,
    val outfitColor: Int = 0,
    val accessory: Int = 0,
    val background: Int = 0,
    val eyes: Int = 0,
    val blush: Boolean = true
)

object PetOptions {
    val skins = listOf(0xFFF9D9C0, 0xFFF1C4A0, 0xFFD9A27A, 0xFFB87850, 0xFF8A5A3A, 0xFF5E3B26).map { Color(it) }
    val hairColors = listOf(0xFF2B1B14, 0xFF5A3825, 0xFFA0522D, 0xFFE0B050, 0xFFC0392B, 0xFF6B4BA8, 0xFF2E86AB, 0xFFE86AA6, 0xFF3FA66B, 0xFFE8E8E4).map { Color(it) }
    val outfitColors = listOf(0xFFF26D85, 0xFFF7B733, 0xFF2E8C7F, 0xFF9B8AE0, 0xFF5BB8D9, 0xFF9BDE3A, 0xFFF8873B, 0xFF2B2B2B, 0xFFF4F4F0).map { Color(it) }
    val backgrounds = listOf(0xFFDDEFD6, 0xFFFAD9BC, 0xFFD9D0F5, 0xFFCFE8F5, 0xFFFBEFB5, 0xFFF9D3DC, 0xFF26283A, 0xFFE8E8E4).map { Color(it) }
    val hairStyles = listOf("Bun", "Long", "Bob", "Ponytail", "Pigtails", "Short", "Spiky", "Curly")
    val outfitStyles = listOf("T-shirt", "Hoodie", "Jacket", "Collar", "Stripes")
    val eyeStyles = listOf("Round", "Sparkle", "Sleepy", "Wink")
    /** Accessories and the pet level at which each unlocks. */
    val accessories = listOf("None" to 1, "Round glasses" to 1, "Cap" to 1, "Headband" to 1, "Flower" to 2, "Headphones" to 3, "Party hat" to 5, "Crown" to 10)
}

/** Growth stage from level: 0 Sprout, 1 Kid, 2 Teen, 3 Star. */
fun petStage(level: Int): Int = when { level >= 10 -> 3; level >= 6 -> 2; level >= 3 -> 1; else -> 0 }
val PetStageNames = listOf("Sprout", "Kid", "Teen", "Star")
/** Level at which the next stage begins, or null at the last stage. */
fun nextStageLevel(level: Int): Int? = when { level < 3 -> 3; level < 6 -> 6; level < 10 -> 10; else -> null }

/**
 * A hand-drawn character companion. It grows with [level] (bigger body, more poise, sparkle aura at the top stage),
 * reacts to [mood], blinks and breathes. Everything is drawn on a Canvas, so it scales cleanly.
 */
@Composable
fun PetCharacter(look: PetLook, mood: PetMood, level: Int, modifier: Modifier = Modifier, diameter: Dp = 140.dp, showBackground: Boolean = true) {
    val t = rememberInfiniteTransition(label = "pet")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val blink by t.animateFloat(0f, 1f, infiniteRepeatable(keyframes<Float> { durationMillis = 3600; 0f at 0; 0f at 3300; 1f at 3420; 0f at 3540 }), label = "blink")
    val spin by t.animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(6000)), label = "spin")
    val stage = petStage(level)
    Canvas(modifier.size(diameter)) {
        val s = size.minDimension
        if (showBackground) drawRoundRect(PetOptions.backgrounds[look.background.coerceIn(0, PetOptions.backgrounds.lastIndex)], cornerRadius = CornerRadius(s * 0.2f))
        drawCharacter(look, mood, stage, breathe, blink, spin)
    }
}

private fun DrawScope.drawCharacter(look: PetLook, mood: PetMood, stage: Int, breathe: Float, blink: Float, spin: Float) {
    val s = size.minDimension
    val skin = PetOptions.skins[look.skin.coerceIn(0, PetOptions.skins.lastIndex)]
    val hair = PetOptions.hairColors[look.hairColor.coerceIn(0, PetOptions.hairColors.lastIndex)]
    val outfit = PetOptions.outfitColors[look.outfitColor.coerceIn(0, PetOptions.outfitColors.lastIndex)]
    val ink = Color(0xFF2A1F1B)
    // Younger = smaller body, relatively bigger head.
    val bodyW = s * (0.50f + 0.06f * stage)
    val headR = s * (0.235f - 0.012f * stage)
    val lift = breathe * s * 0.012f
    val cx = s / 2f
    val headCy = s * (0.46f + 0.02f * (3 - stage) * 0.5f) - lift
    val shoulderTop = headCy + headR * 0.95f

    if (stage == 3) {
        repeat(6) { i ->
            val a = spin + i * 1.0471976f
            val r = s * 0.44f
            val p = Offset(cx + cos(a) * r, s * 0.46f + sin(a) * r * 0.8f)
            drawCircle(Color.White.copy(alpha = 0.85f), s * 0.014f, p)
        }
    }

    // Back hair (behind head and body)
    fun backHair() {
        when (look.hairStyle) {
            1 -> drawRoundRect(hair, Offset(cx - headR * 1.12f, headCy - headR * 0.2f), Size(headR * 2.24f, headR * 2.6f), CornerRadius(headR * 0.9f))
            2 -> drawRoundRect(hair, Offset(cx - headR * 1.1f, headCy - headR * 0.3f), Size(headR * 2.2f, headR * 1.5f), CornerRadius(headR * 0.8f))
            3 -> { drawCircle(hair, headR * 0.34f, Offset(cx + headR * 1.05f, headCy - headR * 0.3f)); drawRoundRect(hair, Offset(cx + headR * 0.85f, headCy - headR * 0.2f), Size(headR * 0.34f, headR * 1.5f), CornerRadius(headR * 0.17f)) }
            4 -> { drawCircle(hair, headR * 0.4f, Offset(cx - headR * 1.05f, headCy + headR * 0.55f)); drawCircle(hair, headR * 0.4f, Offset(cx + headR * 1.05f, headCy + headR * 0.55f)) }
            0 -> drawCircle(hair, headR * 0.45f, Offset(cx, headCy - headR * 1.15f))
            7 -> { listOf(-1.0f, -0.55f, 0f, 0.55f, 1.0f).forEach { k -> drawCircle(hair, headR * 0.42f, Offset(cx + k * headR * 0.95f, headCy - headR * 0.55f + kotlin.math.abs(k) * headR * 0.35f)) } }
        }
    }
    backHair()

    // Body / outfit
    val bodyTop = shoulderTop
    val bodyH = s - bodyTop + s * 0.04f
    val bodyLeft = cx - bodyW / 2f
    val body = Path().apply {
        moveTo(bodyLeft, s)
        lineTo(bodyLeft, bodyTop + bodyH * 0.28f)
        quadraticBezierTo(bodyLeft, bodyTop, bodyLeft + bodyW * 0.22f, bodyTop)
        lineTo(bodyLeft + bodyW * 0.78f, bodyTop)
        quadraticBezierTo(bodyLeft + bodyW, bodyTop, bodyLeft + bodyW, bodyTop + bodyH * 0.28f)
        lineTo(bodyLeft + bodyW, s)
        close()
    }
    if (look.outfitStyle == 1) drawCircle(outfit.copy(alpha = 0.85f), headR * 0.95f, Offset(cx, bodyTop + headR * 0.1f)) // hood
    // neck
    drawRoundRect(skin, Offset(cx - headR * 0.28f, headCy + headR * 0.6f), Size(headR * 0.56f, headR * 0.7f), CornerRadius(headR * 0.15f))
    drawPath(body, outfit)
    when (look.outfitStyle) {
        4 -> { // stripes
            val stripe = Color.White.copy(alpha = 0.55f)
            var y = bodyTop + bodyH * 0.2f
            while (y < s) { drawRect(stripe, Offset(bodyLeft, y), Size(bodyW, s * 0.025f)); y += s * 0.07f }
        }
        2 -> { // jacket: inner shirt + lapels
            drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(cx - bodyW * 0.14f, bodyTop), Size(bodyW * 0.28f, bodyH), CornerRadius(bodyW * 0.05f))
            drawLine(outfit.copy(alpha = 0.6f), Offset(cx - bodyW * 0.14f, bodyTop), Offset(cx - bodyW * 0.14f, s), strokeWidth = s * 0.012f)
            drawLine(outfit.copy(alpha = 0.6f), Offset(cx + bodyW * 0.14f, bodyTop), Offset(cx + bodyW * 0.14f, s), strokeWidth = s * 0.012f)
        }
        3 -> { // collar
            val col = Path().apply { moveTo(cx - headR * 0.5f, bodyTop); lineTo(cx, bodyTop + headR * 0.55f); lineTo(cx + headR * 0.5f, bodyTop); close() }
            drawPath(col, Color.White.copy(alpha = 0.92f))
        }
        else -> drawArc(skin, 0f, 180f, true, Offset(cx - headR * 0.4f, bodyTop - headR * 0.33f), Size(headR * 0.8f, headR * 0.66f)) // round neckline
    }

    // Head
    drawCircle(skin, headR * 0.17f, Offset(cx - headR * 1.0f, headCy + headR * 0.1f))
    drawCircle(skin, headR * 0.17f, Offset(cx + headR * 1.0f, headCy + headR * 0.1f))
    drawOval(skin, Offset(cx - headR, headCy - headR), Size(headR * 2f, headR * 2.05f))

    // Front hair
    when (look.hairStyle) {
        5, 6 -> {
            val p = Path().apply {
                moveTo(cx - headR * 1.02f, headCy - headR * 0.05f)
                quadraticBezierTo(cx - headR * 1.1f, headCy - headR * 1.15f, cx, headCy - headR * 1.12f)
                quadraticBezierTo(cx + headR * 1.1f, headCy - headR * 1.15f, cx + headR * 1.02f, headCy - headR * 0.05f)
                if (look.hairStyle == 6) { lineTo(cx + headR * 0.6f, headCy - headR * 0.55f); lineTo(cx + headR * 0.3f, headCy - headR * 0.3f); lineTo(cx, headCy - headR * 0.6f); lineTo(cx - headR * 0.35f, headCy - headR * 0.3f); lineTo(cx - headR * 0.65f, headCy - headR * 0.55f) }
                else { quadraticBezierTo(cx, headCy - headR * 0.55f, cx - headR * 1.02f, headCy - headR * 0.05f) }
                close()
            }
            drawPath(p, hair)
        }
        7 -> drawArc(hair, 180f, 180f, true, Offset(cx - headR * 1.05f, headCy - headR * 1.1f), Size(headR * 2.1f, headR * 1.3f))
        else -> {
            val p = Path().apply {
                moveTo(cx - headR * 1.03f, headCy + headR * 0.1f)
                quadraticBezierTo(cx - headR * 1.15f, headCy - headR * 1.2f, cx, headCy - headR * 1.12f)
                quadraticBezierTo(cx + headR * 1.15f, headCy - headR * 1.2f, cx + headR * 1.03f, headCy + headR * 0.1f)
                quadraticBezierTo(cx + headR * 0.7f, headCy - headR * 0.35f, cx + headR * 0.1f, headCy - headR * 0.62f)
                quadraticBezierTo(cx - headR * 0.55f, headCy - headR * 0.4f, cx - headR * 1.03f, headCy + headR * 0.1f)
                close()
            }
            drawPath(p, hair)
        }
    }

    // Face
    val eyeY = headCy + headR * 0.12f
    val eyeDx = headR * 0.42f
    val eyeR = headR * 0.1f
    val closedByMood = mood == PetMood.CELEBRATE || look.eyes == 3
    fun eye(x: Float, wink: Boolean) {
        val shut = blink > 0.5f || look.eyes == 2 || (wink && look.eyes == 3)
        if (mood == PetMood.CELEBRATE) {
            drawArc(ink, 200f, 140f, false, Offset(x - eyeR * 1.4f, eyeY - eyeR * 1.2f), Size(eyeR * 2.8f, eyeR * 2.4f), style = Stroke(s * 0.011f, cap = StrokeCap.Round))
        } else if (shut) {
            drawLine(ink, Offset(x - eyeR * 1.3f, eyeY), Offset(x + eyeR * 1.3f, eyeY), strokeWidth = s * 0.011f, cap = StrokeCap.Round)
        } else {
            drawCircle(ink, eyeR * 1.15f, Offset(x, eyeY))
            if (look.eyes == 1) { drawCircle(Color.White, eyeR * 0.5f, Offset(x - eyeR * 0.3f, eyeY - eyeR * 0.35f)); drawCircle(Color.White, eyeR * 0.22f, Offset(x + eyeR * 0.4f, eyeY + eyeR * 0.4f)) }
            else drawCircle(Color.White, eyeR * 0.38f, Offset(x - eyeR * 0.3f, eyeY - eyeR * 0.3f))
        }
    }
    eye(cx - eyeDx, false); eye(cx + eyeDx, true)
    if (mood == PetMood.STRESSED) {
        drawLine(ink, Offset(cx - eyeDx - eyeR * 1.4f, eyeY - eyeR * 3.2f), Offset(cx - eyeDx + eyeR * 1.4f, eyeY - eyeR * 2.2f), strokeWidth = s * 0.011f, cap = StrokeCap.Round)
        drawLine(ink, Offset(cx + eyeDx + eyeR * 1.4f, eyeY - eyeR * 3.2f), Offset(cx + eyeDx - eyeR * 1.4f, eyeY - eyeR * 2.2f), strokeWidth = s * 0.011f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF8ED8F8), s * 0.016f, Offset(cx + headR * 0.95f, headCy - headR * 0.35f))
    }
    if (look.blush) {
        drawCircle(Color(0xFFF08A9B).copy(alpha = 0.45f), headR * 0.16f, Offset(cx - headR * 0.68f, headCy + headR * 0.45f))
        drawCircle(Color(0xFFF08A9B).copy(alpha = 0.45f), headR * 0.16f, Offset(cx + headR * 0.68f, headCy + headR * 0.45f))
    }
    val mouthY = headCy + headR * 0.58f
    when (mood) {
        PetMood.STRESSED -> drawArc(ink, 200f, 140f, false, Offset(cx - headR * 0.16f, mouthY), Size(headR * 0.32f, headR * 0.2f), style = Stroke(s * 0.011f, cap = StrokeCap.Round))
        PetMood.CELEBRATE, PetMood.THRIVING -> drawArc(Color(0xFF8E2B3B), 0f, 180f, true, Offset(cx - headR * 0.24f, mouthY - headR * 0.1f), Size(headR * 0.48f, headR * 0.32f))
        else -> drawArc(ink, 20f, 140f, false, Offset(cx - headR * 0.2f, mouthY - headR * 0.12f), Size(headR * 0.4f, headR * 0.26f), style = Stroke(s * 0.011f, cap = StrokeCap.Round))
    }

    // Accessories
    when (look.accessory) {
        1 -> {
            val st = Stroke(s * 0.011f)
            drawCircle(ink, headR * 0.27f, Offset(cx - eyeDx, eyeY), style = st); drawCircle(ink, headR * 0.27f, Offset(cx + eyeDx, eyeY), style = st)
            drawLine(ink, Offset(cx - eyeDx + headR * 0.27f, eyeY), Offset(cx + eyeDx - headR * 0.27f, eyeY), strokeWidth = s * 0.011f)
        }
        2 -> { drawArc(outfit, 180f, 180f, true, Offset(cx - headR * 1.05f, headCy - headR * 1.2f), Size(headR * 2.1f, headR * 1.5f)); drawRoundRect(outfit, Offset(cx - headR * 0.1f, headCy - headR * 0.52f), Size(headR * 1.35f, headR * 0.16f), CornerRadius(headR * 0.08f)) }
        3 -> drawArc(outfit, 190f, 160f, false, Offset(cx - headR * 1.02f, headCy - headR * 1.12f), Size(headR * 2.04f, headR * 1.9f), style = Stroke(headR * 0.2f, cap = StrokeCap.Round))
        4 -> { val c = Offset(cx + headR * 0.65f, headCy - headR * 0.85f); repeat(5) { i -> val a = i * 1.2566371f; drawCircle(Color(0xFFFF8FB8), headR * 0.13f, Offset(c.x + cos(a) * headR * 0.16f, c.y + sin(a) * headR * 0.16f)) }; drawCircle(Color(0xFFFFD34D), headR * 0.08f, c) }
        5 -> { drawArc(Color(0xFF2B2B2B), 180f, 180f, false, Offset(cx - headR * 1.12f, headCy - headR * 1.15f), Size(headR * 2.24f, headR * 2.0f), style = Stroke(headR * 0.12f)); drawRoundRect(Color(0xFF2B2B2B), Offset(cx - headR * 1.25f, headCy - headR * 0.1f), Size(headR * 0.3f, headR * 0.6f), CornerRadius(headR * 0.1f)); drawRoundRect(Color(0xFF2B2B2B), Offset(cx + headR * 0.95f, headCy - headR * 0.1f), Size(headR * 0.3f, headR * 0.6f), CornerRadius(headR * 0.1f)) }
        6 -> { val p = Path().apply { moveTo(cx - headR * 0.45f, headCy - headR * 0.95f); lineTo(cx, headCy - headR * 1.95f); lineTo(cx + headR * 0.45f, headCy - headR * 0.95f); close() }; drawPath(p, Color(0xFFB48CF2)); drawCircle(Color(0xFFFFD34D), headR * 0.1f, Offset(cx, headCy - headR * 1.95f)) }
        7 -> { val p = Path().apply { moveTo(cx - headR * 0.7f, headCy - headR * 0.95f); lineTo(cx - headR * 0.78f, headCy - headR * 1.6f); lineTo(cx - headR * 0.35f, headCy - headR * 1.25f); lineTo(cx, headCy - headR * 1.75f); lineTo(cx + headR * 0.35f, headCy - headR * 1.25f); lineTo(cx + headR * 0.78f, headCy - headR * 1.6f); lineTo(cx + headR * 0.7f, headCy - headR * 0.95f); close() }; drawPath(p, Color(0xFFFFC93C)) }
    }

    // Mood props
    if (mood == PetMood.RAMEN) {
        drawArc(Color(0xFFE9E2D0), 0f, 180f, true, Offset(cx - s * 0.2f, s * 0.8f), Size(s * 0.4f, s * 0.3f))
        drawLine(Color(0xFFF5C84C), Offset(cx - s * 0.12f, s * 0.82f), Offset(cx - s * 0.04f, s * 0.9f), strokeWidth = s * 0.012f, cap = StrokeCap.Round)
        drawLine(Color(0xFFF5C84C), Offset(cx, s * 0.82f), Offset(cx + s * 0.06f, s * 0.9f), strokeWidth = s * 0.012f, cap = StrokeCap.Round)
    }
    if (mood == PetMood.CELEBRATE) {
        val cols = listOf(Color(0xFFF26D85), Color(0xFFF7B733), Color(0xFF5BB8D9), Color(0xFF9BDE3A))
        repeat(8) { i ->
            val a = spin + i * 0.785f
            drawCircle(cols[i % cols.size], s * 0.012f, Offset(cx + cos(a) * s * 0.4f, s * 0.2f + sin(a * 2f) * s * 0.08f + (i % 3) * s * 0.05f))
        }
    }
}
