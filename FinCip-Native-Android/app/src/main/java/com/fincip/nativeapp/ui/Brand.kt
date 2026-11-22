package com.fincip.nativeapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FinCipLime = Color(0xFFB9F227)
val FinCipInk = Color(0xFF171717)

// Leaf geometry in a 449 x 522 box (origin = top-left of the mark). Same shapes as ic_launcher_foreground.
private val LeafPaths = listOf(
    "M405,495 C405,484 412,477 428,477 L490,477 C570,477 612,520 612,580 L612,682 C585,640 540,630 490,622 C440,612 405,590 405,545 Z", "M402,650 C402,636 408,633 420,639 C500,668 580,685 612,722 L612,872 C560,850 500,832 455,815 C415,800 402,760 402,700 Z", "M638,640 C638,570 700,530 760,515 C800,505 835,498 845,497 C851,497 851,505 851,512 L851,640 C851,700 820,740 790,757 L638,838 Z"
)
private const val AccentPath = "M642,548 C642,440 700,350 800,350 L849,350 L849,380 C849,440 810,490 750,500 C700,510 665,525 642,548 Z"
private const val MarkW = 449f
private const val MarkH = 522f
private const val OriginX = 402f
private const val OriginY = 350f

/**
 * The FinCip mark: three geometric leaves climbing into one lime shoot. No letters.
 * [glyph] is the leaf colour (defaults to the theme text colour so it stays visible in dark mode).
 */
@Composable
fun FinCipMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    glyph: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = FinCipLime
) {
    val leaves = androidx.compose.runtime.remember { LeafPaths.map { PathParser().parsePathString(it).toPath() } }
    val shoot = androidx.compose.runtime.remember { PathParser().parsePathString(AccentPath).toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension * 0.88f / MarkH
        val ox = (this.size.width - MarkW * s) / 2f
        val oy = (this.size.height - MarkH * s) / 2f
        translate(ox, oy) {
            scale(s, s, pivot = Offset.Zero) {
                translate(-OriginX, -OriginY) {
                    leaves.forEach { drawPath(it, glyph) }
                    drawPath(shoot, accent)
                }
            }
        }
    }
}

/** Horizontal logo: mark + wordmark. */
@Composable
fun FinCipLogo(modifier: Modifier = Modifier, markSize: Dp = 36.dp, textColor: Color, glyph: Color = textColor) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FinCipMark(size = markSize, glyph = glyph)
        Text("FinCip", color = textColor, fontWeight = FontWeight.Black, fontSize = (markSize.value * 0.6f).sp, letterSpacing = (-0.8).sp)
    }
}
