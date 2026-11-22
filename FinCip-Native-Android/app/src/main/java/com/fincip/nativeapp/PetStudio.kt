package com.fincip.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.arrayOrEmpty
import com.fincip.nativeapp.data.bool
import com.fincip.nativeapp.data.double
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.objects
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.transactions
import com.fincip.nativeapp.ui.NeoCard
import com.fincip.nativeapp.ui.PetCharacter
import com.fincip.nativeapp.ui.PetLook
import com.fincip.nativeapp.ui.PetMood
import com.fincip.nativeapp.ui.PetOptions
import com.fincip.nativeapp.ui.PetStageNames
import com.fincip.nativeapp.ui.PopChip
import com.fincip.nativeapp.ui.PopField
import com.fincip.nativeapp.ui.ProgressBar
import com.fincip.nativeapp.ui.nextStageLevel
import com.fincip.nativeapp.ui.petStage
import com.fincip.nativeapp.ui.rememberTap
import com.google.gson.JsonObject

data class PetProgress(val xp: Int, val level: Int, val xpInLevel: Int)

/** XP only comes from real activity: entries, budgets, goals, products, and a bonus for a reached goal. */
fun petProgress(workspace: JsonObject): PetProgress {
    val achieved = workspace.arrayOrEmpty("goals").objects().any { it.string("type") != "debt" && it.double("target") > 0 && it.double("saved") >= it.double("target") }
    val xp = transactions(workspace).size * 10 + workspace.arrayOrEmpty("budgets").size() * 25 + workspace.arrayOrEmpty("goals").size() * 25 +
        workspace.arrayOrEmpty("financeProducts").size() * 25 + (if (achieved) 200 else 0)
    return PetProgress(xp, xp / 100 + 1, xp % 100)
}

fun petLookOf(m: JsonObject): PetLook {
    fun i(key: String, def: Int) = if (m.has(key) && !m.get(key).isJsonNull) m.double(key).toInt() else def
    return PetLook(
        girl = m.string("gender", "girl") != "boy",
        skin = i("skin", 1), hairStyle = i("hairStyle", 0), hairColor = i("hairColor", 1), outfitStyle = i("outfitStyle", 0),
        outfitColor = i("outfitColor", 0), accessory = i("accessory", 0), background = i("background", 0), eyes = i("eyes", 0),
        blush = if (m.has("blush")) m.bool("blush") else true
    )
}

/** Home-screen companion card. Reads the same customisation as the studio, so edits show up here instantly. */
@Composable
fun PetHomeCard(workspace: JsonObject, mood: PetMood, moodText: String, streak: Int, onClick: () -> Unit) {
    val pet = workspace.objectOrEmpty("mascot")
    val look = petLookOf(pet)
    val prog = petProgress(workspace)
    val stage = petStage(prog.level)
    val tap = rememberTap()
    val bg = PetOptions.backgrounds[look.background.coerceIn(0, PetOptions.backgrounds.lastIndex)]
    val fg = if (bg.red * 0.3f + bg.green * 0.59f + bg.blue * 0.11f > 0.5f) Color(0xFF16231B) else Color(0xFFF4F4F2)
    NeoCard(fill = bg) {
        Row(Modifier.clickable { tap(); onClick() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PetCharacter(look, mood, prog.level, diameter = 128.dp, showBackground = false)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pet.string("name", "Mochi"), fontWeight = FontWeight.Black, fontSize = 21.sp, color = fg, maxLines = 1)
                    Text("LVL ${prog.level}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = bg, modifier = Modifier.clip(RoundedCornerShape(50)).background(fg).padding(horizontal = 9.dp, vertical = 4.dp))
                }
                Text("${PetStageNames[stage]} stage", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = fg.copy(alpha = .75f))
                Text(moodText, color = fg.copy(alpha = .9f), style = MaterialTheme.typography.bodySmall, maxLines = 3)
                ProgressBar(prog.xpInLevel / 100f, fg)
                val next = nextStageLevel(prog.level)
                Text(
                    "${prog.xpInLevel} / 100 XP · " + (if (streak > 0) "$streak-day streak" else "log something to start a streak") + (if (next != null) " · grows at Lv $next" else ""),
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg
                )
            }
        }
    }
}

@Composable
private fun StudioLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
}

@Composable
private fun Swatches(colors: List<Color>, selected: Int, onPick: (Int) -> Unit) {
    val tap = rememberTap()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEachIndexed { i, c ->
            val on = i == selected
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(c)
                    .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { tap(); onPick(i) }
            )
        }
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: Int, onPick: (Int) -> Unit, locked: (Int) -> String? = { null }) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val lock = locked(i)
            PopChip(if (lock != null) "$label · $lock" else label, i == selected, { if (lock == null) onPick(i) })
        }
    }
}

/** Full companion customiser. [onChange] applies a mutation to the saved mascot object, so Home updates at once. */
@Composable
fun PetCustomizer(workspace: JsonObject, onChange: (JsonObject.() -> Unit) -> Unit) {
    val pet = workspace.objectOrEmpty("mascot")
    val look = petLookOf(pet)
    val prog = petProgress(workspace)
    val stage = petStage(prog.level)
    var previewMood by remember { mutableStateOf(PetMood.HAPPY) }
    var name by remember(pet.string("name", "Mochi")) { mutableStateOf(pet.string("name", "Mochi")) }
    NeoCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetCharacter(look, previewMood, prog.level, diameter = 190.dp) }
        Spacer(Modifier.height(10.dp))
        Text("${PetStageNames[stage]} · Level ${prog.level}", fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        val next = nextStageLevel(prog.level)
        Text(
            if (next != null) "Grows into ${PetStageNames[stage + 1]} at level $next. Keep logging, budgeting and saving." else "Fully grown. Nice work.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f), modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        StudioLabel("Preview mood")
        ChoiceRow(listOf("Happy", "Thriving", "Stressed", "Ramen", "Celebrate"), previewMood.ordinal, { previewMood = PetMood.values()[it] })
    }
    NeoCard {
        PopField(name, { name = it; if (it.isNotBlank()) onChange { addProperty("name", it.take(20)) } }, Modifier.fillMaxWidth(), label = { Text("Pet name") }, singleLine = true)
        StudioLabel("Character")
        ChoiceRow(listOf("Girl", "Boy"), if (look.girl) 0 else 1, { i -> onChange { addProperty("gender", if (i == 0) "girl" else "boy") } })
        StudioLabel("Skin tone")
        Swatches(PetOptions.skins, look.skin) { i -> onChange { addProperty("skin", i) } }
        StudioLabel("Hair style")
        ChoiceRow(PetOptions.hairStyles, look.hairStyle, { i -> onChange { addProperty("hairStyle", i) } })
        StudioLabel("Hair colour")
        Swatches(PetOptions.hairColors, look.hairColor) { i -> onChange { addProperty("hairColor", i) } }
        StudioLabel("Eyes")
        ChoiceRow(PetOptions.eyeStyles, look.eyes, { i -> onChange { addProperty("eyes", i) } })
        StudioLabel("Cheeks")
        ChoiceRow(listOf("Blush on", "Blush off"), if (look.blush) 0 else 1, { i -> onChange { addProperty("blush", i == 0) } })
    }
    NeoCard {
        StudioLabel("Outfit style")
        ChoiceRow(PetOptions.outfitStyles, look.outfitStyle, { i -> onChange { addProperty("outfitStyle", i) } })
        StudioLabel("Outfit colour")
        Swatches(PetOptions.outfitColors, look.outfitColor) { i -> onChange { addProperty("outfitColor", i) } }
        StudioLabel("Accessory")
        ChoiceRow(PetOptions.accessories.map { it.first }, look.accessory, { i -> onChange { addProperty("accessory", i) } }, locked = { i ->
            val need = PetOptions.accessories[i].second
            if (prog.level < need) "Lv $need" else null
        })
        StudioLabel("Background")
        Swatches(PetOptions.backgrounds, look.background) { i -> onChange { addProperty("background", i) } }
        Spacer(Modifier.height(4.dp))
        Text("Some accessories unlock as your pet levels up. Changes appear on Home straight away.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
    }
}
