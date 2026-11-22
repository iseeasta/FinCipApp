package com.fincip.nativeapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class ChartSeries(val name: String, val color: Color, val values: List<Double>)

/** Short money label for chart axes: ₹1.2L, ₹3.4Cr, $4.5k. */
fun compactMoney(amount: Double, currency: String = "INR"): String {
    val symbol = if (currency == "INR") "₹" else runCatching { java.util.Currency.getInstance(currency).symbol }.getOrDefault("")
    val a = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    val body = if (currency == "INR") when {
        a >= 1e7 -> String.format(Locale.ENGLISH, "%.1fCr", a / 1e7)
        a >= 1e5 -> String.format(Locale.ENGLISH, "%.1fL", a / 1e5)
        a >= 1e3 -> String.format(Locale.ENGLISH, "%.1fk", a / 1e3)
        else -> String.format(Locale.ENGLISH, "%.0f", a)
    } else when {
        a >= 1e9 -> String.format(Locale.ENGLISH, "%.1fB", a / 1e9)
        a >= 1e6 -> String.format(Locale.ENGLISH, "%.1fM", a / 1e6)
        a >= 1e3 -> String.format(Locale.ENGLISH, "%.1fk", a / 1e3)
        else -> String.format(Locale.ENGLISH, "%.0f", a)
    }
    return sign + symbol + body
}

@Composable
private fun rememberReveal(durationMs: Int = 800): Float {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val p by animateFloatAsState(if (started) 1f else 0f, tween(durationMs), label = "reveal")
    return p
}

/** Smooth multi-series line chart with area fill under the first series, light grid and animated reveal. */
@Composable
fun LineChart(
    labels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    currency: String = "INR",
    hideValues: Boolean = false
) {
    val fg = LocalContentColor.current
    val peak = (series.flatMap { it.values }.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val progress = rememberReveal()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(height)) {
            Column(Modifier.width(48.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                listOf(peak, peak / 2, 0.0).forEach {
                    Text(if (hideValues) "" else compactMoney(it, currency), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = .55f), maxLines = 1)
                }
            }
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val w = size.width
                val h = size.height
                val n = labels.size.coerceAtLeast(1)
                fun px(i: Int): Float = w * (i + 0.5f) / n
                fun py(v: Double): Float = (h - (v / peak * h)).toFloat()
                listOf(0f, .5f, 1f).forEach { f ->
                    drawLine(fg.copy(alpha = .12f), Offset(0f, (h * f).coerceIn(1f, h - 1f)), Offset(w, (h * f).coerceIn(1f, h - 1f)), strokeWidth = 1.dp.toPx())
                }
                clipRect(right = w * progress) {
                    series.forEachIndexed { si, s ->
                        if (s.values.size < 2) return@forEachIndexed
                        val path = Path()
                        s.values.forEachIndexed { i, v ->
                            if (i == 0) path.moveTo(px(0), py(v)) else {
                                val cx = (px(i - 1) + px(i)) / 2f
                                path.cubicTo(cx, py(s.values[i - 1]), cx, py(v), px(i), py(v))
                            }
                        }
                        if (si == 0) {
                            val area = Path()
                            area.addPath(path)
                            area.lineTo(px(s.values.lastIndex), h)
                            area.lineTo(px(0), h)
                            area.close()
                            drawPath(area, Brush.verticalGradient(listOf(s.color.copy(alpha = .32f), Color.Transparent)))
                        }
                        drawPath(path, s.color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                if (progress > .98f) series.forEach { s ->
                    if (s.values.isNotEmpty()) {
                        val i = s.values.lastIndex
                        drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(px(i), py(s.values[i])))
                        drawCircle(s.color, radius = 4.dp.toPx(), center = Offset(px(i), py(s.values[i])))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(48.dp))
            labels.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = .6f), textAlign = TextAlign.Center, maxLines = 1) }
        }
        if (series.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(10.dp).background(s.color, CircleShape))
                        Text(s.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Donut with centre text. Slices animate in clockwise from 12 o'clock. */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Double>>,
    centerTop: String,
    modifier: Modifier = Modifier,
    centerBottom: String = "",
    diameter: Dp = 150.dp
) {
    val fg = LocalContentColor.current
    val total = slices.sumOf { it.second }.coerceAtLeast(0.0001)
    val progress = rememberReveal(900)
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * .17f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(fg.copy(alpha = .09f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            var start = -90f
            slices.forEach { (color, v) ->
                val full = (v / total * 360.0).toFloat()
                val sweep = full * progress
                if (sweep > 0.5f) drawArc(color, start, (sweep - 1.5f).coerceAtLeast(0.5f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                start += full
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1)
            if (centerBottom.isNotBlank()) Text(centerBottom, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = .65f), maxLines = 1)
        }
    }
}

/** Single-value ring, e.g. a health score or goal completion. */
@Composable
fun RingGauge(progress: Float, color: Color, centerText: String, modifier: Modifier = Modifier, diameter: Dp = 92.dp, caption: String = "") {
    val fg = LocalContentColor.current
    val animated = rememberReveal(900) * progress.coerceIn(0f, 1f)
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * .14f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(fg.copy(alpha = .12f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (animated > 0.002f) drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, fontWeight = FontWeight.Black, fontSize = 22.sp, maxLines = 1)
            if (caption.isNotBlank()) Text(caption, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = .65f), maxLines = 1)
        }
    }
}

/** Colour dot + label + value row used next to donuts. */
@Composable
fun LegendRow(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

/** Colour cycle used for category charts. */
val ChartColors = listOf(Pop.Coral, Pop.Sun, Pop.Lavender, Pop.Teal, Pop.Peach, Color(0xFF5BB8D9), Color(0xFF8FCB5A), Color(0xFFB0A8C8))
