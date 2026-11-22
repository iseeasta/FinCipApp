package com.fincip.nativeapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private class AvatarLook(seed: Long) {
    private val r = java.util.Random(seed)
    private fun <T> pick(list: List<T>): T = list[r.nextInt(list.size)]
    val bg = pick(listOf(Color(0xFFFFF3D6), Color(0xFFFFD6DE), Color(0xFFD6F2E2), Color(0xFFD7E8FF), Color(0xFFFFE0C7), Color(0xFFE6DEFF)))
    val skin = pick(listOf(Color(0xFFFFD9B8), Color(0xFFF2C29B), Color(0xFFD9A06F), Color(0xFFB57B4E), Color(0xFF8A5A38)))
    val hair = pick(listOf(Color(0xFF14204F), Color(0xFF2B1B12), Color(0xFF0E0E10), Color(0xFF7A3B1E), Color(0xFFC99A3E), Color(0xFF5B3A8C)))
    val shirt = pick(listOf(Color(0xFF1E1B2E), Color(0xFFF26D85), Color(0xFF2E8C7F), Color(0xFFF7B733), Color(0xFF354BFF), Color(0xFFF3F0E7)))
    val style = r.nextInt(4)
    val smile = r.nextBoolean()
}

/** Flat, friendly avatar generated from [seed]. The same seed always draws the same face. */
@Composable
fun ProfileAvatar(seed: Long, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val look = remember(seed) { AvatarLook(seed) }
    val ink = Color(0xFF14204F)
    Canvas(modifier.size(size).clip(CircleShape)) {
        val s = this.size.width
        drawRect(look.bg)
        // shoulders + neck
        drawOval(look.shirt, Offset(s * .12f, s * .72f), Size(s * .76f, s * .62f))
        drawRect(lerp(look.skin, ink, .12f), Offset(s * .43f, s * .58f), Size(s * .14f, s * .18f))
        // long hair sits behind the face
        if (look.style == 2) drawRoundRect(look.hair, Offset(s * .23f, s * .26f), Size(s * .54f, s * .56f), CornerRadius(s * .22f))
        // face
        drawOval(look.skin, Offset(s * .28f, s * .27f), Size(s * .44f, s * .46f))
        // hair on top
        when (look.style) {
            1 -> {
                val spikes = Path().apply {
                    moveTo(s * .26f, s * .44f)
                    lineTo(s * .27f, s * .24f); lineTo(s * .35f, s * .31f)
                    lineTo(s * .42f, s * .17f); lineTo(s * .50f, s * .29f)
                    lineTo(s * .58f, s * .17f); lineTo(s * .65f, s * .31f)
                    lineTo(s * .73f, s * .24f); lineTo(s * .74f, s * .44f)
                    close()
                }
                drawPath(spikes, look.hair)
            }
            else -> {
                drawArc(look.hair, 180f, 180f, true, Offset(s * .25f, s * .21f), Size(s * .50f, s * .44f))
                drawRect(look.hair, Offset(s * .26f, s * .36f), Size(s * .48f, s * .07f))
                if (look.style == 3) drawCircle(look.hair, s * .09f, Offset(s * .5f, s * .17f))
            }
        }
        // face details
        drawCircle(ink, s * .024f, Offset(s * .42f, s * .53f))
        drawCircle(ink, s * .024f, Offset(s * .58f, s * .53f))
        drawCircle(Color(0xFFFF8FA3).copy(alpha = .5f), s * .035f, Offset(s * .36f, s * .60f))
        drawCircle(Color(0xFFFF8FA3).copy(alpha = .5f), s * .035f, Offset(s * .64f, s * .60f))
        val stroke = Stroke(s * .016f, cap = StrokeCap.Round)
        if (look.smile) drawArc(ink, 20f, 140f, false, Offset(s * .45f, s * .58f), Size(s * .10f, s * .06f), style = stroke)
        else drawLine(ink, Offset(s * .46f, s * .64f), Offset(s * .54f, s * .64f), strokeWidth = s * .016f, cap = StrokeCap.Round)
    }
}
