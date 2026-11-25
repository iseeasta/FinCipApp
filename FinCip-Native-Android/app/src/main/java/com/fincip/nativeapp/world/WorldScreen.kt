package com.fincip.nativeapp.world

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.fincip.nativeapp.buildPetCtx
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.ui.PopChip
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val GROUND = 480f
private const val UNIT = .75f

/** Everything that changes per frame lives here as plain fields, so only the canvas redraws, not the whole screen. */
private class Sim(startRoom: String, startAct: Activity) {
    var room = startRoom; var act = startAct
    var x = startAct.anchorX; var facing = 1
    var mode = "do"; var left = 30f; var targetX = x; var toDoor = false
    var fadeDir = 0; var fade = 0f; var pendingRoom = startRoom
    var t = 0f; var blinkIn = 3f; var blinkT = 0f
    var react = ""; var reactLeft = 0f
    var lookId = "default"; var sparkle = 0f
    var speech = ""; var speechLeft = 0f
    var camX = startAct.anchorX; var zoom = 1f; var freeUntil = 0f
    var sc = 1f; var tx = 0f; var ty = 0f; var vw = 1f; var vh = 1f

    fun go(a: Activity, instant: Boolean) {
        act = a; react = ""; reactLeft = 0f
        if (a.room == room && !instant) { mode = "walk"; targetX = a.anchorX; toDoor = false }
        else if (instant) { pendingRoom = a.room; fadeDir = 1; toDoor = false; mode = "walk"; targetX = a.anchorX }
        else { mode = "walk"; targetX = Rooms.of(room).doorX; toDoor = true; pendingRoom = a.room }
    }

    fun say(s: String, sec: Float = 4.5f) { speech = s; speechLeft = sec }

    fun step(dt: Float, hour: Double, weather: String, outfit: String, month: Int, rng: Random) {
        t += dt
        blinkIn -= dt; if (blinkIn < 0f) { blinkT = .15f; blinkIn = 2f + rng.nextFloat() * 4f }
        if (blinkT > 0f) blinkT -= dt
        if (speechLeft > 0f) { speechLeft -= dt; if (speechLeft <= 0f) speech = "" }
        if (reactLeft > 0f) reactLeft -= dt
        if (fadeDir != 0) {
            fade += fadeDir * dt * 3.2f
            if (fadeDir > 0 && fade >= 1f) { fade = 1f; room = pendingRoom; x = Rooms.of(room).doorX + 40f; camX = x; fadeDir = -1; mode = "walk"; targetX = act.anchorX }
            else if (fadeDir < 0 && fade <= 0f) { fade = 0f; fadeDir = 0 }
        } else if (mode == "walk") {
            val d = targetX - x; val stepPx = 150f * dt
            if (abs(d) <= stepPx) {
                x = targetX
                if (toDoor) { toDoor = false; fadeDir = 1 } else { mode = "do"; left = Routine.durationSec(act, rng).toFloat(); facing = 1 }
            } else { x += if (d > 0) stepPx else -stepPx; facing = if (d > 0) 1 else -1 }
        } else {
            left -= dt
            if (left <= 0f) go(Routine.next(act.id, hour, weather, rng), false)
        }
        val want = if (outfit == "auto") Looks.autoFor(act.id, weather, room == "garden", hour, month, 0.0) else outfit
        if (want != lookId) { lookId = want; sparkle = 1.2f }
        if (sparkle > 0f) sparkle -= dt
        if (t > freeUntil) camX += (x - camX) * min(1f, dt * 3f)
    }

    fun pose(): Pose = when {
        reactLeft > 0f -> Poses.at(react, t, false)
        mode == "walk" && fadeDir == 0 -> Poses.at("", t, true)
        else -> Poses.at(act.pose, t, false)
    }
}

private val generic = listOf("Hi! I'm FinKip.", "Hehe, that tickles!", "Tiny steps still count. Even tiny savings.", "I'm just vibing in here.", "Want to see my other outfits?", "Today feels like a good day to check the budget.")

@Composable
fun WorldScreen(workspace: JsonObject, onSave: (JsonObject.() -> Unit) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = remember { workspace.objectOrEmpty("world") }
    var outfit by remember { mutableStateOf(saved.string("outfit", "auto").let { id -> if (id == "auto" || Looks.all.any { it.id == id }) id else "auto" }) }
    var timeMode by remember { mutableStateOf(saved.string("time", "auto")) }
    var weatherMode by remember { mutableStateOf(saved.string("weather", "auto")) }
    var tab by remember { mutableStateOf("rooms") }
    val rng = remember { Random(System.nanoTime()) }
    val nowHour = { LocalTime.now().let { it.hour + it.minute / 60.0 } }
    fun hourOf(mode: String): Double = when (mode) { "morning" -> 8.0; "noon" -> 13.0; "evening" -> 19.0; "night" -> 23.5; else -> nowHour() }
    val sim = remember {
        val h = hourOf(timeMode); val w = if (weatherMode == "auto") Weather.auto(LocalDate.now(), h.toInt()) else weatherMode
        Sim("bedroom", Routine.next(null, h, w, rng)).also { it.room = it.act.room; it.x = it.act.anchorX; it.camX = it.x; it.left = Routine.durationSec(it.act, rng).toFloat(); it.lookId = if (outfit == "auto") Looks.autoFor(it.act.id, w, it.room == "garden", h, LocalDate.now().monthValue, 0.0) else outfit }
    }
    val frame = remember { mutableIntStateOf(0) }
    var uiRoom by remember { mutableStateOf(sim.room) }
    var uiAct by remember { mutableStateOf(sim.act.label) }
    var uiSpeech by remember { mutableStateOf("") }
    var uiClock by remember { mutableStateOf("") }
    val layer = rememberGraphicsLayer()

    fun hour() = hourOf(timeMode)
    fun weather() = if (weatherMode == "auto") Weather.auto(LocalDate.now(), hour().toInt()) else weatherMode
    fun persist() = onSave { addProperty("outfit", outfit); addProperty("time", timeMode); addProperty("weather", weatherMode) }

    LaunchedEffect(timeMode, weatherMode) { if (sim.t > 1f) sim.go(Routine.next(sim.act.id, hour(), weather(), rng), false) }
    LaunchedEffect(Unit) {
        var last = 0L; var clockT = 0f
        while (true) {
            androidx.compose.runtime.withFrameNanos { n ->
                if (last == 0L) last = n
                val dt = (n - last) / 1_000_000_000f
                if (dt >= .032f) {
                    last = n
                    sim.step(min(dt, .1f), hour(), weather(), outfit, LocalDate.now().monthValue, rng)
                    frame.intValue = frame.intValue + 1
                    if (uiRoom != sim.room) uiRoom = sim.room
                    if (uiAct != sim.act.label) uiAct = sim.act.label
                    if (uiSpeech != sim.speech) uiSpeech = sim.speech
                    clockT += dt
                    if (clockT > 5f || uiClock.isEmpty()) { clockT = 0f; val h = hour(); val w = weather(); uiClock = "%02d:%02d · %s".format(h.toInt(), ((h % 1.0) * 60).toInt(), when (w) { Weather.RAIN -> "rain"; Weather.SNOW -> "snow"; Weather.CLOUDY -> "cloudy"; else -> "clear" }) }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(22.dp))) {
            Canvas(
                Modifier.fillMaxSize()
                    .drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            sim.zoom = (sim.zoom * zoom).coerceIn(1f, 2.6f)
                            sim.camX -= pan.x / sim.sc; sim.freeUntil = sim.t + 4f
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { sim.zoom = if (sim.zoom > 1.2f) 1f else 1.8f }) { p ->
                            val lx = (p.x - sim.tx) / sim.sc; val ly = (p.y - sim.ty) / sim.sc
                            if (abs(lx - sim.x) < 60f && ly in 220f..490f) {
                                if (sim.act.id == "sleep" && sim.mode == "do") sim.say("zzz… five more minutes…")
                                else {
                                    sim.react = Poses.reactions.random(rng); sim.reactLeft = 2.4f
                                    val fin = if (rng.nextInt(100) < 40) runCatching { com.fincip.nativeapp.data.PetBrain.lines(buildPetCtx(workspace, "FinKip", 0)) }.getOrNull().orEmpty() else emptyList()
                                    sim.say((if (fin.isNotEmpty()) fin else generic).random(rng))
                                }
                            } else RoomArt.props(sim.room).firstOrNull { it.hit(lx, ly) }?.let { pr ->
                                sim.say(pr.line)
                                if (pr.activity.isNotEmpty() && sim.act.id != "sleep" || pr.activity == "sleep") if (pr.activity.isNotEmpty()) sim.go(Activities.of(pr.activity), false)
                            }
                        }
                    }
            ) {
                frame.intValue
                val h = hour(); val w = weather()
                sim.vw = size.width; sim.vh = size.height
                val sc = size.height / RoomArt.H * sim.zoom
                val half = size.width / sc / 2f
                val cx = if (half * 2f >= Rooms.W) Rooms.W / 2f else sim.camX.coerceIn(half, Rooms.W - half)
                val halfH = size.height / sc / 2f
                val cy = if (halfH * 2f >= RoomArt.H) RoomArt.H / 2f else (330f).coerceIn(halfH, RoomArt.H - halfH)
                sim.sc = sc; sim.tx = size.width / 2f - cx * sc; sim.ty = size.height / 2f - cy * sc
                withTransform({ translate(sim.tx, sim.ty); scale(sc, sc, Offset.Zero) }) {
                    with(RoomArt) { drawRoom(sim.room, h, w, sim.t) }
                    drawGirl(Looks.of(sim.lookId), sim.pose(), sim.x, GROUND, UNIT, sim.facing, sim.t, sim.blinkT > 0f)
                    if (sim.sparkle > 0f) for (i in 0 until 10) {
                        val f = 1.2f - sim.sparkle
                        val a = i * .63f + f * 2f
                        drawCircle(Color(0xFFFFE9A0).copy(alpha = (sim.sparkle / 1.2f).coerceIn(0f, 1f)), 4f + 3f * sin(f * 9f + i), Offset(sim.x + 70f * kotlin.math.cos(a), GROUND - 130f - f * 50f + 50f * sin(a)))
                    }
                    with(RoomArt) { drawRoomLight(sim.room, h, w) }
                }
                if (sim.fade > 0f) drawRect(Color(0xFFFFE3EC).copy(alpha = sim.fade), Offset.Zero, size)
            }
            if (uiSpeech.isNotEmpty()) Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 56.dp, end = 56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .94f)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(uiSpeech, color = Color(0xFF3B2A33), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .38f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("$uiClock · $uiAct", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniBtn("＋") { sim.zoom = (sim.zoom * 1.25f).coerceAtMost(2.6f) }
                MiniBtn("－") { sim.zoom = (sim.zoom / 1.25f).coerceAtLeast(1f) }
                MiniBtn("◎") { sim.zoom = 1f; sim.freeUntil = 0f }
                MiniBtn("📷") {
                    scope.launch {
                        runCatching {
                            val bmp: Bitmap = layer.toImageBitmap().asAndroidBitmap()
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { Toast.makeText(ctx, "Screenshots need Android 10 or newer.", Toast.LENGTH_LONG).show(); return@runCatching }
                            val v = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "FinKip-World-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FinKip") }
                            val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: error("no uri")
                            ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            Toast.makeText(ctx, "Saved to Pictures/FinKip", Toast.LENGTH_SHORT).show()
                        }.onFailure { Toast.makeText(ctx, "Couldn't save the screenshot.", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("rooms" to "Rooms", "outfit" to "Outfit", "do" to "Activity", "sky" to "Sky", "day" to "Her day").forEach { (id, label) -> PopChip(label, tab == id, { tab = id }) }
        }
        when (tab) {
            "rooms" -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rooms.all.forEach { r -> PopChip("${r.emoji} ${r.title}", uiRoom == r.id, { if (r.id != sim.room) sim.go(Activities.defaultFor(r.id), true) }) }
            }
            "outfit" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PopChip("Auto", outfit == "auto", { outfit = "auto"; persist() })
                    Looks.all.forEach { l -> PopChip(l.label, outfit == l.id, { outfit = l.id; persist() }) }
                }
                Text(if (outfit == "auto") "Auto: she changes by activity and weather (pyjamas to sleep, sporty to exercise, a coat in the cold)." else "Fixed outfit. Switch to Auto to let her choose.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
            }
            "do" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Activities.all.chunked(4).forEach { row -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { a -> PopChip(a.label, uiAct == a.label, { sim.go(a, false) }) } } }
            }
            "sky" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto" to "Real time", "morning" to "Morning", "noon" to "Noon", "evening" to "Evening", "night" to "Night").forEach { (id, l) -> PopChip(l, timeMode == id, { timeMode = id; persist() }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Weather.choices.forEach { (id, l) -> PopChip(l, weatherMode == id, { weatherMode = id; persist() }) }
                }
                Text("Real time follows your phone clock. Auto weather is a made-up daily pattern, not a real forecast.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Routine.outline.forEach { (t, d) -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Text(t, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp)); Text(d, fontSize = 13.sp) } }
                Text("Right now: $uiAct", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text("Tap her or the furniture. Drag to pan, pinch or double-tap to zoom.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f), modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
    }
}

@Composable
private fun MiniBtn(label: String, onClick: () -> Unit) {
    Box(Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .85f)).clickable(onClick = onClick).padding(horizontal = 11.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF3B2A33))
    }
}
