package com.fincip.nativeapp.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Hard offset shadow colour: ink on light themes, lime on dark ones. */
@Composable
fun hardShadowColor(): Color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Pop.Ink else Pop.Lime

/** Draws a solid, un-blurred offset shadow behind a rounded shape. */
fun Modifier.hardShadow(color: Color, radius: Dp, offset: Dp = 4.dp): Modifier = drawBehind {
    val o = offset.toPx()
    drawRoundRect(color, topLeft = Offset(o, o), size = size, cornerRadius = CornerRadius(radius.toPx()))
}

/**
 * Soft surface: rounded corners, hairline border and a gentle drop shadow. Text inside automatically picks a
 * readable colour for [fill]. Pass a [Pop] colour for a coloured block; default is the plain themed surface.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Pop.Radius)
    CompositionLocalProvider(LocalContentColor provides contentOn(fill)) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = fill,
            tonalElevation = 0.dp,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp), content = content)
        }
    }
}

/** Primary action: deep-green pill that presses in slightly. */
@Composable
fun NeoButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(90), label = "press")
    val tap = rememberTap()
    Button(
        onClick = { tap(); onClick() },
        enabled = enabled,
        interactionSource = source,
        modifier = modifier.scale(scale).height(52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) { Text(text, fontWeight = FontWeight.ExtraBold) }
}

@Composable
fun NeoOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(90), label = "press")
    val tap = rememberTap()
    OutlinedButton(
        onClick = { tap(); onClick() },
        interactionSource = source,
        modifier = modifier.scale(scale).height(48.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.2.dp, LocalContentColor.current.copy(alpha = .5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

/** Selectable pill used for category/filter rows. Selected = dark ink fill. */
@Composable
fun PopChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, leading: ImageVector? = null) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val tap = rememberTap()
    Surface(
        onClick = { tap(); onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (leading != null) Icon(leading, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, maxLines = 1, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun NeoTitle(eyebrow: String, title: String, description: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.6.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f), fontWeight = FontWeight.Black)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f))
    }
}

/** Compact stat block. Defaults to a soft tinted surface; pass a [Pop] colour for colour-blocking. */
@Composable
fun MetricTile(label: String, value: String, note: String = "", fill: Color = MaterialTheme.colorScheme.surfaceVariant, modifier: Modifier = Modifier) {
    val fg = contentOn(fill)
    NeoCard(modifier, fill) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = fg.copy(alpha = .7f), fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = fg, maxLines = 1)
        if (note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = .72f))
        }
    }
}

@Composable
fun Field(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, hint: String = "", singleLine: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            placeholder = if (hint.isNotBlank()) ({ Text(hint) }) else null,
            shape = RoundedCornerShape(Pop.RadiusSmall),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun Selector(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(Pop.RadiusSmall),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
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
    val animated by animateFloatAsState(value.coerceIn(0f, 1f), tween(450), label = "progress")
    Box(modifier.fillMaxWidth().height(14.dp).clip(shape).background(LocalContentColor.current.copy(alpha = .14f))) {
        Box(Modifier.fillMaxWidth(animated).height(14.dp).clip(shape).background(color))
    }
}

@Composable
fun SmallTag(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * Minimal bar chart in the style of the reference "Progress" card.
 * [values] are raw amounts; the tallest bar fills the chart. [highlight] gets [accent], the rest are muted.
 */
@Composable
fun MiniBars(
    values: List<Double>,
    labels: List<String>,
    highlight: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 96.dp
) {
    val peak = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val fg = LocalContentColor.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val target = if (v <= 0.0) 6.dp else (chartHeight * (v / peak).toFloat()).coerceAtLeast(10.dp)
            val h by animateDpAsState(target, tween(450), label = "bar")
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(chartHeight), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth().height(h)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(if (i == highlight) accent else fg.copy(alpha = .16f))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = if (i == highlight) 1f else .6f), fontWeight = if (i == highlight) FontWeight.Black else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}


/** Drop-in themed replacement for OutlinedTextField (rounded, filled, ink focus border). */
@Composable
fun PopField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        label = label, placeholder = placeholder, singleLine = singleLine,
        shape = RoundedCornerShape(Pop.RadiusSmall),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
