package com.fincip.nativeapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class PetMood { HAPPY, THRIVING, STRESSED, RAMEN, CELEBRATE }

/**
 * Hand-drawn money pal: bobs, blinks, and changes face with [mood].
 * Drawn on a Canvas so it scales cleanly and carries the user's accent colour.
 */
@Composable
fun PetBlob(accent: Color, mood: PetMood, modifier: Modifier = Modifier, diameter: Dp = 112.dp) {
    val transition = rememberInfiniteTransition(label = "pet")
    val bob by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val blink by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(keyframes<Float> { durationMillis = 3400; 0f at 0; 0f at 3100; 1f at 3220; 0f at 3340 }),
        label = "blink"
    )
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "spin")
    val ink = Color(0xFF1E1B2E)
    val darker = lerp(accent, ink, 0.22f)
    Canvas(modifier.size(diameter)) {
        val w = this.size.width
        val h = this.size.height
        val dy = -bob * h * 0.05f
        val squash = 1f + bob * 0.03f
        // ground shadow
        drawOval(Color.Black.copy(alpha = .13f), Offset(w * .22f, h * .90f), Size(w * .56f * (1f - bob * .1f), h * .06f))
        // ears
        drawCircle(darker, w * .12f, Offset(w * .27f, h * .22f + dy))
        drawCircle(darker, w * .12f, Offset(w * .73f, h * .22f + dy))
        // body + belly
        drawOval(accent, Offset(w * .12f, h * .18f + dy), Size(w * .76f, h * .70f * squash))
        drawOval(Color.White.copy(alpha = .28f), Offset(w * .30f, h * .54f + dy), Size(w * .40f, h * .26f))
        // eyes (blink squashes them)
        val eyeY = h * .46f + dy
        val eyeR = w * .075f
        val eyeH = eyeR * (1f - blink * .92f)
        listOf(.36f, .64f).forEach { x ->
            drawOval(Color.White, Offset(w * x - eyeR * 1.3f, eyeY - eyeH * 1.3f), Size(eyeR * 2.6f, eyeH * 2.6f))
            drawOval(ink, Offset(w * x - eyeR * .62f, eyeY - eyeH * .62f), Size(eyeR * 1.24f, eyeH * 1.24f))
        }
        // cheeks
        drawCircle(Color(0xFFFF8FA3).copy(alpha = .55f), w * .045f, Offset(w * .26f, h * .59f + dy))
        drawCircle(Color(0xFFFF8FA3).copy(alpha = .55f), w * .045f, Offset(w * .74f, h * .59f + dy))
        // mouth
        val my = h * .62f + dy
        val stroke = Stroke(w * .026f, cap = StrokeCap.Round)
        when (mood) {
            PetMood.HAPPY, PetMood.THRIVING, PetMood.CELEBRATE -> drawArc(ink, 15f, 150f, false, Offset(w * .42f, my - h * .04f), Size(w * .16f, h * .10f), style = stroke)
            PetMood.STRESSED -> drawArc(ink, 195f, 150f, false, Offset(w * .42f, my), Size(w * .16f, h * .08f), style = stroke)
            PetMood.RAMEN -> drawOval(ink, Offset(w * .465f, my - h * .01f), Size(w * .07f, h * .09f))
        }
        if (mood == PetMood.THRIVING || mood == PetMood.CELEBRATE) {
            for (i in 0..2) {
                val a = Math.toRadians((spin + i * 120.0))
                val cx = w * .5f + cos(a).toFloat() * w * .47f
                val cy = h * .40f + sin(a).toFloat() * h * .15f + dy
                drawCircle(Color.White, w * .032f, Offset(cx, cy))
                drawCircle(Color(0xFFF7B733), w * .018f, Offset(cx, cy))
            }
        }
        if (mood == PetMood.STRESSED) drawCircle(Color(0xFF7DC6FF), w * .035f, Offset(w * .80f, h * .34f + dy + bob * h * .03f))
    }
}
