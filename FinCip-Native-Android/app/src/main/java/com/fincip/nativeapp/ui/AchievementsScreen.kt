package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fincip.nativeapp.data.AchState
import com.fincip.nativeapp.data.Achievements
import com.google.gson.JsonObject

@Composable
fun AchievementsScreen(workspace: JsonObject, level: Int, xp: Int) {
    val s = MaterialTheme.colorScheme
    val tap = rememberTap()
    val states = remember(workspace, level, xp) { Achievements.evaluate(Achievements.stats(workspace, level, xp)) }
    val stages = states.sumOf { it.tier }
    val total = states.size * 3
    var filter by remember { mutableStateOf(0) }
    var open by remember { mutableStateOf<AchState?>(null) }
    val shown = when (filter) { 1 -> states.filter { it.tier > 0 }; 2 -> states.filter { it.tier == 0 || it.tier < 3 }.sortedByDescending { it.progress }; 3 -> states.filter { it.done }; else -> states }
    val best = states.filter { it.tier > 0 }.maxWithOrNull(compareBy<AchState> { it.tier }.thenBy { it.def.id })
    val next = states.filter { it.tier < 3 }.maxByOrNull { it.progress }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp + LocalNavTop.current, bottom = 28.dp + LocalNavInset.current), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ---- hero: progress arc, centre emblem
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(s.surface).padding(vertical = 18.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(230.dp, 190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(230.dp, 190.dp)) {
                    val w = 16.dp.toPx(); val d = size.width - w
                    val tl = Offset(w / 2, w / 2)
                    drawArc(s.onSurface.copy(alpha = .08f), 150f, 240f, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
                    drawArc(s.primary, 150f, 240f * stages / total.coerceAtLeast(1), false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
                }
                if (best != null) AchBadge(best.def.glyph, best.def.shape, best.def.hue, best.tier, 110.dp)
                else Text("Lv $level", fontWeight = FontWeight.Black, fontSize = 34.sp)
            }
            Text("Level $level", fontWeight = FontWeight.Black, fontSize = 26.sp)
            Text("$stages of $total stages unlocked", color = s.onSurface.copy(alpha = .6f), fontSize = 14.sp)
            if (next != null) Text("Closest: ${next.def.title}, ${next.def.text(next.tier)} (${next.value.toLong()} so far)", Modifier.padding(top = 8.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = s.onSurface.copy(alpha = .55f))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All ${states.size}", "Unlocked", "In progress", "Mastered").forEachIndexed { i, t ->
                val on = filter == i
                Text(t, Modifier.clip(RoundedCornerShape(50)).background(if (on) s.primary else s.surfaceVariant).clickable { tap(); filter = i }.padding(horizontal = 16.dp, vertical = 9.dp), color = if (on) s.onPrimary else s.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (shown.isEmpty()) Text("Nothing here yet. Log a few entries and come back.", color = s.onSurface.copy(alpha = .6f))
        shown.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { st ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable { tap(); open = st }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AchBadge(st.def.glyph, st.def.shape, st.def.hue, st.tier.coerceAtLeast(1), 84.dp, locked = st.tier == 0)
                        Text(st.def.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
                        Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(3) { i -> Box(Modifier.size(width = 14.dp, height = 4.dp).clip(CircleShape).background(if (i < st.tier) s.primary else s.onSurface.copy(alpha = .12f))) }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    open?.let { st ->
        Dialog(onDismissRequest = { open = null }) {
            Column(Modifier.clip(RoundedCornerShape(28.dp)).background(s.surface).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(st.def.title, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0 until 3).forEach { i ->
                        val got = st.tier > i
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AchBadge(st.def.glyph, st.def.shape, st.def.hue, i + 1, 86.dp, locked = !got)
                            Text("Stage ${i + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(st.def.text(i), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp, color = s.onSurface.copy(alpha = .65f))
                            Text(if (got) "Done" else "Locked", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (got) s.primary else s.onSurface.copy(alpha = .4f))
                        }
                    }
                }
                if (!st.done) {
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(s.onSurface.copy(alpha = .1f))) { Box(Modifier.fillMaxWidth(st.progress.coerceAtLeast(.02f)).height(8.dp).clip(CircleShape).background(s.primary)) }
                    Text("${st.value.toLong()} of ${st.nextTarget.toLong()} for the next stage", fontSize = 12.sp, color = s.onSurface.copy(alpha = .6f))
                } else Text("All three stages complete.", fontWeight = FontWeight.Bold, color = s.primary)
            }
        }
    }
}
