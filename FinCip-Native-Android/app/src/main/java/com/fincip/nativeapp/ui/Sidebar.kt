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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Autorenew
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
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wallet
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

val SidebarCollapsedWidth = 60.dp
val SidebarExpandedWidth = 248.dp

private val Bone = Color(0xFFF3F0E7)

private fun groupIcon(group: String): ImageVector = when (group) {
    "Home" -> Icons.Default.Home
    "Money" -> Icons.Default.Wallet
    "Plan" -> Icons.Default.Flag
    else -> Icons.Default.Explore
}

private fun pageIcon(id: String): ImageVector = when (id) {
    "overview" -> Icons.Default.Dashboard
    "transactions" -> Icons.Default.SwapHoriz
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
    "portfolio" -> Icons.Default.ShowChart
    "forecast" -> Icons.Default.Timeline
    "insights" -> Icons.Default.Insights
    "rules" -> Icons.Default.Bolt
    "receipts" -> Icons.Default.ReceiptLong
    "household" -> Icons.Default.Groups
    "security" -> Icons.Default.Shield
    "settings" -> Icons.Default.Settings
    else -> Icons.Default.Circle
}

/**
 * Left navigation. Collapsed: logo plus one icon per section. Expanded: logo, wordmark and every
 * destination grouped under its section. The logo toggles the state. [width] is animated by the caller
 * so the content area can follow it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinCipSidebar(
    pages: List<NavPage>,
    selectedPage: String,
    expanded: Boolean,
    width: Dp,
    onToggle: () -> Unit,
    onSelect: (NavPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = FinCipInk
    val selectedGroup = pages.firstOrNull { it.id == selectedPage }?.group ?: "Home"
    val groups = pages.map { it.group }.distinct()
    val labelAlpha = ((width - SidebarCollapsedWidth) / (SidebarExpandedWidth - SidebarCollapsedWidth)).coerceIn(0f, 1f)
    val context = LocalContext.current
    Column(
        modifier.width(width).fillMaxHeight().clipToBounds().background(bg)
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        // Logo = toggle. Always visible, same position in both states.
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                .combinedClickable(role = Role.Button, onClickLabel = if (expanded) "Collapse sidebar" else "Expand sidebar", onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(2.dp))
            FinCipMark(size = 40.dp)
            if (labelAlpha > 0.05f) {
                Spacer(Modifier.width(10.dp))
                Text("FinCip", color = Bone.copy(alpha = labelAlpha), fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.6).sp, maxLines = 1, softWrap = false)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Bone.copy(alpha = .14f)))
        Spacer(Modifier.height(10.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            groups.forEachIndexed { index, group ->
                val items = pages.filter { it.group == group }
                if (expanded) {
                    if (index > 0) { Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Bone.copy(alpha = .10f))); Spacer(Modifier.height(8.dp)) }
                    Text(
                        group.uppercase(), color = Bone.copy(alpha = .45f * labelAlpha), style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.6.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp)
                    )
                    items.forEach { p ->
                        SidebarItem(pageIcon(p.id), p.title, p.id == selectedPage, labelAlpha, onClick = { onSelect(p) }, onHint = { Toast.makeText(context, p.title, Toast.LENGTH_SHORT).show() })
                    }
                } else {
                    SidebarItem(groupIcon(group), group, group == selectedGroup, 0f, onClick = { onSelect(items.first()) }, onHint = { Toast.makeText(context, group, Toast.LENGTH_SHORT).show() })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarItem(icon: ImageVector, label: String, selected: Boolean, labelAlpha: Float, onClick: () -> Unit, onHint: () -> Unit) {
    val bg by animateColorAsState(if (selected) FinCipLime else Color.Transparent, tween(180), label = "sbBg")
    val fg by animateColorAsState(if (selected) FinCipInk else Bone.copy(alpha = .78f), tween(180), label = "sbFg")
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .combinedClickable(role = Role.Tab, onLongClick = onHint, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
        if (labelAlpha > 0.05f) {
            Spacer(Modifier.width(12.dp))
            Text(label, color = fg.copy(alpha = fg.alpha * labelAlpha), fontWeight = if (selected) FontWeight.Black else FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}
