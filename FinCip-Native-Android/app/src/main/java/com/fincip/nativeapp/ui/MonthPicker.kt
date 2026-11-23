package com.fincip.nativeapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Month and year picker used by the top bar and the calendar. Months that contain entries get a small dot,
 * so you can see where your data is before jumping there.
 */
@Composable
fun MonthPickerDialog(current: YearMonth, activeMonths: Set<String>, onPick: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    var year by remember { mutableStateOf(current.year) }
    var direction by remember { mutableStateOf(1) }
    var yearMode by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val tap = rememberTap()
    val now = YearMonth.now()
    val decade = year - Math.floorMod(year, 12)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(horizontal = 22.dp).clip(RoundedCornerShape(32.dp))
                .background(scheme.surface)
                .background(Brush.verticalGradient(listOf(scheme.primary.copy(alpha = .16f), Color.Transparent)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("JUMP TO", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = scheme.onSurface.copy(alpha = .5f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(scheme.surfaceVariant).clickable { tap(); direction = -1; year -= if (yearMode) 12 else 1 }, contentAlignment = Alignment.Center) { Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { tap(); yearMode = !yearMode }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = if (yearMode) -decade else year,
                        transitionSpec = { (fadeIn(tween(200)) + slideInVertically(tween(240)) { direction * it / 3 }) togetherWith (fadeOut(tween(120)) + slideOutVertically(tween(200)) { -direction * it / 3 }) },
                        label = "year"
                    ) { y ->
                        Text(if (yearMode) "${-y} – ${-y + 11}" else y.toString(), fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, letterSpacing = (-1).sp)
                    }
                }
                Box(Modifier.size(42.dp).clip(CircleShape).background(scheme.surfaceVariant).clickable { tap(); direction = 1; year += if (yearMode) 12 else 1 }, contentAlignment = Alignment.Center) { Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
            Text(if (yearMode) "Tap a year" else "Tap the year to jump faster", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .45f), modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (yearMode) {
                    (decade until decade + 12).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { y ->
                                val on = y == current.year
                                Tile(y.toString(), on, y == now.year, false, Modifier.weight(1f)) { tap(); year = y; yearMode = false }
                            }
                        }
                    }
                } else {
                    (1..12).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { m ->
                                val ym = YearMonth.of(year, m)
                                Tile(
                                    Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), ym == current, ym == now, ym.toString() in activeMonths, Modifier.weight(1f)
                                ) { tap(); onPick(ym) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This month", Modifier.weight(1f).clip(RoundedCornerShape(50)).background(scheme.surfaceVariant).clickable { tap(); onPick(now) }.padding(vertical = 13.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text("Close", Modifier.weight(1f).clip(RoundedCornerShape(50)).background(scheme.primary).clickable { tap(); onDismiss() }.padding(vertical = 13.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
            }
        }
    }
}

@Composable
private fun Tile(label: String, selected: Boolean, isNow: Boolean, hasData: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.height(58.dp).clip(shape)
            .background(if (selected) scheme.primary else scheme.surfaceVariant.copy(alpha = .65f))
            .then(if (isNow && !selected) Modifier.border(1.5.dp, scheme.primary, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold, fontSize = 16.sp, color = if (selected) scheme.onPrimary else scheme.onSurface)
        if (hasData) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp).size(5.dp).clip(CircleShape).background(if (selected) scheme.onPrimary else scheme.primary))
    }
}
