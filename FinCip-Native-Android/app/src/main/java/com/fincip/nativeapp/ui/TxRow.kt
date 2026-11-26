package com.fincip.nativeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.TxText

/**
 * One transaction, drawn the same everywhere. A tinted round badge (category icon, or a direction arrow when the
 * category is just "Other"), a clean name, a quiet "UPI · 24 Nov" line and a bold coloured amount.
 */
@Composable
fun TxRow(
    rawTitle: String, category: String, type: String, date: String, amount: String, inflow: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier, note: String = "", trailing: (@Composable () -> Unit)? = null, footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val clean = TxText.clean(rawTitle)
    val tone = if (inflow) Pop.Teal else Pop.Coral
    val generic = category.isBlank() || category.equals("other", true) || category.equals("uncategorized", true)
    Surface(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = .6f))) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (generic || type == "transfer") {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(tone.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                        Icon(if (type == "transfer") Icons.Default.SwapHoriz else if (inflow) Icons.Default.SouthWest else Icons.Default.NorthEast, null, tint = tone, modifier = Modifier.size(22.dp))
                    }
                } else CategoryBadge(category, 46.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(clean.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOf(clean.method, if (generic) "" else category, TxText.dayLabel(date)).filter { it.isNotBlank() }.joinToString(" · ")
                    Text(sub, fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .58f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (note.isNotBlank()) Text(note, fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(amount, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = tone, maxLines = 1)
                trailing?.invoke()
            }
            footer?.invoke(this)
        }
    }
}
