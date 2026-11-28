package com.fincip.nativeapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.Social
import com.google.gson.JsonObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TILT = 0.58f

/** The user is the sun. Everyone they're connected to is a planet: more in common means a closer orbit. */
@Composable
fun UniverseScreen(workspace: JsonObject, cloud: JsonObject, level: Int, onOpenSocial: () -> Unit) {
    val signedIn = cloud.string("url").isNotBlank() && cloud.string("userId").isNotBlank() && cloud.string("accessToken").isNotBlank()
    val social = workspace.objectOrEmpty("social")
    val shareSig = !social.has("shareSignature") || social.get("shareSignature").asBoolean
    val mySig = remember(workspace, level) { Social.signature(workspace, level) }
    val myName = workspace.objectOrEmpty("profile").string("name").ifBlank { "You" }
    val state = produceState<Result<SocialData>?>(null, signedIn, shareSig) { value = if (signedIn) runCatching { loadSocial(cloud, workspace, level, shareSig) } else null }
    val people = state.value?.getOrNull()?.others ?: emptyList()
    val d = state.value?.getOrNull()
    val sims = remember(people, mySig) { people.associate { it.id to Social.similarity(mySig, it.sig) } }
    val orbits = remember(people, sims) { Social.orbits(people.map { it.id to (sims[it.id] ?: 0.0) }, 1.0, 3.2, 0.5) }
    var t by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var picked by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { var last = 0L; while (true) { androidx.compose.runtime.withFrameNanos { n -> if (n - last > 28_000_000) { t = n / 1e9f; last = n } } } }
    val stars = remember { (0 until 140).map { i -> Triple(((i * 7919) % 1000) / 1000f, ((i * 104729) % 1000) / 1000f, 0.4f + ((i * 31) % 10) / 10f) } }

    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))) {
        var cw by remember { mutableFloatStateOf(1f) }; var ch by remember { mutableFloatStateOf(1f) }
        Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, p, z, _ -> zoom = (zoom * z).coerceIn(.55f, 3f); pan += p } }
            .pointerInput(orbits, zoom, pan) {
                detectTapGestures { tapAt ->
                    val c = Offset(cw / 2, ch / 2) + pan; val maxR = (orbits.maxOfOrNull { it.radius } ?: 1.0).toFloat(); val unit = min(cw, ch) * .44f / maxR * zoom
                    picked = orbits.minByOrNull { o -> val (x, y) = planetPos(o, t, unit); hypot(c.x + x - tapAt.x, c.y + y - tapAt.y) }?.takeIf { o -> val (x, y) = planetPos(o, t, unit); hypot(c.x + x - tapAt.x, c.y + y - tapAt.y) < 60f }?.id
                }
            }) {
            cw = size.width; ch = size.height
            drawSky(t, stars)
            val c = Offset(size.width / 2, size.height / 2) + pan
            val maxR = (orbits.maxOfOrNull { it.radius } ?: 1.0).toFloat(); val unit = min(size.width, size.height) * .44f / maxR * zoom
            orbits.forEach { o -> drawOval(Color.White.copy(alpha = .13f), Offset(c.x - o.radius.toFloat() * unit, c.y - o.radius.toFloat() * unit * TILT), Size(o.radius.toFloat() * unit * 2, o.radius.toFloat() * unit * 2 * TILT), style = Stroke(1.4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 12f)))) }
            val ordered = orbits.sortedBy { planetPos(it, t, unit).second }
            val sunR = 46f * zoom.coerceIn(.7f, 1.6f)
            ordered.filter { planetPos(it, t, unit).second < 0 }.forEach { o -> drawPlanet(people.first { it.id == o.id }, o, c, planetPos(o, t, unit), t, zoom, picked == o.id, d?.followers?.contains(o.id) == true && d.following.contains(o.id)) }
            drawSun(c, sunR, t)
            ordered.filter { planetPos(it, t, unit).second >= 0 }.forEach { o -> drawPlanet(people.first { it.id == o.id }, o, c, planetPos(o, t, unit), t, zoom, picked == o.id, d?.followers?.contains(o.id) == true && d.following.contains(o.id)) }
            drawLabel(myName, c.x, c.y + sunR + 26f, Color(0xFFFFE9A8), 30f)
            if (zoom > .8f) ordered.forEach { o -> val (x, y) = planetPos(o, t, unit); val p = people.first { it.id == o.id }; drawLabel(p.name.substringBefore(' '), c.x + x, c.y + y + 34f * zoom.coerceIn(.8f, 1.6f), Color.White.copy(alpha = .9f), 24f) }
        }
        Column(Modifier.align(Alignment.TopStart).padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 12.dp + LocalNavTop.current)) {
            Text("My universe", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(when { !signedIn -> "Connect Supabase to see your friends orbit"; state.value == null -> "Loading…"; state.value?.isFailure == true -> "Couldn't load friends"; people.isEmpty() -> "Just you for now. Scan a friend's QR to add the first planet." else -> "${people.size} planets · closer means more in common" }, color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
        }
        val sel = people.firstOrNull { it.id == picked }
        if (sel != null) Column(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 16.dp + LocalNavInset.current).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xE61B1140)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val sim = sims[sel.id] ?: 0.0
            Text("${sel.name}  @${sel.handle}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text("Level ${sel.level} · ${(sim * 100).toInt()}% in common" + if (d?.following?.contains(sel.id) == true && d.followers.contains(sel.id)) " · friends" else "", color = Color(0xFFFFD9F0), fontSize = 13.sp)
            val common = Social.commonGround(mySig, sel.sig)
            Text(if (common.isEmpty()) (if (sel.sig.cats.isEmpty()) "They keep their signature private, so distance is a rough guess." else "Not much overlap yet.") else common.joinToString(" · "), color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
        }
        if (signedIn && people.isEmpty() && state.value != null) Text("Open Social to scan", Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp + LocalNavInset.current).clip(RoundedCornerShape(50)).background(Color(0xFF7C5CFF)).padding(horizontal = 20.dp, vertical = 12.dp).let { it }.then(Modifier.pointerInput(Unit) { detectTapGestures { onOpenSocial() } }), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun planetPos(o: Social.Orbit, t: Float, unit: Float): Pair<Float, Float> {
    val a = o.angle0.toFloat() + t * o.speed.toFloat()
    return (cos(a) * o.radius.toFloat() * unit) to (sin(a) * o.radius.toFloat() * unit * TILT)
}

private fun DrawScope.drawLabel(text: String, x: Float, y: Float, color: Color, px: Float) {
    drawContext.canvas.nativeCanvas.drawText(text, x, y, android.graphics.Paint().apply { this.color = color.toArgb(); textSize = px; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true; isFakeBoldText = true; setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(200, 20, 10, 60)) })
}

private fun DrawScope.drawSky(t: Float, stars: List<Triple<Float, Float, Float>>) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0E0A33), Color(0xFF2A1573), Color(0xFF5B2A8F), Color(0xFFB04A9C)), 0f, size.height))
    listOf(Triple(.2f, .25f, Color(0xFF7C5CFF)), Triple(.8f, .6f, Color(0xFFFF6FB5)), Triple(.45f, .85f, Color(0xFF4DB8FF))).forEach { (x, y, c) -> drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .28f), Color.Transparent), Offset(size.width * x, size.height * y), size.minDimension * .55f), size.minDimension * .55f, Offset(size.width * x, size.height * y)) }
    stars.forEachIndexed { i, (x, y, b) ->
        val tw = .5f + .5f * sin(t * (1.2f + b) + i)
        val p = Offset(x * size.width, y * size.height); val r = (1.2f + b * 1.6f)
        if (i % 7 == 0) { val k = r * 3.2f * (.6f + .4f * tw); drawLine(Color.White.copy(alpha = .8f * tw), p - Offset(k, 0f), p + Offset(k, 0f), 1.6f); drawLine(Color.White.copy(alpha = .8f * tw), p - Offset(0f, k), p + Offset(0f, k), 1.6f) }
        else drawCircle(Color.White.copy(alpha = .35f + .55f * tw), r, p)
    }
    val ph = (t * .12f) % 1f
    if (ph < .12f) { val k = ph / .12f; val a = Offset(size.width * (1.0f - k * 1.1f), size.height * (.05f + k * .3f)); drawLine(Brush.linearGradient(listOf(Color.White, Color.Transparent), a, a + Offset(90f, -50f)), a, a + Offset(90f, -50f), 3f) }
}

private fun DrawScope.drawSun(c: Offset, r: Float, t: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0x88FFB347), Color.Transparent), c, r * 3.2f), r * 3.2f, c)
    rotate(t * 6f, c) { for (i in 0 until 14) { val a = i * 2 * PI.toFloat() / 14; val len = if (i % 2 == 0) r * 1.75f else r * 1.4f; val w = .13f
        drawPath(Path().apply { moveTo(c.x + cos(a - w) * r * .9f, c.y + sin(a - w) * r * .9f); lineTo(c.x + cos(a) * len, c.y + sin(a) * len); lineTo(c.x + cos(a + w) * r * .9f, c.y + sin(a + w) * r * .9f); close() }, if (i % 2 == 0) Color(0xFFFFD36B) else Color(0xFFFFA94D)) } }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6C2), Color(0xFFFFD052), Color(0xFFFF9A3D)), c - Offset(r * .25f, r * .3f), r * 1.4f), r, c)
    drawCircle(Color(0xFFE5742B), r, c, style = Stroke(3f))
    drawArc(Color.White.copy(alpha = .75f), 205f, 55f, false, c - Offset(r * .8f, r * .8f), Size(r * 1.6f, r * 1.6f), style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    // face: happy closed eyes, blush, smile
    for (s in listOf(-1f, 1f)) { drawArc(Color(0xFF7A2E12), 200f, 140f, false, Offset(c.x + s * r * .36f - r * .15f, c.y - r * .12f), Size(r * .3f, r * .26f), style = Stroke(3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)); drawOval(Color(0xFFFF7A8A).copy(alpha = .55f), Offset(c.x + s * r * .55f - r * .14f, c.y + r * .12f), Size(r * .28f, r * .16f)) }
    drawArc(Color(0xFF7A2E12), 20f, 140f, false, Offset(c.x - r * .17f, c.y + r * .05f), Size(r * .34f, r * .26f), style = Stroke(3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

private fun hueOf(id: String) = (id.hashCode().toLong() and 0x7fffffff).toInt() % 360

private fun DrawScope.drawPlanet(p: SProfile, o: Social.Orbit, c: Offset, pos: Pair<Float, Float>, t: Float, zoom: Float, selected: Boolean, mutual: Boolean) {
    val h = hueOf(p.id); val r = (16f + min(p.level, 15) * .9f) * zoom.coerceIn(.7f, 1.7f)
    val ctr = Offset(c.x + pos.first, c.y + pos.second)
    val base = Color.hsv(h.toFloat(), .5f, .95f); val dark = Color.hsv(h.toFloat(), .75f, .55f); val light = Color.hsv(h.toFloat(), .2f, 1f)
    val ring = (h / 7) % 3 == 0
    if (selected) drawCircle(Color.White.copy(alpha = .25f), r * 1.9f, ctr, style = Stroke(3f))
    if (ring) drawOval(light.copy(alpha = .8f), Offset(ctr.x - r * 1.8f, ctr.y - r * .5f), Size(r * 3.6f, r), style = Stroke(r * .16f))
    drawCircle(Brush.radialGradient(listOf(light, base, dark), ctr - Offset(r * .35f, r * .4f), r * 1.5f), r, ctr)
    // cel shading: shadow crescent away from the sun
    val away = Offset(ctr.x - c.x, ctr.y - c.y).let { v -> val l = hypot(v.x, v.y).coerceAtLeast(1f); Offset(v.x / l, v.y / l) }
    drawCircle(dark.copy(alpha = .45f), r, ctr, style = Stroke(r * .35f))
    drawArc(Color.White.copy(alpha = .7f), 200f, 50f, false, ctr - Offset(r * .72f, r * .72f), Size(r * 1.44f, r * 1.44f), style = Stroke(r * .12f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawCircle(Color(0xFF1A0F3D), r, ctr, style = Stroke(2f))
    if (ring) drawArc(light.copy(alpha = .9f), 0f, 180f, false, Offset(ctr.x - r * 1.8f, ctr.y - r * .5f), Size(r * 3.6f, r), style = Stroke(r * .16f))
    // little face
    val ey = ctr.y - r * .05f
    for (s in listOf(-1f, 1f)) { drawCircle(Color(0xFF2A1A4D), r * .09f, Offset(ctr.x + s * r * .32f, ey)); drawCircle(Color.White, r * .035f, Offset(ctr.x + s * r * .34f, ey - r * .03f)); drawOval(Color(0xFFFF8FB0).copy(alpha = .6f), Offset(ctr.x + s * r * .5f - r * .1f, ey + r * .14f), Size(r * .22f, r * .12f)) }
    drawArc(Color(0xFF2A1A4D), 20f, 140f, false, Offset(ctr.x - r * .12f, ey + r * .08f), Size(r * .24f, r * .18f), style = Stroke(r * .05f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    if (p.level >= 8) { val a = t * 2.4f + o.angle0.toFloat(); drawCircle(Color(0xFFE9E4F5), r * .22f, Offset(ctr.x + cos(a) * r * 1.6f, ctr.y + sin(a) * r * .8f)) }
    if (mutual) { val k = r * .42f; val hc = Offset(ctr.x + r * .95f, ctr.y - r * .95f); drawCircle(Color(0xFFFF5C8A), k * .5f, hc - Offset(k * .28f, 0f)); drawCircle(Color(0xFFFF5C8A), k * .5f, hc + Offset(k * .28f, 0f)); drawPath(Path().apply { moveTo(hc.x - k * .72f, hc.y + k * .1f); lineTo(hc.x, hc.y + k * .85f); lineTo(hc.x + k * .72f, hc.y + k * .1f); close() }, Color(0xFFFF5C8A)) }
}
