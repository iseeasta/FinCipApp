package com.fincip.nativeapp.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.NavPage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate

val SidebarWidth = 288.dp

fun groupIcon(group: String): ImageVector = when (group) {
    "Home" -> Icons.Default.Home
    "Money" -> Icons.Default.Wallet
    "Plan" -> Icons.Default.Flag
    "You" -> Icons.Default.Person
    else -> Icons.Default.Explore
}

fun pageIcon(id: String): ImageVector = when (id) {
    "overview" -> Icons.Default.Dashboard
    "transactions" -> Icons.Default.History
    "dataio" -> Icons.Default.SwapHoriz
    "recurring" -> Icons.Default.Autorenew
    "calendar" -> Icons.Default.Event
    "subscriptions" -> Icons.Default.Receipt
    "currency" -> Icons.Default.CurrencyExchange
    "budgets" -> Icons.Default.PieChart
    "goals" -> Icons.Default.Flag
    "networth" -> Icons.Default.Diamond
    "simulator" -> Icons.Default.Science
    "india" -> Icons.Default.AccountBalance
    "tax" -> Icons.Default.Calculate
    "portfolio" -> Icons.Default.BarChart
    "forecast" -> Icons.Default.Timeline
    "insights" -> Icons.Default.Insights
    "world" -> Icons.Default.Face
    "social" -> Icons.Default.QrCode2
    "universe" -> Icons.Default.Public
    "rules" -> Icons.Default.Bolt
    "receipts" -> Icons.Default.ReceiptLong
    "household" -> Icons.Default.Groups
    "security" -> Icons.Default.Shield
    "settings" -> Icons.Default.Settings
    "profile" -> Icons.Default.Person
    "customize" -> Icons.Default.Palette
    else -> Icons.Default.Circle
}


private val SettingIds = setOf("security", "settings")

/**
 * Left navigation drawer. The logo in its header (and the matching logo in the page header when it is
 * hidden) toggles it. Sections are accordions: tap a section to reveal its pages under a thin tree line.
 */
@Composable
fun FinCipSidebar(
    pages: List<NavPage>,
    selectedPage: String,
    profile: ProfileData,
    dark: Boolean,
    onToggle: () -> Unit,
    onProfile: () -> Unit,
    onSelect: (NavPage) -> Unit,
    onVoice: () -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = SidebarWidth
) {
    val scheme = MaterialTheme.colorScheme
    val selectedGroup = pages.firstOrNull { it.id == selectedPage }?.group ?: "Home"
    var open by remember { mutableStateOf(setOf(selectedGroup)) }
    LaunchedEffect(selectedGroup) { open = open + selectedGroup }
    val main = pages.filter { it.group == "Home" }
    val sections = listOf("Money", "Plan", "Explore")
    val settingsPages = pages.filter { it.id in SettingIds }

    Surface(
        modifier.width(width).fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        color = scheme.surface.copy(alpha = .55f), shadowElevation = 0.dp,
        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = .55f), scheme.primary.copy(alpha = .3f), Color.White.copy(alpha = .15f))))
    ) {
        Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(scheme.primary.copy(alpha = .22f), Color.Transparent, scheme.tertiary.copy(alpha = .18f)))).statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Logo row: same place as the page-header logo, so tapping it feels like one control.
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                FinCipMark(Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = "Hide sidebar", onClick = onToggle), size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Text("FinCip", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.6).sp, color = scheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel("Main")
                main.forEach { pg -> TopItem(pageIcon(pg.id), pg.title, pg.id == selectedPage, onClick = { onSelect(pg) }) }
                sections.forEach { group ->
                    val items = pages.filter { it.group == group && it.id !in SettingIds }
                    if (items.isNotEmpty()) {
                        val isOpen = group in open
                        TopItem(
                            groupIcon(group), group, selected = isOpen && selectedGroup == group && items.none { it.id == selectedPage }.not(),
                            trailing = isOpen, onClick = { open = if (isOpen) open - group else open + group }
                        )
                        AnimatedVisibility(isOpen, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(180))) {
                            Row(Modifier.padding(start = 20.dp, top = 2.dp, bottom = 4.dp).height(IntrinsicSize.Min)) {
                                Box(Modifier.width(1.dp).fillMaxHeight().background(scheme.outlineVariant))
                                Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    items.forEach { pg -> SubItem(pageIcon(pg.id), pg.title, pg.id == selectedPage) { onSelect(pg) } }
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(20.dp)).background(scheme.onSurface.copy(alpha = .07f)).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = "Open profile", onClick = onProfile).padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(profile, size = 38.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(profile.tagline.ifBlank { "FinCip member" }, fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconTile(Icons.Default.Mic, "Voice entry", onVoice)
                IconTile(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, if (dark) "Light mode" else "Dark mode", onTheme)
                IconTile(Icons.Default.Settings, "Settings", onSettings)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f), modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 4.dp))
}

@Composable
private fun TopItem(icon: ImageVector, label: String, selected: Boolean, trailing: Boolean? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.primary else Color.Transparent, tween(160), label = "tiBg")
    val rotation by animateFloatAsState(if (trailing == true) 180f else 0f, tween(200), label = "chev")
    val tap = rememberTap()
    Row(
        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable(role = Role.Button, onClick = { tap(); onClick() }).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) scheme.onPrimary else scheme.onSurface.copy(alpha = .75f), modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp, color = if (selected) scheme.onPrimary else scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = scheme.onSurface.copy(alpha = .55f), modifier = Modifier.size(20.dp).rotate(rotation))
    }
}

@Composable
private fun SubItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.primary else Color.Transparent, tween(160), label = "siBg")
    val fg = if (selected) scheme.onPrimary else scheme.onSurface.copy(alpha = .8f)
    val tap = rememberTap()
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(bg).clickable(role = Role.Tab, onClick = { tap(); onClick() }).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tap = rememberTap()
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(scheme.surfaceVariant).clickable(role = Role.Button, onClick = { tap(); onClick() }).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
private fun IconTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tap = rememberTap()
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = label, onClick = { tap(); onClick() }), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = scheme.onSurface.copy(alpha = .85f), modifier = Modifier.size(20.dp))
    }
}
