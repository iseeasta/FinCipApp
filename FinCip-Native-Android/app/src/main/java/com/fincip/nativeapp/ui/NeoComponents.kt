package com.fincip.nativeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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

private val Ink = Color(0xFF171717)

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(shape).background(Ink))
        Surface(
            modifier = Modifier.fillMaxWidth().border(BorderStroke(2.dp, Ink), shape),
            shape = shape,
            color = fill,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(Modifier.padding(15.dp), content = content)
        }
    }
}

@Composable
fun NeoButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.border(2.dp, Ink, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp)
    ) { Text(text, fontWeight = FontWeight.ExtraBold) }
}

@Composable
fun NeoOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Ink)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun NeoTitle(eyebrow: String, title: String, description: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
    }
}

@Composable
fun MetricTile(label: String, value: String, note: String = "", fill: Color = MaterialTheme.colorScheme.surfaceVariant, modifier: Modifier = Modifier) {
    NeoCard(modifier, fill) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(7.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        if (note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
        }
    }
}

@Composable
fun Field(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, hint: String = "", singleLine: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            placeholder = if (hint.isNotBlank()) ({ Text(hint) }) else null,
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
fun Selector(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), border = BorderStroke(2.dp, Ink)) {
                Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("▾", fontWeight = FontWeight.Black)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Composable
fun ProgressBar(value: Float, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(modifier.fillMaxWidth().height(12.dp).clip(shape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)).border(1.dp, Ink, shape)) {
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(12.dp).clip(shape).background(color))
    }
}

@Composable
fun SmallTag(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer, border = BorderStroke(1.dp, Ink)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
