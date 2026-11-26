package com.fincip.nativeapp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as GCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fincip.nativeapp.data.MascotLook
import com.fincip.nativeapp.data.MascotOptions
import com.fincip.nativeapp.data.Opt
import com.google.gson.JsonObject

const val MASCOT_KEY = "fincipMascot"

fun mascotOf(workspace: JsonObject): MascotLook? =
    if (workspace.has(MASCOT_KEY) && workspace.get(MASCOT_KEY).isJsonObject) MascotLook.fromJson(workspace.getAsJsonObject(MASCOT_KEY)) else null

/** Renders the mascot to a bitmap off-screen, used for the pinned home-screen shortcut. */
fun renderMascotBitmap(look: MascotLook, px: Int = 432): android.graphics.Bitmap {
    val img = androidx.compose.ui.graphics.ImageBitmap(px, px)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GCanvas(img), androidx.compose.ui.geometry.Size(px.toFloat(), px.toFloat())) { drawMascot(look, 0f, false, false) }
    return img.asAndroidBitmap()
}

/** Asks the launcher to pin a shortcut with the mascot. The user must confirm; not every launcher supports it. */
fun requestMascotShortcut(context: Context, look: MascotLook): String {
    val sm = context.getSystemService(ShortcutManager::class.java) ?: return "This device can't pin shortcuts."
    if (Build.VERSION.SDK_INT < 26 || !sm.isRequestPinShortcutSupported) return "Your launcher doesn't support pinned shortcuts. The in-app mascot still works."
    val intent = Intent(context, com.fincip.nativeapp.MainActivity::class.java).setAction(Intent.ACTION_MAIN)
    val info = android.content.pm.ShortcutInfo.Builder(context, "fincip_mascot")
        .setShortLabel("FinCip").setLongLabel("FinCip with ${MascotOptions.character(look.character).name}")
        .setIcon(Icon.createWithBitmap(renderMascotBitmap(look))).setIntent(intent).build()
    return if (sm.requestPinShortcut(info, null)) "Confirm the prompt to add it to your home screen." else "The launcher refused the shortcut."
}

private val TABS = listOf("Character", "Outfit", "Hair", "Extras", "Colours", "Backdrop", "Mood", "My FinCip")

@Composable
fun MascotStudioScreen(workspace: JsonObject, petLevel: Int, onSave: (JsonObject.() -> Unit) -> Unit) {
    val context = LocalContext.current
    val tap = rememberTap()
    val saved = remember(workspace) { mascotOf(workspace) }
    var look by remember { mutableStateOf(saved ?: MascotLook.defaultFor(0)) }
    var tab by remember { mutableStateOf(0) }
    var preview by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val dirty = saved == null || saved != look
    val scheme = MaterialTheme.colorScheme
    val unlocked = petLevel >= MascotOptions.PREMIUM_LEVEL

    fun pick(change: MascotLook) { tap(); look = change; note = null }

    Column(Modifier.fillMaxSize()) {
        // ---- live preview, pinned
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFEDE7FF), Color(0xFFD7CBFF)))).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AnimatedContent(look.character, label = "pv") { _ -> MascotView(look, 150.dp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(MascotOptions.character(look.character).name, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF2A1A33))
                    Text(MascotOptions.character(look.character).blurb, fontSize = 12.sp, color = Color(0xFF4B3A66), lineHeight = 16.sp)
                    Text(if (dirty) "Unsaved changes" else "Saved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (dirty) Color(0xFFB4541A) else Color(0xFF2E7D4F))
                }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill("Reset", Modifier.weight(1f)) { pick(MascotLook.defaultFor(look.character).copy(useAsLogo = look.useAsLogo)) }
            ActionPill("Random", Modifier.weight(1f)) { pick(MascotLook.random(look, java.util.Random(), petLevel)) }
            ActionPill("Preview", Modifier.weight(1f)) { tap(); preview = true }
            ActionPill(if (dirty) "Save" else "Saved", Modifier.weight(1f), filled = true) {
                val s = look.sanitized().let { if (unlocked) it else it.forLevel(petLevel) }
                look = s
                onSave { add(MASCOT_KEY, s.toJson()); val p = getAsJsonObject("profile") ?: JsonObject().also { add("profile", it) }; p.addProperty("avatarMode", "mascot") }
                note = "Saved. Your profile, home and empty states now use ${MascotOptions.character(s.character).name}."
            }
        }
        note?.let { Text(it, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), fontSize = 12.sp, color = scheme.primary) }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TABS.forEachIndexed { i, t ->
                val on = tab == i
                val bg by animateColorAsState(if (on) scheme.primary else scheme.surfaceVariant, label = "tab")
                Text(t, Modifier.clip(RoundedCornerShape(50)).background(bg).clickable { tap(); tab = i }.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (on) scheme.onPrimary else scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) {
                0 -> {
                    Text("Pick your companion. Each has its own default style; you can change everything after.", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MascotOptions.characters.take(2).forEach { c -> CharTile(c.id, look, Modifier.weight(1f)) { pick(MascotLook.defaultFor(c.id).copy(useAsLogo = look.useAsLogo)) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MascotOptions.characters.drop(2).forEach { c -> CharTile(c.id, look, Modifier.weight(1f)) { pick(MascotLook.defaultFor(c.id).copy(useAsLogo = look.useAsLogo)) } }
                    }
                }
                1 -> { OptionGrid(MascotOptions.outfits, look.outfit, unlocked, petLevel) { pick(look.copy(outfit = it)) }; ColourRow(look, { pick(it) }) }
                2 -> {
                    Label("Style"); OptionGrid(MascotOptions.hairStyles, look.hair, unlocked, petLevel) { pick(look.copy(hair = it)) }
                    Label("Hair colour"); Swatches(MascotOptions.hairColors.map { it }, MascotOptions.hairColorNames, look.hairColor) { pick(look.copy(hairColor = it)) }
                }
                3 -> {
                    Label("Tap to toggle. Combine as many as you like.")
                    OptionGrid(MascotOptions.accessories, null, unlocked, petLevel, selected = look.accessories) { id -> pick(look.copy(accessories = if (id in look.accessories) look.accessories - id else look.accessories + id)) }
                }
                4 -> ColourRow(look, { pick(it) })
                5 -> OptionGrid(MascotOptions.backgrounds, look.bg, unlocked, petLevel) { pick(look.copy(bg = it)) }
                6 -> OptionGrid(MascotOptions.expressions, look.expression, unlocked, petLevel) { pick(look.copy(expression = it)) }
                else -> IdentityTab(look, context, { pick(it) }) { note = it }
            }
            if (!unlocked && tab in 1..3) Text("Premium items unlock at pet level ${MascotOptions.PREMIUM_LEVEL}. You're level $petLevel. Levels come from real activity: entries, budgets and goals.", fontSize = 12.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(140.dp))
        }
    }
    if (preview) Dialog(onDismissRequest = { preview = false }) {
        var happy by remember { mutableStateOf(false) }
        Column(Modifier.clip(RoundedCornerShape(28.dp)).background(scheme.surface).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MascotView(look, 280.dp, expression = if (happy) "joy" else null)
            Text(if (happy) "Goal reached!" else "Your FinCip companion", fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill("Celebrate", Modifier.width(120.dp)) { happy = !happy }
                ActionPill("Close", Modifier.width(120.dp), filled = true) { preview = false }
            }
        }
    }
}

@Composable private fun Label(t: String) = Text(t, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun ActionPill(text: String, modifier: Modifier, filled: Boolean = false, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    Box(modifier.height(42.dp).clip(RoundedCornerShape(50)).background(if (filled) s.primary else s.surfaceVariant).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (filled) s.onPrimary else s.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CharTile(id: Int, look: MascotLook, modifier: Modifier, onClick: () -> Unit) {
    val on = look.character == id
    val sc by animateFloatAsState(if (on) 1f else .94f, spring(dampingRatio = .55f), label = "sc")
    val c = MascotOptions.character(id)
    Column(modifier.scale(sc).clip(RoundedCornerShape(24.dp)).border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MascotView(MascotLook.defaultFor(id), 120.dp, animated = false)
        Text(c.name, fontWeight = FontWeight.Black)
        Text(c.blurb, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
    }
}

@Composable
private fun OptionGrid(opts: List<Opt>, current: String?, unlocked: Boolean, level: Int, selected: Set<String> = emptySet(), onPick: (String) -> Unit) {
    val s = MaterialTheme.colorScheme
    opts.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { o ->
                val on = o.id == current || o.id in selected
                val locked = o.premium && !unlocked
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(if (on) s.primary else s.surfaceVariant)
                    .clickable(enabled = !locked) { onPick(o.id) }, contentAlignment = Alignment.Center) {
                    Text(if (locked) "${o.title} · Lv$level/${MascotOptions.PREMIUM_LEVEL}" else o.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp), color = if (on) s.onPrimary else if (locked) s.onSurface.copy(alpha = .4f) else s.onSurface)
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}


@Composable
private fun Swatches(colors: List<Long>, names: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEachIndexed { i, c ->
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(c.toInt())).border(if (i == selected) 3.dp else 1.dp, if (i == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape).clickable { onPick(i) })
        }
    }
    Text(names.getOrNull(selected) ?: "Custom", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ColourRow(look: MascotLook, onChange: (MascotLook) -> Unit) {
    Label("Outfit colour")
    Swatches(MascotOptions.colors, MascotOptions.colorNames, look.outfitColor) { onChange(look.copy(outfitColor = it)) }
    Label("Custom mix")
    var hue by remember { mutableStateOf(0.72f) }
    Slider(hue, { hue = it; val argb = android.graphics.Color.HSVToColor(floatArrayOf(it * 360f, .55f, .9f)); onChange(look.copy(outfitColor = -1, customColor = argb.toLong() and 0xFFFFFFFFL)) })
}

@Composable
private fun IdentityTab(look: MascotLook, context: Context, onChange: (MascotLook) -> Unit, say: (String) -> Unit) {
    val s = MaterialTheme.colorScheme
    Text("Customise My FinCip", fontWeight = FontWeight.Black, fontSize = 18.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(true to "Female mascot", false to "Male mascot").forEach { (f, label) ->
            val on = MascotOptions.character(look.character).female == f
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(50)).background(if (on) s.primary else s.surfaceVariant).clickable {
                if (!on) onChange(MascotLook.defaultFor(MascotOptions.characters.first { it.female == f }.id).copy(useAsLogo = look.useAsLogo))
            }, contentAlignment = Alignment.Center) { Text(label, color = if (on) s.onPrimary else s.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
    Text("Outfit, colour, extras and backdrop are on the other tabs; this card shows the identity they build.", fontSize = 12.sp, color = s.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { MascotView(look, 120.dp, animated = false, rounded = true) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("Use as in-app logo", fontWeight = FontWeight.Bold); Text("Replaces the leaf mark inside FinCip only.", fontSize = 12.sp, color = s.onSurfaceVariant) }
        Switch(look.useAsLogo, { onChange(look.copy(useAsLogo = it)) })
    }
    Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(50)).background(s.primary).clickable { say(requestMascotShortcut(context, look)) }, contentAlignment = Alignment.Center) {
        Text("Add mascot shortcut to home screen", color = s.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    Text("The honest limits: Android doesn't let an app redraw its own launcher icon at runtime. What's possible is (1) a pinned home-screen shortcut with your mascot, which you must confirm and which depends on your launcher, or (2) the preset icon colours in Profile > App icon. The installed app icon itself stays as built. Save before pinning so the shortcut matches.", fontSize = 12.sp, color = s.onSurfaceVariant, lineHeight = 16.sp)
}
