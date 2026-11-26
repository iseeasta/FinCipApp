package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.MascotOptions

/** Greeting on the dashboard. Only shown once the user has saved a mascot; before that it invites them to make one. */
@Composable
fun MascotGreetingCard(name: String, line: String, celebrate: Boolean, onOpen: () -> Unit) {
    val look = MascotRuntime.look
    val hour = java.time.LocalTime.now().hour
    val hi = when { hour < 5 -> "Late night"; hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val who = name.substringBefore(' ').ifBlank { "there" }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFFEDE7FF), Color(0xFFD3C6FF)))).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (look != null) MascotView(look, 84.dp, animated = true, expression = if (celebrate) "joy" else null)
        else MascotView(com.fincip.nativeapp.data.MascotLook.defaultFor(0), 84.dp, animated = false)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (look != null) "$hi, $who" else "Meet your FinCip companion", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2A1A33))
            Text(if (look != null) line else "Pick a character and style it. Tap to start.", fontSize = 12.sp, color = Color(0xFF4B3A66), lineHeight = 16.sp)
        }
    }
}

/** Empty-state card that shows the mascot when one is saved, otherwise the plain text card. */
@Composable
fun MascotEmptyCard(title: String, body: String) {
    val look = MascotRuntime.look
    NeoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (look != null) MascotView(look, 72.dp, animated = false, expression = "calm")
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black)
                Text(body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
