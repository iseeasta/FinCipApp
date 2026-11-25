package com.fincip.nativeapp.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.NavItemDef
import com.fincip.nativeapp.data.NavItems
import com.fincip.nativeapp.data.NavPrefs

private fun navIcon(id: String): ImageVector = when (id) {
    "overview" -> Icons.Default.Home
    "transactions" -> Icons.Default.Wallet
    "budgets" -> Icons.Default.PieChart
    "portfolio" -> Icons.Default.BarChart
    "settings" -> Icons.Default.Settings
    else -> Icons.Default.MoreHoriz
}

private val allItems = NavItems.primary + NavItemDef(NavItems.MORE, "More")

/** One destination. Used by every layout, so highlight, labels and icon size behave the same everywhere. */
@Composable
private fun NavEntry(
    def: NavItemDef, active: Boolean, prefs: NavPrefs, sideBySide: Boolean, showIcon: Boolean, showLabel: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (active) scheme.primary else scheme.onSurface.copy(alpha = .62f)
    val shape = RoundedCornerShape(prefs.corner.coerceAtMost(26).dp)
    val fill = when (prefs.activeStyle) {
        "pill" -> if (active) scheme.primaryContainer else Color.Transparent
        "glow" -> if (active) scheme.primary.copy(alpha = .16f) else Color.Transparent
        else -> Color.Transparent
    }
    val rim = if (prefs.activeStyle == "glow" && active) Modifier.border(BorderStroke(1.dp, scheme.primary.copy(alpha = .5f)), shape) else Modifier
    Box(modifier.clip(shape).then(rim).background(fill).clickable(role = Role.Tab, onClickLabel = def.label, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        val iconV = @Composable { Icon(navIcon(def.id), contentDescription = def.label, tint = if (active && prefs.activeStyle == "pill") scheme.onPrimaryContainer else tint, modifier = Modifier.size(prefs.iconSize.dp)) }
        val labelV = @Composable { Text(def.label, fontSize = if (sideBySide) 14.sp else 11.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Medium, color = if (active && prefs.activeStyle == "pill") scheme.onPrimaryContainer else tint, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (sideBySide) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (showIcon) iconV(); if (showLabel) labelV() }
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) { if (showIcon) iconV(); if (showLabel) labelV() }
        if (prefs.activeStyle == "underline" && active) Box(Modifier.align(Alignment.BottomCenter).width(prefs.iconSize.dp).height(3.dp).clip(RoundedCornerShape(50)).background(scheme.primary))
    }
}

private fun showIcon(p: NavPrefs) = p.labels != "text"
private fun showLabel(p: NavPrefs) = p.labels != "icons"

@Composable
private fun Shell(shape: Shape, color: Color, modifier: Modifier = Modifier, border: Boolean = false, elevation: Dp = 8.dp, content: @Composable () -> Unit) {
    Surface(modifier, shape = shape, color = color, shadowElevation = if (color.alpha < .7f) 0.dp else elevation, border = if (border) BorderStroke(1.dp, Color.White.copy(alpha = .35f)) else null, content = content)
}

/**
 * The one navigation view. Position and style come from [prefs]; selection and taps go through the same callbacks
 * in every layout. [onSelect] receives a page id, or [NavItems.MORE] to open the full menu.
 */
@Composable
fun NavBarView(
    prefs: NavPrefs, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier,
    railExpanded: Boolean = false, onToggleRail: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val glass = prefs.style == "glass"
    val color = scheme.surface.copy(alpha = if (glass) minOf(prefs.bgAlpha, .6f) else prefs.bgAlpha)
    val gap = prefs.spacing.dp
    val h = prefs.height.dp

    @Composable fun RowScope.Items(side: Boolean, icon: Boolean, label: Boolean, weighted: Boolean, minW: Dp = 0.dp) {
        allItems.forEach { d ->
            NavEntry(d, d.id == selected, prefs, side, icon, label, { onSelect(d.id) }, (if (weighted) Modifier.weight(1f) else Modifier.widthIn(min = minW)).heightIn(min = 40.dp))
        }
    }

    when (prefs.position) {
        "bottom" -> {
            val compact = prefs.style == "compact"
            val bar: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth().height(if (compact) minOf(h, 52.dp) else h).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                    Items(false, showIcon(prefs), if (compact) prefs.labels == "text" else showLabel(prefs), true)
                }
            }
            when (prefs.style) {
                "floating" -> Box(modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { Shell(RoundedCornerShape(prefs.corner.dp), color, Modifier.fillMaxWidth(), elevation = 12.dp, content = bar) }
                "pill" -> Box(modifier.padding(horizontal = 18.dp, vertical = 8.dp)) { Shell(RoundedCornerShape(50), color, Modifier.fillMaxWidth(), elevation = 12.dp, content = bar) }
                "curved" -> Shell(RoundedCornerShape(topStart = maxOf(prefs.corner, 28).dp, topEnd = maxOf(prefs.corner, 28).dp), color, modifier.fillMaxWidth(), elevation = 14.dp, content = bar)
                else -> Shell(RoundedCornerShape(0.dp), color, modifier.fillMaxWidth(), elevation = 8.dp, content = bar)
            }
        }
        "top" -> {
            val compact = prefs.style == "compact"
            val centered = prefs.style == "centered" || prefs.style == "pill"
            val row: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 6.dp), horizontalArrangement = if (centered) Arrangement.spacedBy(gap, Alignment.CenterHorizontally) else Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                    allItems.forEach { d -> NavEntry(d, d.id == selected, prefs, true, showIcon(prefs), showLabel(prefs) && !(compact && prefs.labels == "both"), { onSelect(d.id) }, Modifier.heightIn(min = if (compact) 36.dp else 44.dp)) }
                }
            }
            when (prefs.style) {
                "floating" -> Box(modifier.padding(horizontal = 14.dp, vertical = 6.dp)) { Shell(RoundedCornerShape(prefs.corner.dp), color, Modifier.fillMaxWidth(), elevation = 10.dp, content = row) }
                "pill" -> Box(modifier.padding(horizontal = 18.dp, vertical = 6.dp)) { Shell(RoundedCornerShape(50), color, Modifier.fillMaxWidth(), elevation = 10.dp, content = row) }
                else -> Shell(RoundedCornerShape(0.dp), color, modifier.fillMaxWidth(), elevation = 2.dp, content = row)
            }
        }
        "dock" -> {
            val minimal = prefs.style == "minimal"
            Box(modifier.fillMaxWidth().padding(bottom = 10.dp, top = 6.dp), contentAlignment = Alignment.Center) {
                Shell(RoundedCornerShape(prefs.corner.dp), if (prefs.style == "glass") color else if (minimal) color.copy(alpha = minOf(prefs.bgAlpha, .85f)) else color, border = prefs.style == "glass", elevation = 14.dp) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        allItems.forEach { d -> NavEntry(d, d.id == selected, prefs, false, showIcon(prefs) || minimal, (if (prefs.style == "labels") showLabel(prefs) else prefs.labels == "text") && !minimal, { onSelect(d.id) }, Modifier.widthIn(min = if (minimal) 44.dp else 54.dp)) }
                    }
                }
            }
        }
        else -> {
            // left rail styles (the "expanded" style is the classic drawer and is not drawn here)
            val collapsible = prefs.style == "collapsible"
            val wide = collapsible && railExpanded
            val iconOnly = prefs.style == "compact" || (collapsible && !railExpanded)
            val railW = if (wide) 164.dp else maxOf(60.dp, minOf(prefs.height.dp + 4.dp, 92.dp))
            val body: @Composable () -> Unit = {
                Column(Modifier.width(railW).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 6.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (collapsible) Icon(if (wide) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, contentDescription = if (wide) "Collapse menu" else "Expand menu", modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).clickable(onClick = onToggleRail).padding(4.dp))
                    allItems.forEach { d ->
                        NavEntry(d, d.id == selected, prefs, wide, showIcon(prefs) || iconOnly, if (iconOnly) false else showLabel(prefs), { onSelect(d.id) }, Modifier.fillMaxWidth().heightIn(min = 44.dp))
                    }
                }
            }
            when (prefs.style) {
                "floating", "glass" -> Box(modifier.padding(8.dp).fillMaxHeight()) { Shell(RoundedCornerShape(prefs.corner.dp), color, Modifier.fillMaxHeight(), border = glass, elevation = 12.dp, content = body) }
                else -> Shell(RoundedCornerShape(0.dp), color, modifier.fillMaxHeight(), elevation = 4.dp, content = body)
            }
        }
    }
}

// ============================================================ Studio

@Composable
private fun StudioLabel(text: String) = Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Row { Text(label, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(display, fontWeight = FontWeight.Black, fontSize = 13.sp) }
        Slider(value, onChange, valueRange = range)
    }
}

/** Mini phone showing the real navigation composable with the draft settings. */
@Composable
private fun NavPreview(prefs: NavPrefs) {
    val scheme = MaterialTheme.colorScheme
    var sel by remember(prefs.position) { mutableStateOf("overview") }
    var railOpen by remember { mutableStateOf(false) }
    val content: @Composable (Modifier) -> Unit = { m ->
        Column(m.background(scheme.background).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(NavItems.primary.firstOrNull { it.id == sel }?.label ?: "More", fontWeight = FontWeight.Black, fontSize = 18.sp)
            repeat(3) { Box(Modifier.fillMaxWidth().height(if (it == 0) 54.dp else 34.dp).clip(RoundedCornerShape(14.dp)).background(scheme.surfaceVariant.copy(alpha = .8f))) }
        }
    }
    Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(26.dp)).border(2.dp, scheme.outline.copy(alpha = .4f), RoundedCornerShape(26.dp)).background(scheme.background)) {
        when (prefs.position) {
            "top" -> Column(Modifier.fillMaxSize()) { NavBarView(prefs, sel, { sel = it }); content(Modifier.weight(1f).fillMaxWidth()) }
            "bottom" -> Column(Modifier.fillMaxSize()) { content(Modifier.weight(1f).fillMaxWidth()); NavBarView(prefs, sel, { sel = it }) }
            "dock" -> Box(Modifier.fillMaxSize()) { content(Modifier.fillMaxSize()); NavBarView(prefs, sel, { sel = it }, Modifier.align(Alignment.BottomCenter)) }
            else -> Row(Modifier.fillMaxSize()) {
                if (prefs.isDefaultDrawer) {
                    Column(Modifier.width(128.dp).fillMaxHeight().background(scheme.surface).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        NavItems.primary.forEach { d -> NavEntry(d, d.id == sel, prefs, true, showIcon(prefs), showLabel(prefs), { sel = d.id }, Modifier.fillMaxWidth()) }
                    }
                } else NavBarView(prefs, sel, { sel = it }, railExpanded = railOpen, onToggleRail = { railOpen = !railOpen })
                content(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
fun NavStudioScreen(current: NavPrefs, onApply: (NavPrefs) -> Unit, onReset: () -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    val width = LocalConfiguration.current.screenWidthDp
    val note = draft.fitted(width, NavPrefs.DEFAULT_ITEMS).second
    fun set(p: NavPrefs) { draft = p.sanitized() }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NeoTitle("MAKE IT YOURS", "Navigation Studio", "Pick where the menu lives and how it looks. The preview below is the real navigation, so what you see is what you get.")
        NavPreview(draft.fitted(width, NavPrefs.DEFAULT_ITEMS).first)
        if (note != null) NeoCard(fill = Pop.Sun.copy(alpha = .35f)) { Text(note, fontSize = 13.sp, color = Pop.Ink) }
        NeoCard {
            StudioLabel("POSITION")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavPrefs.POSITIONS.forEach { pos -> PopChip(NavPrefs.positionLabel(pos), draft.position == pos, { set(draft.copy(position = pos, style = NavPrefs.stylesFor(pos).first().first)) }) }
            }
            Spacer(Modifier.height(12.dp))
            StudioLabel("STYLE")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavPrefs.stylesFor(draft.position).forEach { (id, label) -> PopChip(label, draft.style == id, { set(draft.copy(style = id)) }) }
            }
            if (draft.isDefaultDrawer) Text("The classic slide-in menu with every page, grouped in sections.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 8.dp))
            else Text("Shows ${NavItems.primary.joinToString(", ") { it.label }} and More. More opens the full menu, so no page is ever out of reach.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 8.dp))
        }
        NeoCard {
            StudioLabel("CONTENT")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("both" to "Icons + labels", "icons" to "Icons only", "text" to "Text only").forEach { (id, l) -> PopChip(l, draft.labels == id, { set(draft.copy(labels = id)) }) }
            }
            Spacer(Modifier.height(12.dp))
            StudioLabel("ACTIVE ITEM")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("pill" to "Pill", "underline" to "Underline", "glow" to "Glow", "none" to "Colour only").forEach { (id, l) -> PopChip(l, draft.activeStyle == id, { set(draft.copy(activeStyle = id)) }) }
            }
        }
        NeoCard {
            StudioLabel("FINE TUNING")
            SliderRow("Icon size", draft.iconSize.toFloat(), 18f..34f, "${draft.iconSize} dp") { set(draft.copy(iconSize = it.toInt())) }
            SliderRow("Spacing", draft.spacing.toFloat(), 0f..16f, "${draft.spacing} dp") { set(draft.copy(spacing = it.toInt())) }
            SliderRow("Background opacity", draft.bgAlpha, 0.3f..1f, "${(draft.bgAlpha * 100).toInt()}%") { set(draft.copy(bgAlpha = it)) }
            SliderRow("Corner radius", draft.corner.toFloat(), 0f..36f, "${draft.corner} dp") { set(draft.copy(corner = it.toInt())) }
            if (!draft.isDefaultDrawer) SliderRow(if (draft.position == "left") "Width" else "Height", draft.height.toFloat(), 48f..96f, "${draft.height} dp") { set(draft.copy(height = it.toInt())) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoOutlineButton("Reset to default", { draft = NavPrefs(); onReset() }, Modifier.weight(1f))
            NeoButton("Apply", Modifier.weight(1f), onClick = { onApply(draft) })
        }
        Text("Saved on this device and included in your FinCip backup.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
    }
}
