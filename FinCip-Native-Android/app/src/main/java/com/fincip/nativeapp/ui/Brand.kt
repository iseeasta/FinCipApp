package com.fincip.nativeapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FinCipLime = Color(0xFFC6FF00)
val FinCipInk = Color(0xFF171717)

/**
 * The FinCip mark: a single-colour F whose arms are cut on a rising diagonal, so the letter
 * also reads as upward movement. Same geometry as the Android launcher icon (ic_launcher_foreground).
 */
@Composable
fun FinCipMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tile: Color = FinCipLime,
    glyph: Color = FinCipInk,
    border: Color? = null
) {
    Canvas(modifier.size(size)) {
        val s = this.size.width
        drawRoundRect(tile, cornerRadius = CornerRadius(s * 0.24f))
        if (border != null) drawRoundRect(border, cornerRadius = CornerRadius(s * 0.24f), style = Stroke(s * 0.06f))
        // F glyph in a 38 x 44 box (viewport units), centred in the tile with ~20% padding.
        val u = s * 0.56f / 38f
        val ox = s * 0.22f
        val oy = (s - 44f * u) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
        val f = Path().apply {
            moveTo(p(0f, 0f).x, p(0f, 0f).y)
            lineTo(p(38f, 0f).x, p(38f, 0f).y)
            lineTo(p(30f, 12f).x, p(30f, 12f).y)
            lineTo(p(12f, 12f).x, p(12f, 12f).y)
            lineTo(p(12f, 20f).x, p(12f, 20f).y)
            lineTo(p(26f, 20f).x, p(26f, 20f).y)
            lineTo(p(20f, 30f).x, p(20f, 30f).y)
            lineTo(p(12f, 30f).x, p(12f, 30f).y)
            lineTo(p(12f, 44f).x, p(12f, 44f).y)
            lineTo(p(0f, 44f).x, p(0f, 44f).y)
            close()
        }
        drawPath(f, glyph)
    }
}

/** Horizontal logo: mark + wordmark. */
@Composable
fun FinCipLogo(modifier: Modifier = Modifier, markSize: Dp = 36.dp, textColor: Color, tile: Color = FinCipLime, glyph: Color = FinCipInk) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FinCipMark(size = markSize, tile = tile, glyph = glyph)
        Text("FinCip", color = textColor, fontWeight = FontWeight.Black, fontSize = (markSize.value * 0.62f).sp, letterSpacing = (-0.8).sp)
    }
}
