package com.fincip.nativeapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.NavDests
import com.fincip.nativeapp.data.NavLayouts
import com.fincip.nativeapp.data.NavPrefs
import com.fincip.nativeapp.data.NavPage
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** Everything that can be open at once lives here, so opening one menu always closes the others. */
class NavState {
    var sheetOpen by mutableStateOf(false)
    var menuOpen by mutableStateOf(false)
    var fanOpen by mutableStateOf(false)
    var searchOpen by mutableStateOf(false)
    var popupGroup by mutableStateOf<String?>(null)
    var flyoutGroup by mutableStateOf<String?>(null)
    var sideExpanded by mutableStateOf(false)
    val openGroups = mutableStateListOf<String>()
    val lastInGroup = mutableStateMapOf<String, String>()
    val anyOpen: Boolean get() = sheetOpen || menuOpen || fanOpen || searchOpen || popupGroup != null || flyoutGroup != null
    fun closeAll() { sheetOpen = false; menuOpen = false; fanOpen = false; searchOpen = false; popupGroup = null; flyoutGroup = null }
}

/** Extra room pages leave at the bottom when a floating bar sits over them. */
val LocalNavInset = androidx.compose.runtime.compositionLocalOf { 0.dp }

@Composable fun rememberNavState() = remember { NavState() }

// ------------------------------------------------------------------ building blocks

@Composable
private fun barColor(p: NavPrefs, glass: Boolean) = MaterialTheme.colorScheme.surface.copy(alpha = if (glass) minOf(p.bgAlpha, .6f) else p.bgAlpha)

private fun showIcon(p: NavPrefs) = p.labels != "text"
private fun showLabel(p: NavPrefs) = p.labels != "icons"

@Composable
private fun Shell(shape: Shape, color: Color, modifier: Modifier = Modifier, border: Boolean = false, elevation: Dp = 8.dp, content: @Composable () -> Unit) {
    Surface(modifier, shape = shape, color = color, shadowElevation = if (color.alpha < .7f) 0.dp else elevation, border = if (border) BorderStroke(1.dp, Color.White.copy(alpha = .35f)) else null, content = content)
}

@Composable
private fun BarShell(prefs: NavPrefs, bottom: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val glass = prefs.look == "glass"
    val color = barColor(prefs, glass)
    val c = prefs.corner.dp
    when (prefs.look) {
        "floating", "glass", "rounded" -> Box(modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { Shell(RoundedCornerShape(c), color, Modifier.fillMaxWidth(), glass, 12.dp, content) }
        "pill" -> Box(modifier.padding(horizontal = 18.dp, vertical = 8.dp)) { Shell(RoundedCornerShape(50), color, Modifier.fillMaxWidth(), false, 12.dp, content) }
        "curved" -> {
            val r = maxOf(prefs.corner, 28).dp
            Shell(if (bottom) RoundedCornerShape(topStart = r, topEnd = r) else RoundedCornerShape(bottomStart = r, bottomEnd = r), color, modifier.fillMaxWidth(), false, 14.dp, content)
        }
        else -> Shell(RoundedCornerShape(0.dp), color, modifier.fillMaxWidth(), false, 6.dp, content)
    }
}

/** One destination. Colour and scale animate between states, so selection never snaps. */
@Composable
private fun NavCell(
    icon: ImageVector, label: String, active: Boolean, prefs: NavPrefs, sideBySide: Boolean, selfBg: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier, forceLabel: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val onPill = active && prefs.activeStyle == "pill"
    val tint by animateColorAsState(if (onPill) scheme.onPrimaryContainer else if (active) scheme.primary else scheme.onSurface.copy(alpha = .62f), tween(240), label = "tint")
    val scale by animateFloatAsState(if (active) 1.12f else 1f, spring(dampingRatio = .55f, stiffness = 380f), label = "scale")
    val shape = RoundedCornerShape(prefs.corner.coerceAtMost(26).dp)
    val target = if (!selfBg) Color.Transparent else when (prefs.activeStyle) { "pill" -> if (active) scheme.primaryContainer else Color.Transparent; "glow" -> if (active) scheme.primary.copy(alpha = .16f) else Color.Transparent; else -> Color.Transparent }
    val bg by animateColorAsState(target, tween(260), label = "bg")
    val doLabel = forceLabel || showLabel(prefs)
    val doIcon = showIcon(prefs) || !doLabel
    Box(modifier.clip(shape).background(bg).clickable(role = Role.Tab, onClickLabel = label, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        val iconV = @Composable { Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(prefs.iconSize.dp).graphicsLayer { scaleX = scale; scaleY = scale }) }
        val labelV = @Composable { Text(label, fontSize = if (sideBySide) 14.sp else 11.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Medium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (sideBySide) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (doIcon) iconV(); if (doLabel) labelV() }
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) { if (doIcon) iconV(); if (doLabel) labelV() }
        if (selfBg && prefs.activeStyle == "underline") AnimatedVisibility(active, Modifier.align(Alignment.BottomCenter), enter = scaleIn(tween(220)) + fadeIn(), exit = scaleOut(tween(150)) + fadeOut()) {
            Box(Modifier.width(prefs.iconSize.dp).height(3.dp).clip(RoundedCornerShape(50)).background(scheme.primary))
        }
    }
}

/** A row of equal cells with one indicator that glides to the selected cell. */
@Composable
private fun SlidingRow(count: Int, selected: Int, cellW: Dp, cellH: Dp, prefs: NavPrefs, modifier: Modifier = Modifier, cell: @Composable (Int) -> Unit) {
    val gap = prefs.spacing.dp
    val scheme = MaterialTheme.colorScheme
    val x by animateDpAsState((cellW + gap) * selected.coerceAtLeast(0), spring(dampingRatio = .72f, stiffness = Spring.StiffnessMedium), label = "slide")
    val shape = RoundedCornerShape(prefs.corner.coerceAtMost(26).dp)
    Box(modifier) {
        if (selected >= 0) Box(Modifier.offset(x = x).size(cellW, cellH)) {
            when (prefs.activeStyle) {
                "pill" -> Box(Modifier.fillMaxSize().clip(shape).background(scheme.primaryContainer))
                "glow" -> Box(Modifier.fillMaxSize().clip(shape).background(scheme.primary.copy(alpha = .16f)).border(1.dp, scheme.primary.copy(alpha = .5f), shape))
                "underline" -> Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).width(prefs.iconSize.dp).height(3.dp).clip(RoundedCornerShape(50)).background(scheme.primary))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            repeat(count) { i -> Box(Modifier.size(cellW, cellH), contentAlignment = Alignment.Center) { cell(i) } }
        }
    }
}

@Composable
private fun PageChips(pages: List<NavPage>, page: String, prefs: NavPrefs, onGo: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(prefs.spacing.dp), verticalAlignment = Alignment.CenterVertically) {
        pages.forEach { p -> NavCell(pageIcon(p.id), p.title, NavDests.baseId(page) == p.id, prefs, true, true, { onGo(p.id) }, forceLabel = true) }
    }
}

@Composable
private fun TrackLast(nav: NavState, page: String) {
    LaunchedEffect(page) { nav.lastInGroup[NavDests.groupOf(page)] = NavDests.baseId(page) }
}

private fun goGroup(g: String, nav: NavState, onGo: (String) -> Unit) { onGo(nav.lastInGroup[g] ?: NavDests.pagesIn(g).first().id) }

private val spec = tween<Float>(260)

// ------------------------------------------------------------------ TOP

@Composable
fun NavTop(prefs: NavPrefs, page: String, onGo: (String) -> Unit, nav: NavState, modifier: Modifier = Modifier) {
    if (prefs.region != "top") return
    TrackLast(nav, page)
    AnimatedContent(prefs.layout + prefs.look, modifier, transitionSpec = { (fadeIn(tween(260)) + slideInVertically(tween(300)) { -it / 3 }) togetherWith fadeOut(tween(140)) }, label = "topLayout") {
        BarShell(prefs, bottom = false) {
            if (prefs.layout == "topTabs") {
                val group = NavDests.groupOf(page)
                val idx = NavDests.groups.indexOf(group)
                Column(Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))) {
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        val n = NavDests.groups.size
                        val w = ((maxWidth - prefs.spacing.dp * (n - 1)) / n).coerceAtLeast(60.dp)
                        SlidingRow(n, idx, w, if (prefs.look == "compact") 44.dp else prefs.height.dp.coerceAtMost(64.dp), prefs, Modifier.horizontalScroll(rememberScrollState())) { i ->
                            val g = NavDests.groups[i]
                            NavCell(groupIcon(g), g, i == idx, prefs, false, false, { goGroup(g, nav, onGo) })
                        }
                    }
                    AnimatedContent(group, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "pages") { g ->
                        val ps = NavDests.pagesIn(g)
                        if (ps.size > 1) PageChips(ps, page, prefs, onGo) else Spacer(Modifier.height(2.dp))
                    }
                }
            } else {
                val scroll = rememberScrollState()
                val density = LocalDensity.current
                val idx = NavDests.all.indexOfFirst { it.id == NavDests.baseId(page) }.coerceAtLeast(0)
                LaunchedEffect(idx) { scroll.animateScrollTo(((idx * with(density) { 112.dp.toPx() }) - 120f).toInt().coerceAtLeast(0)) }
                Row(Modifier.horizontalScroll(scroll).padding(horizontal = 8.dp, vertical = if (prefs.look == "compact") 2.dp else 6.dp), horizontalArrangement = Arrangement.spacedBy(prefs.spacing.dp), verticalAlignment = Alignment.CenterVertically) {
                    var last = ""
                    NavDests.all.forEach { p ->
                        if (last.isNotEmpty() && last != p.group) Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        last = p.group
                        NavCell(pageIcon(p.id), p.title, NavDests.baseId(page) == p.id, prefs, true, true, { onGo(p.id) }, Modifier.heightIn(min = if (prefs.look == "compact") 36.dp else 44.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ BOTTOM

@Composable
fun NavBottom(prefs: NavPrefs, page: String, onGo: (String) -> Unit, nav: NavState, modifier: Modifier = Modifier) {
    if (prefs.region != "bottom") return
    TrackLast(nav, page)
    val compact = prefs.look == "compact"
    val barH = if (compact) minOf(prefs.height.dp, 52.dp) else prefs.height.dp
    AnimatedContent(prefs.layout + prefs.look, modifier, transitionSpec = { (fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 3 }) togetherWith fadeOut(tween(140)) }, label = "bottomLayout") {
        when (prefs.layout) {
            "bottomGroups" -> BarShell(prefs, bottom = true) {
                val group = NavDests.groupOf(page)
                val idx = NavDests.groups.indexOf(group)
                Column(Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))) {
                    AnimatedContent(group, transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 4 }) togetherWith fadeOut(tween(120)) }, label = "pages") { g ->
                        val ps = NavDests.pagesIn(g)
                        if (ps.size > 1) PageChips(ps, page, prefs, onGo) else Spacer(Modifier.height(2.dp))
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                        val n = NavDests.groups.size
                        val w = ((maxWidth - prefs.spacing.dp * (n - 1)) / n).coerceAtLeast(56.dp)
                        SlidingRow(n, idx, w, barH, prefs) { i -> val g = NavDests.groups[i]; NavCell(groupIcon(g), g, i == idx, prefs, false, false, { goGroup(g, nav, onGo) }, forceLabel = compact && prefs.labels == "text") }
                    }
                }
            }
            "bottomMore" -> BarShell(prefs, bottom = true) {
                val base = NavDests.baseId(page)
                val items = NavDests.quick
                val idx = items.indexOf(base).let { if (it >= 0) it else items.size }
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                    val n = items.size + 1
                    val w = ((maxWidth - prefs.spacing.dp * (n - 1)) / n).coerceAtLeast(52.dp)
                    SlidingRow(n, if (nav.sheetOpen) items.size else idx, w, barH, prefs) { i ->
                        if (i < items.size) { val d = NavDests.all.first { it.id == items[i] }; NavCell(pageIcon(d.id), shortTitle(d), i == idx, prefs, false, false, { nav.closeAll(); onGo(d.id) }) }
                        else NavCell(Icons.Default.MoreHoriz, "More", idx == items.size || nav.sheetOpen, prefs, false, false, { val o = !nav.sheetOpen; nav.closeAll(); nav.sheetOpen = o })
                    }
                }
            }
            else -> Unit
        }
    }
}

private fun shortTitle(d: NavPage) = when (d.id) { "overview" -> "Home"; "transactions" -> "Money"; else -> d.title }

@Composable
private fun DockBar(prefs: NavPrefs, page: String, onGo: (String) -> Unit, nav: NavState) {
    val glass = prefs.look == "glass"; val minimal = prefs.look == "minimal"
    val color = barColor(prefs, glass).let { if (minimal) it.copy(alpha = minOf(prefs.bgAlpha, .88f)) else it }
    val tabs = NavDests.dockTabs
    val idx = tabs.indexOf(NavDests.dockTabOf(page))
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Shell(RoundedCornerShape(prefs.corner.coerceAtLeast(20).dp), color, Modifier.fillMaxWidth(), glass, 14.dp) {
            BoxWithConstraints(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                val n = tabs.size
                val w = ((maxWidth - prefs.spacing.dp * (n - 1)) / n).coerceAtLeast(52.dp)
                SlidingRow(n, idx, w, if (minimal) 52.dp else prefs.height.dp, prefs) { i ->
                    val t = tabs[i]
                    NavCell(if (t == "More") Icons.Default.MoreHoriz else groupIcon(t), t, i == idx || nav.popupGroup == t, prefs, false, false, { val open = nav.popupGroup == t; nav.closeAll(); if (!open) nav.popupGroup = t })
                }
            }
        }
    }
}

/** Pages of a section as round buttons that pop in one after another, with room to breathe. */
@Composable
private fun CirclePopup(pages: List<NavPage>, title: String, page: String, onGo: (String) -> Unit, modifier: Modifier = Modifier, end: Boolean = false) {
    val cols = when { pages.size <= 4 -> pages.size.coerceAtLeast(1); pages.size <= 6 -> 3; else -> 4 }
    Column(modifier, horizontalAlignment = if (end) Alignment.End else Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp, color = Color.White.copy(alpha = .9f))
        pages.chunked(cols).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEachIndexed { c, d -> CircleItem(d, NavDests.baseId(page) == d.id, r * cols + c) { onGo(d.id) } }
            }
        }
    }
}

@Composable
private fun CircleItem(d: NavPage, active: Boolean, index: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 35L); shown = true }
    val s by animateFloatAsState(if (shown) 1f else 0f, spring(dampingRatio = .55f, stiffness = 380f), label = "pop")
    Column(Modifier.width(66.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = s.coerceIn(0f, 1f) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(56.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(if (active) scheme.primaryContainer else scheme.surface).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(pageIcon(d.id), d.title, tint = scheme.primary, modifier = Modifier.size(26.dp))
        }
        Text(shortTitle(d), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PillBar(prefs: NavPrefs, page: String, onGo: (String) -> Unit, nav: NavState) {
    val glass = prefs.look == "glass"
    val color = barColor(prefs, glass)
    val group = NavDests.groupOf(page)
    val idx = NavDests.groups.indexOf(group)
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Shell(RoundedCornerShape(50), color, Modifier.wrapContentWidth(), glass, 14.dp) {
            SlidingRow(NavDests.groups.size, idx, 62.dp, prefs.height.dp.coerceAtMost(64.dp), prefs, Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { i ->
                val g = NavDests.groups[i]
                NavCell(groupIcon(g), g, i == idx || nav.popupGroup == g, prefs, false, false, {
                    val ps = NavDests.pagesIn(g)
                    if (ps.size == 1) { nav.closeAll(); onGo(ps.first().id) } else { val open = nav.popupGroup == g; nav.closeAll(); if (!open) nav.popupGroup = g }
                })
            }
        }
    }
}

// ------------------------------------------------------------------ SIDE

@Composable
fun NavSide(prefs: NavPrefs, page: String, onGo: (String) -> Unit, nav: NavState, modifier: Modifier = Modifier) {
    if (prefs.region != "side") return
    TrackLast(nav, page)
    val glass = prefs.look == "glass"
    val color = barColor(prefs, glass)
    val group = NavDests.groupOf(page)
    val collapsible = prefs.layout == "collapsible"
    val expanded = collapsible && nav.sideExpanded
    val width by animateDpAsState(if (expanded) 236.dp else maxOf(64.dp, minOf(prefs.height.dp + 4.dp, 92.dp)), spring(dampingRatio = .85f, stiffness = Spring.StiffnessMediumLow), label = "sideW")
    val body: @Composable () -> Unit = {
        Column(Modifier.width(width).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(prefs.spacing.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (collapsible) Icon(if (expanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, contentDescription = if (expanded) "Collapse menu" else "Expand menu", modifier = Modifier.size(36.dp).clip(CircleShape).clickable { nav.closeAll(); nav.sideExpanded = !nav.sideExpanded }.padding(6.dp))
            NavDests.groups.forEach { g ->
                val ps = NavDests.pagesIn(g)
                val open = g in nav.openGroups
                if (expanded) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { if (ps.size == 1) onGo(ps.first().id) else if (open) nav.openGroups.remove(g) else nav.openGroups.add(g) }.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(groupIcon(g), null, tint = if (g == group) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .7f), modifier = Modifier.size(prefs.iconSize.dp))
                            Text(g, Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            if (ps.size > 1) Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = if (open) 180f else 0f })
                        }
                        if (ps.size > 1) AnimatedVisibility(open, enter = expandVertically(tween(260)) + fadeIn(tween(260)), exit = shrinkVertically(tween(200)) + fadeOut(tween(150))) {
                            Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                ps.forEach { p -> NavCell(pageIcon(p.id), p.title, NavDests.baseId(page) == p.id, prefs, true, true, { onGo(p.id) }, Modifier.fillMaxWidth(), forceLabel = true) }
                            }
                        }
                    }
                } else {
                    NavCell(groupIcon(g), g, g == group || nav.flyoutGroup == g, prefs, false, true, {
                        if (collapsible) { nav.sideExpanded = true; if (ps.size > 1 && g !in nav.openGroups) nav.openGroups.add(g); if (ps.size == 1) onGo(ps.first().id) }
                        else if (ps.size == 1) { nav.closeAll(); onGo(ps.first().id) } else { val o = nav.flyoutGroup == g; nav.closeAll(); if (!o) nav.flyoutGroup = g }
                    }, Modifier.fillMaxWidth().heightIn(min = 48.dp))
                }
            }
        }
    }
    when (prefs.look) {
        "floating", "glass" -> Box(modifier.padding(8.dp).fillMaxHeight()) { Shell(RoundedCornerShape(prefs.corner.dp), color, Modifier.fillMaxHeight(), glass, 12.dp, body) }
        else -> Shell(RoundedCornerShape(0.dp), color, modifier.fillMaxHeight(), false, 4.dp, body)
    }
}

// ------------------------------------------------------------------ OVERLAY (sheets, flyout, fan, search, full-screen menu)

@Composable
private fun PageTile(d: NavPage, active: Boolean, index: Int, staggered: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(!staggered) }
    LaunchedEffect(Unit) { if (staggered) { delay(index * 22L); shown = true } }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(300), label = "tileA")
    val bg by animateColorAsState(if (active) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = .55f), tween(200), label = "tileBg")
    Column(modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 40f; scaleX = .94f + .06f * a; scaleY = .94f + .06f * a }.clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(pageIcon(d.id), null, tint = if (active) scheme.onPrimaryContainer else scheme.primary, modifier = Modifier.size(26.dp))
        Text(d.title, fontSize = 11.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = if (active) scheme.onPrimaryContainer else scheme.onSurface)
    }
}

@Composable
private fun AllPagesPanel(page: String, onGo: (String) -> Unit, grid: Boolean, staggered: Boolean) {
    var i = 0
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NavDests.groups.forEach { g ->
            Text(g.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(top = 6.dp))
            val ps = NavDests.pagesIn(g)
            if (grid) ps.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d -> PageTile(d, NavDests.baseId(page) == d.id, i++, staggered, Modifier.weight(1f)) { onGo(d.id) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            } else ps.forEach { d ->
                val active = NavDests.baseId(page) == d.id
                val idx = i++
                var shown by remember { mutableStateOf(!staggered) }
                LaunchedEffect(Unit) { if (staggered) { delay(idx * 20L); shown = true } }
                val a by animateFloatAsState(if (shown) 1f else 0f, tween(280), label = "rowA")
                Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a; translationX = (1f - a) * 60f }.clip(RoundedCornerShape(16.dp)).background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { onGo(d.id) }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(pageIcon(d.id), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text(d.title, Modifier.padding(start = 14.dp), fontWeight = if (active) FontWeight.Black else FontWeight.Medium, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PagesPopup(group: String, page: String, onGo: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 16.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(group.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(start = 6.dp))
            val ps = NavDests.pagesIn(group)
            ps.chunked(3).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { c, d -> PageTile(d, NavDests.baseId(page) == d.id, r * 3 + c, true, Modifier.width(92.dp)) { onGo(d.id) } }
                }
            }
        }
    }
}

@Composable
fun NavOverlay(prefs: NavPrefs, page: String, onGo: (String) -> Unit, nav: NavState, modifier: Modifier = Modifier, insets: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    TrackLast(nav, page)
    val l = prefs.layout
    val scrim = (nav.sheetOpen && l == "bottomMore") || (nav.popupGroup != null && (l == "pillMenu" || l == "fab" || l == "dock")) || (nav.flyoutGroup != null && l == "rail") || (nav.fanOpen && l == "fab")
    fun Modifier.nb() = if (insets) navigationBarsPadding() else this
    val go: (String) -> Unit = { nav.closeAll(); onGo(it) }
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(scrim, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .36f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { nav.closeAll() })
        }

        // Floating bars sit above the page, so content scrolls behind them
        if (l == "dock") Box(Modifier.align(Alignment.BottomCenter).nb()) { DockBar(prefs, page, onGo, nav) }
        if (l == "pillMenu") Box(Modifier.align(Alignment.BottomCenter).nb()) { PillBar(prefs, page, onGo, nav) }

        // More sheet
        AnimatedVisibility(nav.sheetOpen && l == "bottomMore", Modifier.align(Alignment.BottomCenter), enter = slideInVertically(spring(dampingRatio = .85f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(), exit = slideOutVertically(tween(220)) { it } + fadeOut()) {
            Surface(Modifier.fillMaxWidth().fillMaxHeight(.74f), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), color = scheme.surface, shadowElevation = 20.dp) {
                Column(Modifier.nb()) {
                    Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(50)).background(scheme.outlineVariant))
                    Text("All pages", Modifier.padding(start = 20.dp, top = 10.dp), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    AllPagesPanel(page, go, grid = true, staggered = true)
                }
            }
        }

        // Flyout beside the rail
        AnimatedVisibility(nav.flyoutGroup != null && l == "rail", Modifier.align(Alignment.CenterStart).padding(start = maxOf(64.dp, minOf(prefs.height.dp + 4.dp, 92.dp)) + if (prefs.look == "flat") 4.dp else 12.dp), enter = slideInHorizontally(spring(dampingRatio = .8f, stiffness = Spring.StiffnessMediumLow)) { -it / 2 } + fadeIn(), exit = slideOutHorizontally(tween(180)) { -it / 2 } + fadeOut()) {
            val g = nav.flyoutGroup ?: ""
            Surface(shape = RoundedCornerShape(24.dp), color = scheme.surface, shadowElevation = 16.dp) {
                Column(Modifier.width(220.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(g.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, color = scheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(start = 10.dp, bottom = 4.dp))
                    NavDests.pagesIn(g).forEach { p -> NavCell(pageIcon(p.id), p.title, NavDests.baseId(page) == p.id, prefs, true, true, { go(p.id) }, Modifier.fillMaxWidth(), forceLabel = true) }
                }
            }
        }

        // Pop-up of a section's pages (pill menu and fan menu)
        AnimatedVisibility(nav.popupGroup != null && (l == "pillMenu" || l == "fab" || l == "dock"), Modifier.align(if (l == "fab") Alignment.BottomEnd else Alignment.BottomCenter).nb().padding(bottom = if (l == "fab") 100.dp else if (l == "pillMenu") prefs.height.dp.coerceAtMost(64.dp) + 44.dp else prefs.height.dp + 56.dp, end = if (l == "fab") 22.dp else 0.dp), enter = scaleIn(spring(dampingRatio = .7f, stiffness = 380f), initialScale = .7f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (l == "fab") 1f else .5f, 1f)) + fadeIn(), exit = scaleOut(tween(150), targetScale = .85f) + fadeOut(tween(150))) {
            val g = nav.popupGroup ?: "Home"
            CirclePopup(NavDests.tabPages(g), g, page, go, end = l == "fab")
        }

        // Fan menu
        if (l == "fab") {
            val prog by animateFloatAsState(if (nav.fanOpen) 1f else 0f, spring(dampingRatio = .62f, stiffness = 320f), label = "fan")
            val glass = prefs.look == "glass"
            val groups = NavDests.groups
            val activeG = NavDests.groupOf(page)
            Box(Modifier.align(Alignment.BottomEnd).nb().padding(end = 20.dp, bottom = 20.dp)) {
                groups.forEachIndexed { i, g ->
                    val a = Math.toRadians(184.0 + 82.0 * i / (groups.size - 1))
                    val r = 190f * prog
                    Column(
                        Modifier.offset(x = (cos(a) * r).dp, y = (sin(a) * r).dp).size(56.dp).graphicsLayer { alpha = prog; scaleX = .5f + .5f * prog; scaleY = .5f + .5f * prog }.clip(CircleShape).background(if (g == activeG) scheme.primaryContainer else scheme.surface).border(1.dp, scheme.outlineVariant, CircleShape)
                            .clickable(enabled = nav.fanOpen) {
                                val ps = NavDests.pagesIn(g)
                                if (ps.size == 1) go(ps.first().id) else { nav.fanOpen = false; nav.popupGroup = g }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) { Icon(groupIcon(g), g, tint = scheme.primary, modifier = Modifier.size(22.dp)); Text(g, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                }
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(if (glass) scheme.primary.copy(alpha = .78f) else scheme.primary).clickable { val o = nav.fanOpen; nav.closeAll(); nav.fanOpen = !o },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, if (nav.fanOpen) "Close menu" else "Open menu", tint = scheme.onPrimary, modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = 135f * prog }) }
            }
        }

        // Full-screen menu (opened by the header button)
        AnimatedVisibility(nav.menuOpen && l == "overlay", enter = fadeIn(tween(240)) + scaleIn(tween(320), initialScale = .94f), exit = fadeOut(tween(180)) + scaleOut(tween(200), targetScale = .96f)) {
            Surface(Modifier.fillMaxSize(), color = scheme.background.copy(alpha = .98f)) {
                Column(Modifier.then(if (insets) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)) {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Where to?", Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 26.sp)
                        Box(Modifier.size(42.dp).clip(CircleShape).background(scheme.surface).clickable { nav.closeAll() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, "Close menu") }
                    }
                    AllPagesPanel(page, go, grid = prefs.look != "list", staggered = true)
                }
            }
        }

        // Search menu
        if (l == "search") {
            val glass = prefs.look == "glass"
            Column(Modifier.align(Alignment.BottomCenter).nb().then(if (insets) Modifier.imePadding() else Modifier).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(nav.searchOpen, enter = expandVertically(spring(dampingRatio = .85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(), exit = shrinkVertically(tween(200)) + fadeOut()) {
                    var q by remember { mutableStateOf("") }
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                    val hits = remember(q) { val t = q.trim(); if (t.isEmpty()) NavDests.all.filter { it.id in NavDests.quick + "goals" + "insights" } else NavDests.all.filter { it.title.contains(t, true) || it.group.contains(t, true) || it.id.contains(t, true) } }.take(7)
                    Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(26.dp), color = if (glass) scheme.surface.copy(alpha = .8f) else scheme.surface, shadowElevation = 18.dp) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(scheme.surfaceVariant.copy(alpha = .6f)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = scheme.primary)
                                Box(Modifier.weight(1f).padding(start = 10.dp)) {
                                    if (q.isEmpty()) Text("Jump to a page…", color = scheme.onSurface.copy(alpha = .5f))
                                    BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(color = scheme.onSurface, fontSize = 16.sp), cursorBrush = SolidColor(scheme.primary), modifier = Modifier.fillMaxWidth().focusRequester(focus))
                                }
                            }
                            if (hits.isEmpty()) Text("No page matches “$q”", Modifier.padding(12.dp), color = scheme.onSurface.copy(alpha = .6f))
                            hits.forEach { d ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { go(d.id) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(pageIcon(d.id), null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                                    Text(d.title, Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Medium)
                                    Text(d.group, fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .5f))
                                }
                            }
                        }
                    }
                }
                Surface(Modifier.clip(RoundedCornerShape(50)).clickable { val o = nav.searchOpen; nav.closeAll(); nav.searchOpen = !o }, shape = RoundedCornerShape(50), color = if (glass) scheme.primary.copy(alpha = .8f) else scheme.primary, shadowElevation = 12.dp) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (nav.searchOpen) Icons.Default.Close else Icons.Default.Search, null, tint = scheme.onPrimary)
                        Text(if (nav.searchOpen) "Close" else NavDests.title(page), color = scheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ STUDIO (lives in Settings → Customization)

@Composable
private fun StudioLabel(text: String) = Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Row { Text(label, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(display, fontWeight = FontWeight.Black, fontSize = 13.sp) }
        Slider(value, onChange, valueRange = range)
    }
}

/** A small phone running the real navigation composables with the draft settings. */
@Composable
private fun NavPreview(prefs: NavPrefs) {
    val scheme = MaterialTheme.colorScheme
    val nav = remember(prefs.layout) { NavState() }
    var page by remember { mutableStateOf("overview") }
    val go: (String) -> Unit = { page = it }
    val content: @Composable (Modifier) -> Unit = { m ->
        Column(m.background(scheme.background).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(NavDests.title(page), Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
                if (prefs.layout == "overlay") Box(Modifier.size(34.dp).clip(CircleShape).background(scheme.surface).clickable { nav.menuOpen = true }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Menu, "Open menu", Modifier.size(20.dp)) }
            }
            repeat(3) { Box(Modifier.fillMaxWidth().height(if (it == 0) 50.dp else 30.dp).clip(RoundedCornerShape(14.dp)).background(scheme.surfaceVariant.copy(alpha = .8f))) }
        }
    }
    Box(Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(26.dp)).border(2.dp, scheme.outline.copy(alpha = .4f), RoundedCornerShape(26.dp)).background(scheme.background)) {
        when (prefs.region) {
            "top" -> Column(Modifier.fillMaxSize()) { NavTop(prefs, page, go, nav); content(Modifier.weight(1f).fillMaxWidth()) }
            "bottom" -> Column(Modifier.fillMaxSize()) { content(Modifier.weight(1f).fillMaxWidth()); NavBottom(prefs, page, go, nav) }
            "side" -> Row(Modifier.fillMaxSize()) { NavSide(prefs, page, go, nav); content(Modifier.weight(1f).fillMaxHeight()) }
            "drawer" -> Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(130.dp).fillMaxHeight().background(scheme.surface).padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("FinCip", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(6.dp))
                    NavDests.groups.forEach { g -> Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(groupIcon(g), null, Modifier.size(18.dp), tint = scheme.primary); Text(g, Modifier.padding(start = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                }
                content(Modifier.weight(1f).fillMaxHeight())
            }
            else -> content(Modifier.fillMaxSize())
        }
        NavOverlay(prefs, page, go, nav, Modifier.fillMaxSize(), insets = false)
    }
}

/** The whole studio as a column of cards, with no scroll of its own, so it can sit inside the Customization tab. */
@Composable
fun NavStudioContent(current: NavPrefs, onApply: (NavPrefs) -> Unit, onReset: () -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    var category by remember(current) { mutableStateOf(NavLayouts.of(current.layout).category) }
    fun set(p: NavPrefs) { draft = p.sanitized() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NeoTitle("MAKE IT YOURS", "Navigation Studio", "Pick how you move around FinCip. Only the layout you choose is shown, and every layout reaches every page.")
        NavPreview(draft)
        NeoCard {
            StudioLabel("TYPE")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavLayouts.categories.forEach { c -> PopChip(c, category == c, { category = c }) }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(category, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "layouts") { c ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavLayouts.inCategory(c).forEach { l ->
                        val on = draft.layout == l.id
                        val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), tween(220), label = "cardBg")
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).clickable { set(draft.copy(layout = l.id)) }.padding(14.dp)) {
                            Text(l.title, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text(l.blurb, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                        }
                    }
                }
            }
            val looks = NavLayouts.of(draft.layout).looks
            if (looks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp)); StudioLabel("STYLE"); Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    looks.forEach { (id, label) -> PopChip(label, draft.look == id, { set(draft.copy(look = id)) }) }
                }
            }
        }
        if (!draft.isDefaultDrawer) {
            NeoCard {
                StudioLabel("CONTENT")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("both" to "Icons + labels", "icons" to "Icons only", "text" to "Text only").forEach { (id, l) -> PopChip(l, draft.labels == id, { set(draft.copy(labels = id)) }) }
                }
                Spacer(Modifier.height(12.dp)); StudioLabel("ACTIVE ITEM"); Spacer(Modifier.height(8.dp))
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
                SliderRow("Size", draft.height.toFloat(), 48f..96f, "${draft.height} dp") { set(draft.copy(height = it.toInt())) }
            }
        } else Text("The classic drawer keeps its own look. Pick another type above for more controls.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoOutlineButton("Reset to default", { draft = NavPrefs(); category = "Sidebar"; onReset() }, Modifier.weight(1f))
            NeoButton("Apply", Modifier.weight(1f), onClick = { onApply(draft) })
        }
        Text("Saved on this device and included in your FinCip backup.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
    }
}
