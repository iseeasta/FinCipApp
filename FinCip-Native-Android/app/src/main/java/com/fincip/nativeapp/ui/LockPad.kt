package com.fincip.nativeapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.LockShape

val LockTypes = listOf("password" to "Password", "pin" to "PIN", "pattern" to "Pattern", "draw" to "Drawing", "rhythm" to "Rhythm", "emoji" to "Emoji", "orbs" to "Orbs")
fun lockTypeLabel(t: String) = LockTypes.firstOrNull { it.first == t }?.second ?: "Password"
fun lockTypeHint(t: String) = when (t) {
    "pin" -> "Enter your PIN"
    "pattern" -> "Draw your pattern"
    "draw" -> "Draw your secret sketch"
    "rhythm" -> "Tap your secret rhythm"
    "emoji" -> "Tap your emoji code"
    "orbs" -> "Tap your orb sequence"
    else -> "Enter your password"
}
fun lockTypeBlurb(t: String) = when (t) {
    "pin" -> "4 to 6 digits on a keypad."
    "pattern" -> "Connect at least 4 dots on a 3×3 grid."
    "draw" -> "Sketch any shape or squiggle. It only needs to look about the same."
    "rhythm" -> "Tap a beat only you know. Timing matters, not speed."
    "emoji" -> "Tap 3 to 8 emojis in order."
    "orbs" -> "Tap 4 to 8 colour orbs in order."
    else -> "At least 4 characters."
}

/** One input surface for every lock style. Calls [onComplete] with an encoded secret. Bump [nonce] to clear it. */
@Composable
fun LockInput(type: String, nonce: Int, onComplete: (String) -> Unit, modifier: Modifier = Modifier) {
    val done by rememberUpdatedState(onComplete)
    key(type, nonce) {
        Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            when (type) {
                "pin" -> PinPad { done(it) }
                "pattern" -> PatternPad { done(it) }
                "draw" -> DrawPad { done(it) }
                "rhythm" -> RhythmPad { done(it) }
                "emoji" -> {
                    val emojis = listOf("🌱", "🔥", "🌙", "⭐", "🍀", "🎯", "🦊", "🚀", "💎", "🍕", "🎧", "🌊")
                    SymbolPad(emojis.size, 4, 3, { done(it) }) { i -> Text(emojis[i], fontSize = 28.sp) }
                }
                "orbs" -> {
                    val orbs = listOf(0xFFF26D85, 0xFFF7B733, 0xFF2E8C7F, 0xFF9B8AE0, 0xFF5BB8D9, 0xFF9BDE3A, 0xFFF8873B, 0xFF354BFF).map { Color(it) }
                    SymbolPad(orbs.size, 4, 4, { done(it) }) { i ->
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.radialGradient(listOf(lerp(orbs[i], Color.White, .5f), orbs[i]))))
                    }
                }
                else -> PasswordEntry { done(it) }
            }
        }
    }
}

@Composable
private fun PasswordEntry(onDone: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    PasswordField(pw, { pw = it }, "Password", onDone = { if (pw.isNotEmpty()) onDone(pw) })
    Spacer(Modifier.height(12.dp))
    NeoButton("Continue", Modifier.fillMaxWidth(), onClick = { if (pw.isNotEmpty()) onDone(pw) }, enabled = pw.isNotEmpty())
}

@Composable
private fun PinPad(onDone: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    val tap = rememberTap()
    val primary = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.outline.copy(alpha = .35f)
    fun press(k: String) {
        if (k == "⌫") pin = pin.dropLast(1)
        else if (k == "OK") { if (pin.length >= 4) onDone(pin) }
        else if (pin.length < 6) pin += k
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(6) { i -> Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < pin.length) primary else dim)) }
    }
    Spacer(Modifier.height(18.dp))
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK").chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            row.forEach { k ->
                Box(
                    Modifier.size(66.dp).clip(CircleShape).background(if (k == "OK") primary else MaterialTheme.colorScheme.surfaceVariant).clickable { tap(); press(k) },
                    contentAlignment = Alignment.Center
                ) { Text(k, fontWeight = FontWeight.Black, fontSize = 21.sp, color = if (k == "OK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PatternPad(onDone: (String) -> Unit) {
    val sel = remember { mutableStateListOf<Int>() }
    var pos by remember { mutableStateOf<Offset?>(null) }
    var msg by remember { mutableStateOf("") }
    val cb by rememberUpdatedState(onDone)
    val primary = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = .28f)
    Canvas(
        Modifier.size(280.dp).pointerInput(Unit) {
            fun center(i: Int) = Offset(size.width * ((i % 3) * 2 + 1) / 6f, size.height * ((i / 3) * 2 + 1) / 6f)
            fun hit(p: Offset): Int? = (0..8).firstOrNull { (center(it) - p).getDistance() < size.width / 8f }
            detectDragGestures(
                onDragStart = { p -> sel.clear(); msg = ""; hit(p)?.let { sel.add(it) }; pos = p },
                onDrag = { change, _ -> change.consume(); pos = change.position; hit(change.position)?.let { if (it !in sel) sel.add(it) } },
                onDragEnd = { pos = null; if (sel.size >= 4) cb(sel.joinToString("-")) else { sel.clear(); msg = "Connect at least 4 dots" } },
                onDragCancel = { pos = null; sel.clear() }
            )
        }
    ) {
        val r = size.width / 18f
        fun c(i: Int) = Offset(size.width * ((i % 3) * 2 + 1) / 6f, size.height * ((i / 3) * 2 + 1) / 6f)
        for (k in 0 until sel.size - 1) drawLine(primary, c(sel[k]), c(sel[k + 1]), strokeWidth = r * 0.5f, cap = StrokeCap.Round)
        val pp = pos
        if (sel.isNotEmpty() && pp != null) drawLine(primary.copy(alpha = .6f), c(sel.last()), pp, strokeWidth = r * 0.5f, cap = StrokeCap.Round)
        for (i in 0..8) {
            val on = i in sel
            if (on) drawCircle(primary.copy(alpha = .18f), r * 2.2f, c(i))
            drawCircle(if (on) primary else dim, if (on) r * 1.1f else r * 0.7f, c(i))
        }
    }
    if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun DrawPad(onDone: (String) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var msg by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.size(280.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = .5f), shape)) {
        Canvas(
            Modifier.size(280.dp).pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { p -> current = listOf(p); msg = "" },
                    onDrag = { change, _ -> change.consume(); current = current + change.position },
                    onDragEnd = { if (current.size > 1) strokes.add(current); current = emptyList() },
                    onDragCancel = { current = emptyList() }
                )
            }
        ) {
            val st = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            (strokes.toList() + listOf(current)).forEach { pts ->
                if (pts.size > 1) {
                    val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                    drawPath(path, primary, style = st)
                }
            }
        }
        if (strokes.isEmpty() && current.isEmpty()) Text("Draw here", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f), fontWeight = FontWeight.Bold)
    }
    if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        NeoOutlineButton("Clear", { strokes.clear(); current = emptyList(); msg = "" }, Modifier.weight(1f))
        NeoButton("Done", Modifier.weight(1f), onClick = {
            val code = LockShape.encodeDrawing(strokes.flatten().map { it.x to it.y })
            if (code == null) msg = "Draw a bit more" else onDone(code)
        })
    }
}

@Composable
private fun RhythmPad(onDone: (String) -> Unit) {
    val times = remember { mutableStateListOf<Long>() }
    var msg by remember { mutableStateOf("") }
    val tap = rememberTap()
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(190.dp).clip(CircleShape).background(Brush.radialGradient(listOf(lerp(primary, Color.White, .35f), primary)))
            .clickable { tap(); times.add(android.os.SystemClock.elapsedRealtime()); msg = "" },
        contentAlignment = Alignment.Center
    ) { Text(if (times.isEmpty()) "TAP" else times.size.toString(), fontWeight = FontWeight.Black, fontSize = 34.sp, color = MaterialTheme.colorScheme.onPrimary) }
    Spacer(Modifier.height(10.dp))
    Text(if (times.size < 4) "At least 4 taps" else "${times.size} taps", style = MaterialTheme.typography.labelMedium)
    if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        NeoOutlineButton("Clear", { times.clear(); msg = "" }, Modifier.weight(1f))
        NeoButton("Done", Modifier.weight(1f), onClick = {
            val code = LockShape.encodeRhythm(times.toList())
            if (code == null) msg = "Tap at least 4 times" else onDone(code)
        })
    }
}

@Composable
private fun SymbolPad(count: Int, cols: Int, minLen: Int, onDone: (String) -> Unit, cell: @Composable (Int) -> Unit) {
    val seq = remember { mutableStateListOf<Int>() }
    val tap = rememberTap()
    val primary = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(18.dp)) {
        seq.forEach { _ -> Box(Modifier.size(12.dp).clip(CircleShape).background(primary)) }
    }
    Spacer(Modifier.height(12.dp))
    (0 until count).chunked(cols).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { i ->
                Box(
                    Modifier.size(62.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { tap(); if (seq.size < 8) seq.add(i) },
                    contentAlignment = Alignment.Center
                ) { cell(i) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        NeoOutlineButton("Undo", { if (seq.isNotEmpty()) seq.removeAt(seq.lastIndex) }, Modifier.weight(1f))
        NeoButton("Continue", Modifier.weight(1f), onClick = { if (seq.size >= minLen) onDone(seq.joinToString(",")) }, enabled = seq.size >= minLen)
    }
}
