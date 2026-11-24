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
    val species: Int = 0,
    val eyeColor: Int = 0,
    val freckles: Boolean = false,
    val mouth: Int = 0,
    val brows: Int = 0,
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
    val species = listOf("Girl", "Boy", "Cat", "Bunny", "Bear", "Fox", "Panda", "Robot", "Dino")
    val furs = listOf(0xFFF2C9A0, 0xFFE8A15C, 0xFFF5F0E6, 0xFF9C9C9C, 0xFF5E4636, 0xFF2E2E32, 0xFFF7B7C8, 0xFFA8D8F0, 0xFF7BC67E, 0xFFB9A0E8).map { Color(it) }
    val eyeColors = listOf(0xFF2A1F1B, 0xFF4B7BEC, 0xFF3FA66B, 0xFF8B5A2B, 0xFF7C4DFF, 0xFFE0408A, 0xFF00B8D9, 0xFFF2A900).map { Color(it) }
    val mouths = listOf("Auto", "Smile", "Grin", "Tongue", "Cat")
    val brows = listOf("None", "Soft", "Bold")
    val hairStyles = listOf("Bun", "Long", "Bob", "Ponytail", "Pigtails", "Short", "Spiky", "Curly")
    val outfitStyles = listOf("T-shirt", "Hoodie", "Jacket", "Collar", "Stripes")
    val eyeStyles = listOf("Round", "Sparkle", "Sleepy", "Wink")
    /** Accessories and the pet level at which each unlocks. */
    val accessories = listOf(
        "None" to 1, "Round glasses" to 1, "Cap" to 1, "Headband" to 1, "Flower" to 2, "Headphones" to 3, "Party hat" to 5, "Crown" to 10,
        "Sunglasses" to 2, "Bow" to 2, "Scarf" to 3, "Moustache" to 4, "Wizard hat" to 6, "Halo" to 8, "Bandana" to 4, "Cat ears" to 3
    )
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
fun PetCharacter(look: PetLook, mood: PetMood, level: Int, modifier: Modifier = Modifier, diameter: Dp = 140.dp, showBackground: Boolean = true, talking: Boolean = false) {
    val t = rememberInfiniteTransition(label = "pet")
    val talkOsc by t.animateFloat(0.15f, 1f, infiniteRepeatable(tween(170, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "talk")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val blink by t.animateFloat(0f, 1f, infiniteRepeatable(keyframes<Float> { durationMillis = 3600; 0f at 0; 0f at 3300; 1f at 3420; 0f at 3540 }), label = "blink")
    val spin by t.animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(6000)), label = "spin")
    val stage = petStage(level)
    Canvas(modifier.size(diameter)) {
        val s = size.minDimension
        if (showBackground) drawRoundRect(PetOptions.backgrounds[look.background.coerceIn(0, PetOptions.backgrounds.lastIndex)], cornerRadius = CornerRadius(s * 0.2f))
        drawCharacter(look, mood, stage, breathe, blink, spin, if (talking) talkOsc else 0f)
    }
}

private fun DrawScope.drawCharacter(look: PetLook, mood: PetMood, stage: Int, breathe: Float, blink: Float, spin: Float, talk: Float = 0f) {
    val s = size.minDimension
    val animal = look.species >= 2
    val skin = if (look.species == 6) Color(0xFFF5F0E6) else if (animal) PetOptions.furs[look.skin.coerceIn(0, PetOptions.furs.lastIndex)] else PetOptions.skins[look.skin.coerceIn(0, PetOptions.skins.lastIndex)]
    val eyeCol = PetOptions.eyeColors[look.eyeColor.coerceIn(0, PetOptions.eyeColors.lastIndex)]
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
    if (!animal) backHair()
    if (animal) drawEarsBehind(look.species, skin, headR, cx, headCy)

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
    if (!animal) {
        drawCircle(skin, headR * 0.17f, Offset(cx - headR * 1.0f, headCy + headR * 0.1f))
        drawCircle(skin, headR * 0.17f, Offset(cx + headR * 1.0f, headCy + headR * 0.1f))
    }
    if (look.species == 7) {
        drawRoundRect(skin, Offset(cx - headR, headCy - headR * 0.95f), Size(headR * 2f, headR * 2f), CornerRadius(headR * 0.45f))
        drawLine(Color(0xFF8A8A92), Offset(cx, headCy - headR * 0.95f), Offset(cx, headCy - headR * 1.4f), strokeWidth = s * 0.012f)
        drawCircle(Color(0xFFB9F227), headR * 0.12f, Offset(cx, headCy - headR * 1.4f))
        drawRoundRect(Color(0xFF1E2430), Offset(cx - headR * 0.8f, headCy - headR * 0.4f), Size(headR * 1.6f, headR * 1.05f), CornerRadius(headR * 0.3f))
    } else drawOval(skin, Offset(cx - headR, headCy - headR), Size(headR * 2f, headR * 2.05f))
    if (animal) drawAnimalFace(look.species, skin, headR, cx, headCy, s, ink)

    // Front hair
    if (!animal) when (look.hairStyle) {
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
            if (look.species == 7) { drawCircle(eyeCol, eyeR * 1.2f, Offset(x, eyeY)); return }
            drawCircle(ink, eyeR * 1.15f, Offset(x, eyeY))
            if (eyeCol != PetOptions.eyeColors[0]) drawCircle(eyeCol, eyeR * 0.85f, Offset(x, eyeY + eyeR * 0.12f))
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
    if (look.brows > 0 && mood != PetMood.STRESSED && look.species != 7) {
        val bw = if (look.brows == 2) s * 0.018f else s * 0.01f
        drawLine(hair, Offset(cx - eyeDx - eyeR * 1.4f, eyeY - eyeR * 2.4f), Offset(cx - eyeDx + eyeR * 1.4f, eyeY - eyeR * 2.6f), strokeWidth = bw, cap = StrokeCap.Round)
        drawLine(hair, Offset(cx + eyeDx - eyeR * 1.4f, eyeY - eyeR * 2.6f), Offset(cx + eyeDx + eyeR * 1.4f, eyeY - eyeR * 2.4f), strokeWidth = bw, cap = StrokeCap.Round)
    }
    if (look.freckles) listOf(-0.78f to 0.4f, -0.6f to 0.5f, -0.46f to 0.38f, 0.46f to 0.38f, 0.6f to 0.5f, 0.78f to 0.4f).forEach { (dx, dy) -> drawCircle(Color(0xFF8A5A3A).copy(alpha = 0.55f), headR * 0.04f, Offset(cx + dx * headR, headCy + dy * headR)) }
    if (look.blush) {
        drawCircle(Color(0xFFF08A9B).copy(alpha = 0.45f), headR * 0.16f, Offset(cx - headR * 0.68f, headCy + headR * 0.45f))
        drawCircle(Color(0xFFF08A9B).copy(alpha = 0.45f), headR * 0.16f, Offset(cx + headR * 0.68f, headCy + headR * 0.45f))
    }
    val mouthY = headCy + headR * 0.58f
    if (look.mouth > 0 && mood != PetMood.STRESSED && mood != PetMood.CELEBRATE) when (look.mouth) {
        1 -> drawArc(ink, 20f, 140f, false, Offset(cx - headR * 0.2f, mouthY - headR * 0.12f), Size(headR * 0.4f, headR * 0.26f), style = Stroke(s * 0.011f, cap = StrokeCap.Round))
        2 -> drawArc(Color(0xFF8E2B3B), 0f, 180f, true, Offset(cx - headR * 0.26f, mouthY - headR * 0.1f), Size(headR * 0.52f, headR * 0.34f))
        3 -> { drawArc(Color(0xFF8E2B3B), 0f, 180f, true, Offset(cx - headR * 0.22f, mouthY - headR * 0.1f), Size(headR * 0.44f, headR * 0.3f)); drawOval(Color(0xFFF07A8E), Offset(cx - headR * 0.1f, mouthY + headR * 0.08f), Size(headR * 0.2f, headR * 0.16f)) }
        else -> { drawArc(ink, 0f, 160f, false, Offset(cx - headR * 0.22f, mouthY - headR * 0.14f), Size(headR * 0.22f, headR * 0.2f), style = Stroke(s * 0.01f, cap = StrokeCap.Round)); drawArc(ink, 20f, 160f, false, Offset(cx, mouthY - headR * 0.14f), Size(headR * 0.22f, headR * 0.2f), style = Stroke(s * 0.01f, cap = StrokeCap.Round)) }
    } else when (mood) {
        PetMood.STRESSED -> drawArc(ink, 200f, 140f, false, Offset(cx - headR * 0.16f, mouthY), Size(headR * 0.32f, headR * 0.2f), style = Stroke(s * 0.011f, cap = StrokeCap.Round))
        PetMood.CELEBRATE, PetMood.THRIVING -> drawArc(Color(0xFF8E2B3B), 0f, 180f, true, Offset(cx - headR * 0.24f, mouthY - headR * 0.1f), Size(headR * 0.48f, headR * 0.32f))
        else -> drawArc(ink, 20f, 140f, false, Offset(cx - headR * 0.2f, mouthY - headR * 0.12f), Size(headR * 0.4f, headR * 0.26f), style = Stroke(s * 0.011f, cap = StrokeCap.Round))
    }
    if (talk > 0f && look.species != 7) {
        drawOval(Color(0xFF8E2B3B), Offset(cx - headR * 0.15f, mouthY - headR * 0.04f), Size(headR * 0.30f, headR * 0.30f * talk))
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

    when (look.accessory) {
        8 -> { drawRoundRect(Color(0xFF1B1B1F), Offset(cx - eyeDx - headR * 0.3f, eyeY - headR * 0.16f), Size(headR * 0.6f, headR * 0.36f), CornerRadius(headR * 0.12f)); drawRoundRect(Color(0xFF1B1B1F), Offset(cx + eyeDx - headR * 0.3f, eyeY - headR * 0.16f), Size(headR * 0.6f, headR * 0.36f), CornerRadius(headR * 0.12f)); drawLine(Color(0xFF1B1B1F), Offset(cx - eyeDx + headR * 0.3f, eyeY), Offset(cx + eyeDx - headR * 0.3f, eyeY), strokeWidth = s * 0.012f) }
        9 -> { val c = Offset(cx - headR * 0.7f, headCy - headR * 0.95f); val a = Path().apply { moveTo(c.x, c.y); lineTo(c.x - headR * 0.4f, c.y - headR * 0.28f); lineTo(c.x - headR * 0.4f, c.y + headR * 0.28f); close() }; val b = Path().apply { moveTo(c.x, c.y); lineTo(c.x + headR * 0.4f, c.y - headR * 0.28f); lineTo(c.x + headR * 0.4f, c.y + headR * 0.28f); close() }; drawPath(a, outfit); drawPath(b, outfit); drawCircle(outfit, headR * 0.12f, c) }
        10 -> { drawRoundRect(Color(0xFFF26D85), Offset(cx - bodyW * 0.32f, bodyTop - headR * 0.12f), Size(bodyW * 0.64f, headR * 0.34f), CornerRadius(headR * 0.15f)); drawRoundRect(Color(0xFFF26D85), Offset(cx + bodyW * 0.1f, bodyTop), Size(headR * 0.3f, headR * 0.7f), CornerRadius(headR * 0.1f)) }
        11 -> { drawArc(hair, 200f, 140f, false, Offset(cx - headR * 0.38f, mouthY - headR * 0.3f), Size(headR * 0.38f, headR * 0.22f), style = Stroke(s * 0.016f, cap = StrokeCap.Round)); drawArc(hair, 200f, 140f, false, Offset(cx, mouthY - headR * 0.3f), Size(headR * 0.38f, headR * 0.22f), style = Stroke(s * 0.016f, cap = StrokeCap.Round)) }
        12 -> { val p = Path().apply { moveTo(cx - headR * 0.8f, headCy - headR * 0.85f); lineTo(cx + headR * 0.1f, headCy - headR * 2.2f); lineTo(cx + headR * 0.8f, headCy - headR * 0.85f); close() }; drawPath(p, Color(0xFF6A4DC9)); drawRoundRect(Color(0xFF6A4DC9), Offset(cx - headR * 1.2f, headCy - headR * 0.95f), Size(headR * 2.4f, headR * 0.2f), CornerRadius(headR * 0.1f)); drawCircle(Color(0xFFFFD34D), headR * 0.1f, Offset(cx - headR * 0.1f, headCy - headR * 1.4f)) }
        13 -> drawOval(Color(0xFFFFD34D), Offset(cx - headR * 0.6f, headCy - headR * 1.55f), Size(headR * 1.2f, headR * 0.34f), style = Stroke(s * 0.014f))
        14 -> { drawArc(Color(0xFFF26D85), 190f, 160f, false, Offset(cx - headR * 1.02f, headCy - headR * 1.12f), Size(headR * 2.04f, headR * 1.9f), style = Stroke(headR * 0.26f)); drawCircle(Color(0xFFF26D85), headR * 0.16f, Offset(cx + headR * 0.98f, headCy - headR * 0.2f)) }
        15 -> { drawArc(Color(0xFF2B2B2B), 190f, 160f, false, Offset(cx - headR * 1.0f, headCy - headR * 1.08f), Size(headR * 2f, headR * 1.8f), style = Stroke(headR * 0.1f)); val l = Path().apply { moveTo(cx - headR * 0.85f, headCy - headR * 0.7f); lineTo(cx - headR * 0.7f, headCy - headR * 1.45f); lineTo(cx - headR * 0.2f, headCy - headR * 0.98f); close() }; val r = Path().apply { moveTo(cx + headR * 0.85f, headCy - headR * 0.7f); lineTo(cx + headR * 0.7f, headCy - headR * 1.45f); lineTo(cx + headR * 0.2f, headCy - headR * 0.98f); close() }; drawPath(l, Color(0xFF2B2B2B)); drawPath(r, Color(0xFF2B2B2B)) }
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


private fun DrawScope.drawEarsBehind(species: Int, fur: Color, headR: Float, cx: Float, cy: Float) {
    val inner = Color(0xFFF4A8B8)
    when (species) {
        2, 5 -> listOf(-1f, 1f).forEach { k ->
            val p = Path().apply { moveTo(cx + k * headR * 0.95f, cy - headR * 0.1f); lineTo(cx + k * headR * 0.7f, cy - headR * 1.4f); lineTo(cx + k * headR * 0.1f, cy - headR * 0.85f); close() }
            drawPath(p, if (species == 5) Color(0xFFE8863A) else fur)
            val q = Path().apply { moveTo(cx + k * headR * 0.8f, cy - headR * 0.4f); lineTo(cx + k * headR * 0.68f, cy - headR * 1.1f); lineTo(cx + k * headR * 0.3f, cy - headR * 0.8f); close() }
            drawPath(q, if (species == 5) Color(0xFF3A2A22) else inner)
        }
        3 -> listOf(-1f, 1f).forEach { k ->
            drawOval(fur, Offset(cx + k * headR * 0.5f - headR * 0.22f, cy - headR * 2.0f), Size(headR * 0.44f, headR * 1.3f))
            drawOval(inner, Offset(cx + k * headR * 0.5f - headR * 0.11f, cy - headR * 1.85f), Size(headR * 0.22f, headR * 0.95f))
        }
        4, 6 -> listOf(-1f, 1f).forEach { k -> drawCircle(if (species == 6) Color(0xFF2B2B2B) else fur, headR * 0.34f, Offset(cx + k * headR * 0.78f, cy - headR * 0.78f)); if (species == 4) drawCircle(inner, headR * 0.18f, Offset(cx + k * headR * 0.78f, cy - headR * 0.78f)) }
        8 -> listOf(-0.5f, 0f, 0.5f).forEach { k ->
            val p = Path().apply { moveTo(cx + k * headR * 0.7f - headR * 0.18f, cy - headR * 0.92f); lineTo(cx + k * headR * 0.7f, cy - headR * 1.38f); lineTo(cx + k * headR * 0.7f + headR * 0.18f, cy - headR * 0.92f); close() }
            drawPath(p, Color(0xFF4F9B69))
        }
    }
}

private fun DrawScope.drawAnimalFace(species: Int, fur: Color, headR: Float, cx: Float, cy: Float, s: Float, ink: Color) {
    when (species) {
        2 -> { listOf(-1f, 1f).forEach { k -> repeat(2) { i -> drawLine(ink.copy(alpha = 0.6f), Offset(cx + k * headR * 0.45f, cy + headR * (0.42f + i * 0.1f)), Offset(cx + k * headR * 1.1f, cy + headR * (0.34f + i * 0.2f)), strokeWidth = s * 0.006f) } }; drawCircle(Color(0xFFF08A9B), headR * 0.07f, Offset(cx, cy + headR * 0.34f)) }
        3 -> drawCircle(Color(0xFFF08A9B), headR * 0.08f, Offset(cx, cy + headR * 0.34f))
        4 -> { drawOval(Color.White.copy(alpha = 0.55f), Offset(cx - headR * 0.4f, cy + headR * 0.22f), Size(headR * 0.8f, headR * 0.6f)); drawCircle(Color(0xFF3A2A22), headR * 0.09f, Offset(cx, cy + headR * 0.36f)) }
        5 -> { listOf(-1f, 1f).forEach { k -> val p = Path().apply { moveTo(cx + k * headR * 0.98f, cy + headR * 0.2f); lineTo(cx + k * headR * 0.2f, cy + headR * 0.85f); lineTo(cx + k * headR * 0.95f, cy + headR * 0.7f); close() }; drawPath(p, Color.White) }; drawCircle(Color(0xFF3A2A22), headR * 0.08f, Offset(cx, cy + headR * 0.4f)) }
        6 -> { listOf(-1f, 1f).forEach { k -> drawOval(Color(0xFF2B2B2B), Offset(cx + k * headR * 0.42f - headR * 0.2f, cy - headR * 0.05f), Size(headR * 0.4f, headR * 0.5f)) }; drawCircle(Color(0xFF2B2B2B), headR * 0.09f, Offset(cx, cy + headR * 0.36f)) }
        8 -> { drawCircle(Color(0xFF3A6B4A), headR * 0.04f, Offset(cx - headR * 0.14f, cy + headR * 0.32f)); drawCircle(Color(0xFF3A6B4A), headR * 0.04f, Offset(cx + headR * 0.14f, cy + headR * 0.32f)) }
    }
}
