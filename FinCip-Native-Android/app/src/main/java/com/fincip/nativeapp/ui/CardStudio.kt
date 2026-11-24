package com.fincip.nativeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.bool
import com.fincip.nativeapp.data.double
import com.fincip.nativeapp.data.string
import com.google.gson.JsonObject

/** Everything the identity card shows. Built from real data by the caller, so nothing here is invented. */
data class IdData(
    val name: String, val handle: String, val level: Int, val rank: String, val xpInLevel: Int, val xp: Int,
    val badgeEmojis: List<String>, val badgeCount: Int, val badgeTotal: Int, val entries: Int, val streak: Int, val since: String
)

data class IdLook(
    val style: String = "gradient", val layout: String = "level", val accent: Int = 0, val pattern: String = "circles",
    val shape: String = "round", val showXp: Boolean = true, val showBadges: Boolean = true, val showName: Boolean = true
)

fun idLookOf(raw: JsonObject) = IdLook(
    raw.string("idStyle", "gradient"), raw.string("idLayout", "level"), raw.double("idAccent", 0.0).toInt().coerceIn(0, IdAccents.lastIndex), raw.string("idPattern", "circles"),
    raw.string("idShape", "round"), raw.bool("idShowXp", true), raw.bool("idShowBadges", true), raw.bool("idShowName", true)
)

val IdAccents: List<Pair<String, Color?>> = listOf(
    "Theme" to null, "Forest" to Color(0xFF1F5C3A), "Lime" to Color(0xFFB9F227), "Ocean" to Color(0xFF2D7DD2), "Violet" to Color(0xFF7B4FD6),
    "Coral" to Color(0xFFE0614F), "Gold" to Color(0xFFE8A317), "Rose" to Color(0xFFD6477D), "Teal" to Color(0xFF14A89B), "Slate" to Color(0xFF3A4452)
)
val IdStyles = listOf("gradient" to "Gradient", "dark" to "Midnight", "paper" to "Paper", "outline" to "Outline", "neon" to "Neon", "mono" to "Mono", "aurora" to "Aurora")
val IdLayouts = listOf("level" to "Big level", "stats" to "Stats strip", "badges" to "Badges", "minimal" to "Minimal")
val IdPatterns = listOf("circles" to "Circles", "dots" to "Dots", "waves" to "Waves", "none" to "None")
val IdShapes = listOf("round" to "Rounded", "sharp" to "Sharp", "pill" to "Pill")

private class Pal(val bg: Brush, val fg: Color, val bar: Color, val border: Color?, val borderW: Int = 1)

@Composable
private fun palette(look: IdLook): Pal {
    val scheme = MaterialTheme.colorScheme
    val accent = IdAccents[look.accent].second ?: scheme.primary
    val onAccent = if (accent.luminance() > .5f) Color(0xFF16231B) else Color.White
    return when (look.style) {
        "dark" -> Pal(Brush.verticalGradient(listOf(Color(0xFF1B2124), Color(0xFF0B0E0F))), Color.White, accent, accent.copy(alpha = .4f))
        "paper" -> Pal(Brush.linearGradient(listOf(Color(0xFFF8F1DF), Color(0xFFF1E8CF))), Color(0xFF16231B), accent, Color(0xFF16231B).copy(alpha = .25f))
        "outline" -> Pal(Brush.linearGradient(listOf(scheme.surface, scheme.surface)), scheme.onSurface, accent, accent, 2)
        "neon" -> Pal(Brush.linearGradient(listOf(Color.Black, Color(0xFF0A0A12))), accent, accent, accent, 2)
        "mono" -> Pal(Brush.linearGradient(listOf(Color(0xFF3A3A3A), Color(0xFF0E0E0E))), Color.White, Color.White, null)
        "aurora" -> Pal(Brush.linearGradient(listOf(accent, lerp(accent, Color(0xFF4FD6C2), .6f), lerp(accent, Color(0xFFD64FB0), .6f))), onAccent, onAccent, null)
        else -> Pal(Brush.linearGradient(listOf(accent, lerp(accent, Color(0xFF7B4FD6), .35f), lerp(accent, Color.Black, .5f))), onAccent, onAccent, null)
    }
}

@Composable
private fun StatCol(value: String, label: String, fg: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp, color = fg)
        Text(label, fontSize = 11.sp, color = fg.copy(alpha = .75f))
    }
}

/** The FinCip identity card. Style, layout, accent, pattern, corner shape and which parts show are all user choices. */
@Composable
fun IdentityCard(d: IdData, look: IdLook, modifier: Modifier = Modifier) {
    val pal = palette(look)
    val fg = pal.fg
    val shape = RoundedCornerShape(when (look.shape) { "sharp" -> 10.dp; "pill" -> 44.dp; else -> 30.dp })
    Box(modifier.fillMaxWidth().clip(shape).background(pal.bg).then(if (pal.border != null) Modifier.border(BorderStroke(pal.borderW.dp, pal.border), shape) else Modifier)) {
        Canvas(Modifier.matchParentSize()) {
            val c = fg.copy(alpha = .10f)
            when (look.pattern) {
                "circles" -> { drawCircle(c, 100.dp.toPx(), Offset(size.width - 10.dp.toPx(), 0f)); drawCircle(fg.copy(alpha = .07f), 75.dp.toPx(), Offset(0f, size.height)) }
                "dots" -> { val g = 18.dp.toPx(); var x = g; while (x < size.width) { var y = g; while (y < size.height) { drawCircle(fg.copy(alpha = .14f), 1.6.dp.toPx(), Offset(x, y)); y += g }; x += g } }
                "waves" -> for (i in 0..3) {
                    val path = Path(); val y0 = size.height * (.35f + i * .17f)
                    path.moveTo(0f, y0); path.cubicTo(size.width * .3f, y0 - 30.dp.toPx(), size.width * .6f, y0 + 30.dp.toPx(), size.width, y0 - 10.dp.toPx())
                    drawPath(path, fg.copy(alpha = .12f), style = Stroke(2.dp.toPx()))
                }
            }
        }
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FINCIP IDENTITY", Modifier.weight(1f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = fg.copy(alpha = .8f))
                Text(d.rank.uppercase(), Modifier.clip(RoundedCornerShape(50)).background(fg.copy(alpha = .18f)).padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black, color = fg)
            }
            when (look.layout) {
                "stats" -> {
                    if (look.showName) Text(d.name, fontWeight = FontWeight.Black, fontSize = 24.sp, color = fg, maxLines = 1)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(fg.copy(alpha = .12f)).padding(vertical = 14.dp)) {
                        StatCol(d.level.toString(), "Level", fg, Modifier.weight(1f)); StatCol(d.entries.toString(), "Entries", fg, Modifier.weight(1f)); StatCol("${d.streak}d", "Streak", fg, Modifier.weight(1f))
                    }
                }
                "badges" -> {
                    if (look.showName) Text(d.name, fontWeight = FontWeight.Black, fontSize = 24.sp, color = fg, maxLines = 1)
                    Text("${d.badgeCount} of ${d.badgeTotal} badges unlocked", fontSize = 12.sp, color = fg.copy(alpha = .8f))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (d.badgeEmojis.isEmpty()) Text("Log entries to unlock your first badge", fontSize = 12.sp, color = fg.copy(alpha = .7f))
                        d.badgeEmojis.take(8).forEach { e -> Box(Modifier.size(44.dp).background(fg.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) { Text(e, fontSize = 22.sp) } }
                    }
                }
                "minimal" -> {
                    if (look.showName) Text(d.name, fontWeight = FontWeight.Black, fontSize = 30.sp, color = fg, maxLines = 1, letterSpacing = (-0.5).sp)
                    Text("@${d.handle} · Level ${d.level}" + if (d.since.isNotBlank()) " · since ${d.since}" else "", fontSize = 13.sp, color = fg.copy(alpha = .8f))
                }
                else -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("LV", Modifier.padding(bottom = 10.dp, end = 6.dp), fontWeight = FontWeight.Black, color = fg.copy(alpha = .75f))
                        Text(d.level.toString(), fontSize = 60.sp, fontWeight = FontWeight.Black, color = fg, letterSpacing = (-2).sp, lineHeight = 60.sp)
                    }
                }
            }
            if (look.showXp) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(fg.copy(alpha = .2f))) {
                    Box(Modifier.fillMaxWidth((d.xpInLevel / 100f).coerceIn(0.02f, 1f)).height(8.dp).clip(CircleShape).background(pal.bar))
                }
                Text("${d.xpInLevel} / 100 XP to level ${d.level + 1} · ${d.xp} XP total", fontSize = 12.sp, color = fg.copy(alpha = .85f))
            }
            if (look.showBadges && look.layout != "badges") Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (look.showName && look.layout == "level") Text(d.name, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
                    Text("${d.badgeCount} of ${d.badgeTotal} badges", fontSize = 12.sp, color = fg.copy(alpha = .75f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    d.badgeEmojis.take(4).forEach { e -> Box(Modifier.size(34.dp).background(fg.copy(alpha = .2f), CircleShape).border(2.dp, fg.copy(alpha = .25f), CircleShape), contentAlignment = Alignment.Center) { Text(e, fontSize = 16.sp) } }
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
        content()
    }
}

@Composable
private fun ChipsRow(options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { (k, l) -> PopChip(l, k == selected, { onPick(k) }) } }
}

/** Full-screen studio: live ID-card preview on top, every option below. [apply] writes a key/value into the saved profile. */
@Composable
fun CardStudioDialog(
    data: IdData, look: IdLook, heroLayout: String, onHeroLayout: (String) -> Unit,
    onLook: (IdLook, Map<String, Any>) -> Unit, onChangeBanner: () -> Unit, onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().padding(top = 12.dp), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), color = scheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CARD STUDIO", fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black, color = scheme.onSurface.copy(alpha = .55f))
                        Text("Make your cards yours", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    TextButton(onDismiss) { Text("Done", fontWeight = FontWeight.Black) }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    IdentityCard(data, look)
                    Group("Profile card layout") {
                        ChipsRow(listOf("classic" to "Classic", "cover" to "Full-bleed cover", "centered" to "Centered cover"), heroLayout, onHeroLayout)
                        Text("Cover layouts use your banner (photo or gradient) as the whole card background.", fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f))
                        PopChip("Change banner or photo", false, { onChangeBanner() })
                    }
                    Group("ID card style") { ChipsRow(IdStyles, look.style) { onLook(look.copy(style = it), mapOf("idStyle" to it)) } }
                    Group("ID card layout") { ChipsRow(IdLayouts, look.layout) { onLook(look.copy(layout = it), mapOf("idLayout" to it)) } }
                    Group("Accent colour") {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IdAccents.forEachIndexed { i, (name, c) ->
                                val on = look.accent == i
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onLook(look.copy(accent = i), mapOf("idAccent" to i)) }) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(c ?: scheme.primary).border(if (on) 3.dp else 1.dp, if (on) scheme.onSurface else scheme.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
                                        if (c == null) Text("A", fontWeight = FontWeight.Black, color = scheme.onPrimary)
                                    }
                                    Text(name, fontSize = 10.sp, color = scheme.onSurface.copy(alpha = if (on) 1f else .6f))
                                }
                            }
                        }
                    }
                    Group("Pattern") { ChipsRow(IdPatterns, look.pattern) { onLook(look.copy(pattern = it), mapOf("idPattern" to it)) } }
                    Group("Corners") { ChipsRow(IdShapes, look.shape) { onLook(look.copy(shape = it), mapOf("idShape" to it)) } }
                    Group("Show on card") {
                        listOf(
                            Triple("Name", look.showName, "idShowName"), Triple("XP bar", look.showXp, "idShowXp"), Triple("Badges", look.showBadges, "idShowBadges")
                        ).forEach { (label, on, key) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Switch(on, { v -> onLook(when (key) { "idShowName" -> look.copy(showName = v); "idShowXp" -> look.copy(showXp = v); else -> look.copy(showBadges = v) }, mapOf(key to v)) })
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
