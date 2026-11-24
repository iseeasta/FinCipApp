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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
    val style: String = "gradient", val layout: String = "bank", val accent: Int = 0, val pattern: String = "circles",
    val shape: String = "round", val showXp: Boolean = true, val showBadges: Boolean = true, val showName: Boolean = true,
    val chip: String = "gold", val tap: Boolean = true, val number: String = "stats", val badge: String = "rank"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "idStyle" to style, "idLayout" to layout, "idAccent" to accent, "idPattern" to pattern, "idShape" to shape,
        "idShowXp" to showXp, "idShowBadges" to showBadges, "idShowName" to showName, "idChip" to chip, "idTap" to tap, "idNumber" to number, "idBadge" to badge
    )
}

fun idLookOf(raw: JsonObject) = IdLook(
    raw.string("idStyle", "gradient"), raw.string("idLayout", "bank"), raw.double("idAccent", 0.0).toInt().coerceIn(0, IdAccents.lastIndex), raw.string("idPattern", "circles"),
    raw.string("idShape", "round"), raw.bool("idShowXp", true), raw.bool("idShowBadges", true), raw.bool("idShowName", true),
    raw.string("idChip", "gold"), raw.bool("idTap", true), raw.string("idNumber", "stats"), raw.string("idBadge", "rank")
)

val IdAccents: List<Pair<String, Color?>> = listOf(
    "Theme" to null, "Forest" to Color(0xFF1F5C3A), "Lime" to Color(0xFFB9F227), "Ocean" to Color(0xFF2D7DD2), "Violet" to Color(0xFF7B4FD6),
    "Coral" to Color(0xFFE0614F), "Gold" to Color(0xFFE8A317), "Rose" to Color(0xFFD6477D), "Teal" to Color(0xFF14A89B), "Slate" to Color(0xFF3A4452)
)
val IdStyles = listOf("gradient" to "Gradient", "dark" to "Midnight", "paper" to "Paper", "outline" to "Outline", "neon" to "Neon", "mono" to "Mono", "aurora" to "Aurora",
    "metal" to "Titanium", "gold" to "Gold", "holo" to "Holo", "sunset" to "Sunset", "glass" to "Glass")
val IdChips = listOf("gold" to "Gold chip", "silver" to "Silver chip", "none" to "No chip")
val IdNumbers = listOf("stats" to "Stats code", "handle" to "@handle", "hidden" to "Hidden")
val IdBadges = listOf("rank" to "Rank", "level" to "Level", "none" to "None")
val IdLayouts = listOf("bank" to "Credit card", "level" to "Big level", "stats" to "Stats strip", "badges" to "Badges", "minimal" to "Minimal")
val IdPatterns = listOf("circles" to "Circles", "dots" to "Dots", "waves" to "Waves", "none" to "None")
val IdShapes = listOf("round" to "Rounded", "sharp" to "Sharp", "pill" to "Pill")

val IdPresets: List<Pair<String, IdLook>> = listOf(
    "Classic" to IdLook(style = "gradient", accent = 1, pattern = "circles", chip = "gold"),
    "Black metal" to IdLook(style = "dark", accent = 2, pattern = "waves", chip = "silver", badge = "level"),
    "Gold elite" to IdLook(style = "gold", accent = 6, pattern = "waves", chip = "gold"),
    "Titanium" to IdLook(style = "metal", accent = 9, pattern = "dots", chip = "silver"),
    "Holo" to IdLook(style = "holo", accent = 4, pattern = "circles", chip = "silver"),
    "Sunset" to IdLook(style = "sunset", accent = 5, pattern = "waves", chip = "gold"),
    "Neon night" to IdLook(style = "neon", accent = 2, pattern = "dots", chip = "none"),
    "Glass ocean" to IdLook(style = "glass", accent = 3, pattern = "circles", chip = "silver")
)

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
        "metal" -> Pal(Brush.linearGradient(listOf(Color(0xFFE3E6EA), Color(0xFF8F969F), Color(0xFFCBD0D6), Color(0xFF6E757E))), Color(0xFF1B2024), accent, Color.White.copy(alpha = .6f))
        "gold" -> Pal(Brush.linearGradient(listOf(Color(0xFFF7E58A), Color(0xFFD4A017), Color(0xFFF3D36B), Color(0xFFB8860B))), Color(0xFF3B2A00), Color(0xFF3B2A00), Color.White.copy(alpha = .5f))
        "holo" -> Pal(Brush.linearGradient(listOf(Color(0xFFBDE6FF), Color(0xFFE6C8FF), Color(0xFFFFD3E0), Color(0xFFC9FFE3))), Color(0xFF16231B), accent, Color.White.copy(alpha = .7f))
        "sunset" -> Pal(Brush.linearGradient(listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B), Color(0xFFB65FCF), Color(0xFF5B3FD6))), Color.White, Color.White, null)
        "glass" -> Pal(Brush.linearGradient(listOf(accent.copy(alpha = .85f), lerp(accent, Color.White, .35f).copy(alpha = .6f), accent.copy(alpha = .75f))), onAccent, onAccent, Color.White.copy(alpha = .55f))
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


@Composable
private fun BoxScope.PatternLayer(pattern: String, fg: Color) {
    Canvas(Modifier.matchParentSize()) {
        val c = fg.copy(alpha = .10f)
        when (pattern) {
            "circles" -> { drawCircle(c, 100.dp.toPx(), Offset(size.width - 10.dp.toPx(), 0f)); drawCircle(fg.copy(alpha = .07f), 75.dp.toPx(), Offset(0f, size.height)) }
            "dots" -> { val g = 18.dp.toPx(); var x = g; while (x < size.width) { var y = g; while (y < size.height) { drawCircle(fg.copy(alpha = .14f), 1.6.dp.toPx(), Offset(x, y)); y += g }; x += g } }
            "waves" -> for (i in 0..3) {
                val path = Path(); val y0 = size.height * (.35f + i * .17f)
                path.moveTo(0f, y0); path.cubicTo(size.width * .3f, y0 - 30.dp.toPx(), size.width * .6f, y0 + 30.dp.toPx(), size.width, y0 - 10.dp.toPx())
                drawPath(path, fg.copy(alpha = .12f), style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun EmvChip(kind: String) {
    if (kind == "none") return
    val cols = if (kind == "silver") listOf(Color(0xFFEDEFF2), Color(0xFF9AA0A8)) else listOf(Color(0xFFF4DC8E), Color(0xFFB8902F))
    Canvas(Modifier.size(width = 42.dp, height = 32.dp)) {
        val r = 6.dp.toPx()
        drawRoundRect(Brush.linearGradient(cols), cornerRadius = CornerRadius(r, r))
        val line = Color(0x66000000); val w = 1.dp.toPx()
        drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w)
        drawLine(line, Offset(size.width / 3, 0f), Offset(size.width / 3, size.height), w)
        drawLine(line, Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height), w)
        drawRoundRect(line, Offset(size.width / 3, size.height / 4), Size(size.width / 3, size.height / 2), CornerRadius(4.dp.toPx()), Stroke(w))
    }
}

@Composable
private fun Contactless(color: Color) {
    Canvas(Modifier.size(24.dp)) {
        for (i in 1..3) {
            val r = size.height * i / 3.4f
            drawArc(color.copy(alpha = .9f), -45f, 90f, false, Offset(-r + size.width * .15f, size.height / 2 - r), Size(2 * r, 2 * r), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

private fun cardNumber(d: IdData, mode: String): String = when (mode) {
    "handle" -> "@" + d.handle.uppercase().take(18)
    "hidden" -> ""
    else -> "FNCP %04d %04d %04d".format(d.level.coerceAtMost(9999), d.xp.coerceAtMost(9999), d.entries.coerceAtMost(9999))
}

/** A credit-card styled identity card. Tap it to flip. The number is a decorative stats code (FNCP prefix), never a real card number. */
@Composable
private fun BankCard(d: IdData, look: IdLook, pal: Pal, shape: RoundedCornerShape, modifier: Modifier) {
    var flipped by remember { mutableStateOf(false) }
    val rot by androidx.compose.animation.core.animateFloatAsState(if (flipped) 180f else 0f, androidx.compose.animation.core.tween(550), label = "flip")
    val density = LocalDensity.current.density
    val fg = pal.fg
    Box(
        modifier.fillMaxWidth().aspectRatio(1.586f).graphicsLayer { rotationY = rot; cameraDistance = 14f * density }
            .clip(shape).background(pal.bg).then(if (pal.border != null) Modifier.border(BorderStroke(pal.borderW.dp, pal.border), shape) else Modifier)
            .clickable { flipped = !flipped }
    ) {
        PatternLayer(look.pattern, fg)
        if (rot <= 90f) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FinCipMark(size = 26.dp, glyph = fg)
                    Spacer(Modifier.width(8.dp))
                    Text("FinCip", fontWeight = FontWeight.Black, fontSize = 17.sp, color = fg, letterSpacing = (-0.3).sp)
                    Spacer(Modifier.weight(1f))
                    val badge = when (look.badge) { "rank" -> d.rank.uppercase(); "level" -> "LEVEL ${d.level}"; else -> "" }
                    if (badge.isNotEmpty()) Text(badge, Modifier.clip(RoundedCornerShape(50)).background(fg.copy(alpha = .16f)).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = fg)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EmvChip(look.chip)
                    if (look.tap) Contactless(fg)
                }
                val num = cardNumber(d, look.number)
                if (num.isNotEmpty()) Text(num, fontFamily = FontFamily.Monospace, fontSize = 18.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("CARD HOLDER", fontSize = 8.sp, letterSpacing = 1.2.sp, color = fg.copy(alpha = .7f))
                        Text(if (look.showName) d.name.uppercase() else "FINCIP MEMBER", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, letterSpacing = .6.sp)
                    }
                    if (d.since.isNotBlank()) Column(Modifier.padding(end = 12.dp), horizontalAlignment = Alignment.End) {
                        Text("MEMBER SINCE", fontSize = 8.sp, letterSpacing = 1.2.sp, color = fg.copy(alpha = .7f))
                        Text(d.since.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
                    }
                    Box(Modifier.size(34.dp).background(fg.copy(alpha = .2f), CircleShape).border(1.5.dp, fg.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(d.level.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = fg)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(36.dp).background(Color.Black.copy(alpha = .8f)))
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(32.dp).background(Color.White.copy(alpha = .9f)).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                        Text("@${d.handle}", fontFamily = FontFamily.Cursive, fontSize = 18.sp, color = Color(0xFF16231B), maxLines = 1)
                    }
                    Text(" LV ${d.level}", fontWeight = FontWeight.Black, color = fg, modifier = Modifier.padding(start = 8.dp))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    StatCol(d.entries.toString(), "Entries", fg, Modifier.weight(1f)); StatCol("${d.streak}d", "Streak", fg, Modifier.weight(1f))
                    StatCol(d.xp.toString(), "XP", fg, Modifier.weight(1f)); StatCol("${d.badgeCount}/${d.badgeTotal}", "Badges", fg, Modifier.weight(1f))
                }
                Text("Your FinCip identity · not a payment card · tap to flip back", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 9.sp, color = fg.copy(alpha = .7f))
            }
        }
    }
}

/** The FinCip identity card. Style, layout, accent, pattern, corner shape and which parts show are all user choices. */
@Composable
fun IdentityCard(d: IdData, look: IdLook, modifier: Modifier = Modifier) {
    val pal = palette(look)
    val fg = pal.fg
    val shape = RoundedCornerShape(when (look.shape) { "sharp" -> 10.dp; "pill" -> 44.dp; else -> 30.dp })
    if (look.layout == "bank") { BankCard(d, look, pal, RoundedCornerShape(when (look.shape) { "sharp" -> 10.dp; "pill" -> 36.dp; else -> 22.dp }), modifier); return }
    Box(modifier.fillMaxWidth().clip(shape).background(pal.bg).then(if (pal.border != null) Modifier.border(BorderStroke(pal.borderW.dp, pal.border), shape) else Modifier)) {
        PatternLayer(look.pattern, fg)
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
                Box(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) { IdentityCard(data, look) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Group("Quick looks · one tap") {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IdPresets.forEach { (name, pl) ->
                                val applied = pl.copy(showXp = look.showXp, showBadges = look.showBadges, showName = look.showName)
                                val pal = palette(applied)
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onLook(applied, applied.toMap()) }) {
                                    Box(Modifier.size(width = 92.dp, height = 58.dp).clip(RoundedCornerShape(10.dp)).background(pal.bg).then(if (pal.border != null) Modifier.border(1.dp, pal.border, RoundedCornerShape(10.dp)) else Modifier)) {
                                        Box(Modifier.padding(8.dp).size(width = 16.dp, height = 12.dp).clip(RoundedCornerShape(3.dp)).background(if (pl.chip == "silver") Color(0xFFD0D4D9) else if (pl.chip == "none") Color.Transparent else Color(0xFFE6C766)))
                                    }
                                    Text(name, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp), color = scheme.onSurface.copy(alpha = .8f))
                                }
                            }
                        }
                    }
                    Group("Profile card layout") {
                        ChipsRow(listOf("classic" to "Classic", "cover" to "Full-bleed cover", "centered" to "Centered cover"), heroLayout, onHeroLayout)
                        Text("Cover layouts use your banner (photo or gradient) as the whole card background.", fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f))
                        PopChip("Change banner or photo", false, { onChangeBanner() })
                    }
                    Group("ID card style") { ChipsRow(IdStyles, look.style) { onLook(look.copy(style = it), mapOf("idStyle" to it)) } }
                    Group("Chip") { ChipsRow(IdChips, look.chip) { onLook(look.copy(chip = it), mapOf("idChip" to it)) } }
                    Group("Card number") {
                        ChipsRow(IdNumbers, look.number) { onLook(look.copy(number = it), mapOf("idNumber" to it)) }
                        Text("A decorative code built from your level, XP and entries. It always starts with FNCP so it can never pass for a real card number.", fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f))
                    }
                    Group("Top-right label") { ChipsRow(IdBadges, look.badge) { onLook(look.copy(badge = it), mapOf("idBadge" to it)) } }
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
                            Triple("Name", look.showName, "idShowName"), Triple("XP bar (panel layouts)", look.showXp, "idShowXp"), Triple("Badges (panel layouts)", look.showBadges, "idShowBadges"), Triple("Contactless symbol", look.tap, "idTap")
                        ).forEach { (label, on, key) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Switch(on, { v -> onLook(when (key) { "idShowName" -> look.copy(showName = v); "idShowXp" -> look.copy(showXp = v); "idTap" -> look.copy(tap = v); else -> look.copy(showBadges = v) }, mapOf(key to v)) })
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
