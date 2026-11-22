package com.fincip.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.fincip.nativeapp.data.currentMonth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.CURRENCY_INFO
import com.fincip.nativeapp.data.arrayOrEmpty
import com.fincip.nativeapp.data.asObjectOrNull
import com.fincip.nativeapp.data.asStringOr
import com.fincip.nativeapp.data.bool
import com.fincip.nativeapp.data.double
import com.fincip.nativeapp.data.expenseTotal
import com.fincip.nativeapp.data.fxRate
import com.fincip.nativeapp.data.incomeTotal
import com.fincip.nativeapp.data.money
import com.fincip.nativeapp.data.monthlyTransactions
import com.fincip.nativeapp.data.nextDueLabel
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.objects
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.toBase
import com.fincip.nativeapp.data.transactions
import com.fincip.nativeapp.ui.Field
import com.fincip.nativeapp.ui.FinCipMark
import com.fincip.nativeapp.ui.ProfileAvatar
import com.fincip.nativeapp.ui.ProfileData
import com.fincip.nativeapp.ui.MiniBars
import com.fincip.nativeapp.ui.PetBlob
import com.fincip.nativeapp.ui.PetMood
import com.fincip.nativeapp.ui.LineChart
import com.fincip.nativeapp.ui.ChartSeries
import com.fincip.nativeapp.ui.DonutChart
import com.fincip.nativeapp.ui.RingGauge
import com.fincip.nativeapp.ui.LegendRow
import com.fincip.nativeapp.ui.ChartColors
import com.fincip.nativeapp.ui.compactMoney
import com.fincip.nativeapp.ui.Pop
import com.fincip.nativeapp.ui.PopField
import com.fincip.nativeapp.ui.PopChip
import com.fincip.nativeapp.ui.contentOn
import com.fincip.nativeapp.data.parseMonth
import com.fincip.nativeapp.ui.MetricTile
import com.fincip.nativeapp.ui.NeoButton
import com.fincip.nativeapp.ui.NeoCard
import com.fincip.nativeapp.ui.NeoOutlineButton
import com.fincip.nativeapp.ui.NeoTitle
import com.fincip.nativeapp.ui.ProgressBar
import com.fincip.nativeapp.ui.Selector
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow

private val Green = Color(0xFF4F9B69)
private val Red = Color(0xFFD66B67)
private val Gold = Color(0xFFE7B83F)
private val Blue = Color(0xFF579CD4)

/** True when a screen is shown inside the Profile hub, which already scrolls and has its own header. */
val LocalEmbedded = androidx.compose.runtime.compositionLocalOf { false }

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    if (LocalEmbedded.current) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    else Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}

/** Soft header card: pale green block with leaf blobs, big title, short description and the FinCip mark. */
@Composable
private fun PageHeading(eyebrow: String, title: String, description: String = "") {
    if (LocalEmbedded.current) return
    val fill = MaterialTheme.colorScheme.primaryContainer
    val fg = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Pop.Radius)).background(fill).drawBehind {
            drawCircle(Color.White.copy(alpha = .35f), radius = size.minDimension * .75f, center = Offset(size.width * 1.0f, -size.height * .1f))
            drawCircle(Pop.Teal.copy(alpha = .12f), radius = size.minDimension * .42f, center = Offset(size.width * .92f, size.height))
        }.padding(22.dp)
    ) {
        FinCipMark(Modifier.align(Alignment.TopEnd), size = 64.dp)
        Column(Modifier.fillMaxWidth(.72f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = fg.copy(alpha = .7f))
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .78f))
        }
    }
}


@Composable
private fun CardLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = LocalContentColor.current.copy(alpha = .62f))
    Spacer(Modifier.height(8.dp))
}

private fun spendByCategory(workspace: JsonObject, month: String): List<Pair<String, Double>> =
    monthlyTransactions(workspace, month).filter { it.string("type") != "income" }
        .groupBy { it.string("category", "Other").ifBlank { "Other" } }
        .mapValues { (_, v) -> v.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) } }
        .toList().sortedByDescending { it.second }

/** Donut + legend for any list of (name, amount) pairs. */
@Composable
private fun BreakdownCard(title: String, groups: List<Pair<String, Double>>, privacy: Boolean, base: String, centerLabel: String, emptyText: String) {
    NeoCard {
        CardLabel(title)
        val total = groups.sumOf { it.second }
        if (groups.isEmpty() || total <= 0.0) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall)
        } else {
            val top = groups.take(5)
            val other = groups.drop(5).sumOf { it.second }
            val slices = top.mapIndexed { i, (_, v) -> ChartColors[i % ChartColors.size] to v } + (if (other > 0) listOf(Color(0xFFB0A8C8) to other) else emptyList())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutChart(slices, centerTop = if (privacy) "••••" else compactMoney(total, base), centerBottom = centerLabel, diameter = 132.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    top.forEachIndexed { i, (name, v) -> LegendRow(ChartColors[i % ChartColors.size], name, if (privacy) "••" else "${(v / total * 100).toInt()}%") }
                    if (other > 0) LegendRow(Color(0xFFB0A8C8), "Other", if (privacy) "••" else "${(other / total * 100).toInt()}%")
                }
            }
        }
    }
}

@Composable
private fun CashFlowCard(workspace: JsonObject, month: String, privacy: Boolean, base: String) {
    val months = (5 downTo 0).map { parseMonth(month).minusMonths(it.toLong()) }
    val income = months.map { incomeTotal(workspace, it.toString()) }
    val spend = months.map { expenseTotal(workspace, it.toString()) }
    NeoCard {
        CardLabel("Cash flow · last 6 months")
        LineChart(
            labels = months.map { it.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) },
            series = listOf(ChartSeries("Income", Pop.Teal, income), ChartSeries("Spending", Pop.Coral, spend)),
            currency = base, hideValues = privacy
        )
    }
}

@Composable
private fun WeeklySpendCard(workspace: JsonObject, month: String, privacy: Boolean) {
    val ym = parseMonth(month)
    val weekCount = if (ym.lengthOfMonth() > 28) 5 else 4
    val spendTx = monthlyTransactions(workspace, month).filter { it.string("type") != "income" }
    val weeks = (0 until weekCount).map { w ->
        val lo = w * 7 + 1
        val hi = if (w == weekCount - 1) 31 else w * 7 + 7
        if (privacy) 0.0 else spendTx.filter { (it.string("date").takeLast(2).toIntOrNull() ?: 0) in lo..hi }
            .sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
    }
    NeoCard {
        CardLabel("Spending by week · $month")
        MiniBars(values = weeks, labels = (1..weekCount).map { "W$it" }, highlight = weeks.indexOf(weeks.maxOrNull() ?: 0.0).coerceAtLeast(0), accent = Pop.Coral)
    }
}

private fun budgetUsed(workspace: JsonObject, month: String, b: JsonObject): Double {
    val cur = b.string("currency", "INR")
    return monthlyTransactions(workspace, month).filter { it.string("type") != "income" }.sumOf { tx ->
        if (tx.string("currency", "INR") != cur) 0.0 else {
            val splits = tx.arrayOrEmpty("splits").objects()
            if (splits.isNotEmpty()) splits.filter { it.string("category") == b.string("category") }.sumOf { it.double("amount") }
            else if (tx.string("category") == b.string("category")) tx.double("amount") else 0.0
        }
    }
}


/** Consecutive days (ending today, or yesterday if nothing logged yet today) with at least one entry. */
private fun entryStreak(workspace: JsonObject): Int {
    val days = transactions(workspace).mapNotNull { runCatching { LocalDate.parse(it.string("date")) }.getOrNull() }.toSet()
    var d = LocalDate.now()
    if (d !in days) d = d.minusDays(1)
    var n = 0
    while (d in days) { n++; d = d.minusDays(1) }
    return n
}

/** A badge is unlocked purely from the user's own data; nothing here is fabricated. */
@Composable
private fun BadgeRow(badges: List<Pair<String, Boolean>>) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        badges.forEach { (label, unlocked) ->
            Text(
                (if (unlocked) "✓ " else "○ ") + label,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
                color = if (unlocked) Pop.Ink else MaterialTheme.colorScheme.onBackground.copy(alpha = .5f),
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (unlocked) Pop.Sun else Color.Transparent)
                    .border(1.dp, if (unlocked) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun InlineAction(text: String, onClick: () -> Unit) {
    Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp))
}

@Composable
fun OverviewScreen(workspace: JsonObject, month: String, privacy: Boolean, savingsRate: Double, balance: Double, onAdd: () -> Unit, onMascot: () -> Unit, onOpen: (String) -> Unit = {}, onNewGoal: () -> Unit = {}, onEditTx: (JsonObject) -> Unit = {}) {
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val income = incomeTotal(workspace, month)
    val expenses = expenseTotal(workspace, month)
    val allTransactions = transactions(workspace)
    val takeout = monthlyTransactions(workspace, month).filter { t ->
        t.string("type") != "income" && (t.string("category").contains("food", true) || t.string("title").contains("takeout", true) || t.string("title").contains("restaurant", true))
    }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
    val foodBudget = workspace.arrayOrEmpty("budgets").objects().filter { it.string("category").contains("food", true) }.sumOf { toBase(workspace, it.double("limit"), it.string("currency", base)) }
    val ramen = foodBudget > 0 && takeout > foodBudget
    val pet = workspace.objectOrEmpty("mascot")
    val achievedGoal = workspace.arrayOrEmpty("goals").objects().any { it.string("type") != "debt" && it.double("target") > 0 && it.double("saved") >= it.double("target") }
    val contributingGoal = workspace.arrayOrEmpty("goals").objects().any { it.string("type") != "debt" && it.double("monthlyContribution") > 0 && it.double("saved") > 0 }
    val savingMood = achievedGoal || (savingsRate >= 20 && income > 0) || contributingGoal
    val mascotAccent = runCatching { Color(android.graphics.Color.parseColor(pet.string("accent", "#D8FF3E"))) }.getOrElse { Color(0xFFFFD166) }
    val mascotEmoji = when { ramen -> "🍜"; achievedGoal -> "🥳"; savingMood -> when (pet.string("kind", "🐱")) { "🌱" -> "🌳"; "🤖" -> "🤖✨"; "🦊" -> "🦊💛"; else -> "😸" }; expenses > income && income > 0 -> "😵"; else -> pet.string("kind", "🐱") }
    val mood = when { ramen -> "Ramen mode · takeout has crossed the food budget"; achievedGoal -> "Goal unlocked · you reached a savings milestone"; savingMood -> "Thriving · consistent contributions are helping future-you"; expenses > income && income > 0 -> "A little stressed · spending is ahead of income"; else -> "Taking it one transaction at a time" }
    AnalyticsOverview(workspace, month, privacy, base, onAdd, onOpen, onNewGoal, onEditTx) {
        run {
            val petMood = when { ramen -> PetMood.RAMEN; achievedGoal -> PetMood.CELEBRATE; savingMood -> PetMood.THRIVING; expenses > income && income > 0 -> PetMood.STRESSED; else -> PetMood.HAPPY }
            val streak = entryStreak(workspace)
            val xp = allTransactions.size * 10 + workspace.arrayOrEmpty("budgets").size() * 25 + workspace.arrayOrEmpty("goals").size() * 25 + workspace.arrayOrEmpty("financeProducts").size() * 25 + (if (achievedGoal) 200 else 0)
            val level = xp / 100 + 1
            val condition = when { ramen -> "ramen"; achievedGoal -> "goal"; expenses > income && income > 0 -> "stress"; savingMood -> "saving"; streak >= 7 -> "streak"; allTransactions.isEmpty() -> "empty"; else -> "neutral" }
            PetHomeCard(workspace, petMood, condition, streak, onMascot)
        }
    }
}

@Composable
fun TransactionsScreen(workspace: JsonObject, month: String, privacy: Boolean, query: String, onQuery: (String) -> Unit, onAdd: () -> Unit, onEdit: (JsonObject) -> Unit, onDelete: (JsonObject) -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val filtered = monthlyTransactions(workspace, month).filter { item ->
        val tags = item.arrayOrEmpty("tags").joinToString(" ") { it.asStringOr() }
        item.string("title").contains(query, true) || item.string("category").contains(query, true) ||
            item.string("person").contains(query, true) || item.string("project").contains(query, true) || tags.contains(query, true)
    }
    PageColumn {
        PageHeading("EVERY RUPEE, ACCOUNTED FOR", "Keep the details clear.", "Search, import and manage transactions. Bank statements can be CSV or OFX.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { MetricTile("Income", if (privacy) "••••" else compactMoney(incomeTotal(workspace, month), base), month, fill = Pop.Mint) }
            Box(Modifier.weight(1f)) { MetricTile("Spent", if (privacy) "••••" else compactMoney(expenseTotal(workspace, month), base), "${filtered.size} entries", fill = Pop.Coral) }
        }
        WeeklySpendCard(workspace, month, privacy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NeoButton("+ Add transaction", Modifier.weight(1f), onAdd); NeoOutlineButton("Import statement", onImport, Modifier.weight(1f)) }
        PopField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), label = { Text("Search transaction, person or project") })
        Row(verticalAlignment = Alignment.CenterVertically) { Text("${filtered.size} transactions · $month", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium); InlineAction("Export CSV ↓", onExport) }
        if (filtered.isEmpty()) NeoCard { Text("No transactions for this month", fontWeight = FontWeight.Black); Text("Add an entry or import a bank statement.", style = MaterialTheme.typography.bodySmall) }
        filtered.sortedByDescending { it.string("date") }.forEach { item -> TransactionCard(item, privacy, { onEdit(item) }, { onDelete(item) }) }
        Text("Converted summaries use ${base}. Original transaction amounts remain unchanged.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
    }
}

@Composable
private fun TransactionCard(item: JsonObject, privacy: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val income = item.string("type") == "income"
    NeoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(42.dp).background(if (income) Pop.Mint else Pop.Coral.copy(alpha = .28f), CircleShape), contentAlignment = Alignment.Center) { Text(if (income) "↙" else "↗", fontWeight = FontWeight.Black, color = Pop.Ink) }
            Column(Modifier.weight(1f)) {
                Text(item.string("title", "Transaction"), fontWeight = FontWeight.Black)
                Text("${item.string("category", "Other")} · ${item.string("date")} · ${item.string("account", "Account")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                val extra = listOf(item.string("person"), item.string("project")).filter { it.isNotBlank() }.joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (privacy) "••••" else (if (income) "+" else "−") + money(item.double("amount"), item.string("currency", "INR")), color = if (income) Green else Red, fontWeight = FontWeight.Black)
                Row {
                    Text("Edit", style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable(onClick = onClick).padding(4.dp))
                    Text("×", style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(onClick = onDelete).padding(4.dp))
                }
            }
        }
        val splits = item.arrayOrEmpty("splits").objects()
        if (splits.isNotEmpty()) {
            Divider(Modifier.padding(vertical = 7.dp))
            splits.forEach { Text("• ${it.string("category")}: ${money(it.double("amount"), item.string("currency", "INR"))}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun RecurringScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onDelete: (JsonObject) -> Unit, onMarkPaid: (JsonObject) -> Unit) {
    PageColumn {
        PageHeading("UPCOMING COMMITMENTS", "Bills, without surprises.", "EMIs, subscriptions, rent and recurring income. Reminders are local notifications; no payments are made automatically.")
        run {
            val all = workspace.arrayOrEmpty("recurringRules").objects()
            val cur = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
            val out = all.filter { it.string("type") != "income" }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
            val inn = all.filter { it.string("type") == "income" }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
            val nextRule = all.filter { it.string("nextDate").isNotBlank() }.minByOrNull { it.string("nextDate") }
            NeoCard(fill = Pop.Peach) {
                CardLabel("Per-cycle commitments")
                Text(if (privacy) "••••" else money(out, cur), fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Text(if (nextRule != null) "Next up: ${nextRule.string("title", "Payment")} · ${nextDueLabel(nextRule.string("nextDate"))}" else "Nothing scheduled yet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SmallPill("${all.size} rules"); SmallPill("Income in ${if (privacy) "••••" else compactMoney(inn, cur)}") }
            }
        }
        NeoButton("+ Add bill or repeat", onClick = onAdd)
        val rules = workspace.arrayOrEmpty("recurringRules").objects().sortedBy { it.string("nextDate") }
        if (rules.isEmpty()) NeoCard { Text("No recurring payments yet", fontWeight = FontWeight.Black); Text("Set a reminder for rent, EMI, subscriptions or salary.", style = MaterialTheme.typography.bodySmall) }
        rules.forEach { item ->
            NeoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.string("title", "Recurring payment"), fontWeight = FontWeight.Black)
                        Text("${item.string("frequency", "monthly").replaceFirstChar { it.uppercase() }} · ${item.string("nextDate")}", style = MaterialTheme.typography.bodySmall)
                        Text(nextDueLabel(item.string("nextDate")), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (privacy) "••••" else money(item.double("amount"), item.string("currency", "INR")), fontWeight = FontWeight.Black)
                        Text("Mark paid", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { onMarkPaid(item) }.padding(4.dp))
                        Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { onDelete(item) }.padding(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit) {
    val subs = workspace.arrayOrEmpty("recurringRules").objects().filter { it.string("title").contains("subscription", true) || it.string("category").contains("subscription", true) || it.string("category").contains("stream", true) }
    PageColumn {
        PageHeading("REPEAT SPENDING", "Subscriptions, gathered.", "Keep recurring apps, streaming services and memberships visible in one place.")
        NeoButton("+ Add subscription reminder", onClick = onAdd)
        if (subs.isNotEmpty()) BreakdownCard("Share of subscription spend", subs.map { it.string("title", "Subscription") to toBase(workspace, it.double("amount"), it.string("currency", "INR")) }, privacy, workspace.objectOrEmpty("settings").string("baseCurrency", "INR"), "per cycle", "")
        MetricTile("Monthly subscription total", if (privacy) "••••" else money(subs.filter { it.string("frequency", "monthly") == "monthly" }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }, workspace.objectOrEmpty("settings").string("baseCurrency", "INR")), "Converted from each subscription currency")
        if (subs.isEmpty()) NeoCard { Text("No subscriptions tagged yet", fontWeight = FontWeight.Black); Text("Add a recurring rule with “Subscription” in the name or category.", style = MaterialTheme.typography.bodySmall) }
        subs.forEach { item -> NeoCard { Text(item.string("title"), fontWeight = FontWeight.Black); Text("${item.string("nextDate")} · ${item.string("frequency")}"); Text(if (privacy) "••••" else money(item.double("amount"), item.string("currency", "INR")), fontWeight = FontWeight.Black) } }
    }
}

@Composable
fun CalendarScreen(workspace: JsonObject, month: String, onMonthChange: (String) -> Unit) {
    val monthDate = runCatching { YearMonth.parse(month) }.getOrElse { YearMonth.now() }
    var picker by remember { mutableStateOf(false) }
    var selected by remember(month) { mutableStateOf(LocalDate.now().takeIf { YearMonth.from(it) == monthDate } ?: monthDate.atDay(1)) }
    val tx = monthlyTransactions(workspace, month)
    val bills = workspace.arrayOrEmpty("recurringRules").objects()
    val goals = workspace.arrayOrEmpty("goals").objects()
    val firstOffset = (monthDate.atDay(1).dayOfWeek.value % 7)
    val days = monthDate.lengthOfMonth()
    val allCells = List(firstOffset) { 0 } + (1..days).toList()
    val weeks = allCells.chunked(7)
    if (picker) MonthYearPicker(monthDate, { onMonthChange(it.toString()); picker = false }, { picker = false })
    PageColumn {
        PageHeading("MONEY ON THE MOVE", "Your month, in color.", "Green marks income, red marks spending, gold marks bills and blue marks savings milestones.")
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) + " ▾", Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { picker = true }.padding(vertical = 4.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Today", Modifier.clip(RoundedCornerShape(50)).clickable { onMonthChange(YearMonth.now().toString()) }.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Text("‹", fontSize = 28.sp, modifier = Modifier.clickable { onMonthChange(monthDate.minusMonths(1).toString()) }.padding(horizontal = 12.dp))
                Text("›", fontSize = 28.sp, modifier = Modifier.clickable { onMonthChange(monthDate.plusMonths(1).toString()) }.padding(horizontal = 12.dp))
            }
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth()) { listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, Modifier.weight(1f), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)) } }
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        if (day == 0) Spacer(Modifier.weight(1f).height(53.dp)) else {
                            val date = monthDate.atDay(day)
                            val dateStr = date.toString()
                            val dayTx = tx.filter { it.string("date") == dateStr }
                            val hasIncome = dayTx.any { it.string("type") == "income" }
                            val hasExpense = dayTx.any { it.string("type") != "income" }
                            val hasBill = bills.any { it.string("nextDate") == dateStr }
                            val hasGoal = goals.any { it.string("deadline") == dateStr }
                            val isSelected = selected == date
                            val tint = when { hasBill -> Gold; hasGoal -> Blue; hasIncome && hasExpense -> Color(0xFF9B77D3); hasIncome -> Green; hasExpense -> Red; else -> MaterialTheme.colorScheme.surfaceVariant }
                            Column(Modifier.weight(1f).height(53.dp).padding(vertical = 2.dp).clip(RoundedCornerShape(7.dp)).background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else tint.copy(alpha = if (hasIncome || hasExpense || hasBill) .28f else .35f)).border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color.Black else MaterialTheme.colorScheme.outline.copy(alpha = .4f), RoundedCornerShape(7.dp)).clickable { selected = date }.padding(5.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(day.toString(), fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (hasIncome) Dot(Green)
                                    if (hasExpense) Dot(Red)
                                    if (hasBill) Dot(Gold)
                                    if (hasGoal) Dot(Blue)
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { LegendDot(Green, "Income"); LegendDot(Red, "Spend"); LegendDot(Gold, "Bill") }
        }
        NeoCard {
            Text(selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            val selectedTx = tx.filter { it.string("date") == selected.toString() }
            val selectedBills = bills.filter { it.string("nextDate") == selected.toString() }
            val selectedGoals = goals.filter { it.string("deadline") == selected.toString() }
            if (selectedTx.isEmpty() && selectedBills.isEmpty() && selectedGoals.isEmpty()) Text("Nothing scheduled for this day.", style = MaterialTheme.typography.bodySmall)
            selectedTx.forEach { t -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (t.string("type") == "income") "↙" else "↗", fontWeight = FontWeight.Black); Spacer(Modifier.width(8.dp)); Text(t.string("title"), Modifier.weight(1f)); Text(money(t.double("amount"), t.string("currency", "INR")), fontWeight = FontWeight.Bold) } }
            selectedBills.forEach { b -> Text("BILL · ${b.string("title")} · ${money(b.double("amount"), b.string("currency", "INR"))}", color = Color(0xFFAE8122), fontWeight = FontWeight.Bold) }
            selectedGoals.forEach { g -> Text("GOAL · ${g.string("name", "Savings milestone")}", color = Blue, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MonthYearPicker(current: YearMonth, onPick: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    var year by remember { mutableStateOf(current.year) }
    var yearGrid by remember { mutableStateOf(false) }
    val decadeStart = remember(year) { year - Math.floorMod(year, 12) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Text("Close", Modifier.clickable { onDismiss() }.padding(12.dp), fontWeight = FontWeight.Black) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontSize = 28.sp, modifier = Modifier.clickable { if (yearGrid) year -= 12 else year-- }.padding(horizontal = 12.dp))
                Text(if (yearGrid) "$decadeStart – ${decadeStart + 11}" else "$year ▾", Modifier.weight(1f).clickable { yearGrid = !yearGrid }, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("›", fontSize = 28.sp, modifier = Modifier.clickable { if (yearGrid) year += 12 else year++ }.padding(horizontal = 12.dp))
            }
        },
        text = {
            val items: List<Pair<String, () -> Unit>> = if (yearGrid) (decadeStart until decadeStart + 12).map { y -> y.toString() to { year = y; yearGrid = false } }
            else (1..12).map { m -> java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH) to { onPick(YearMonth.of(year, m)) } }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, act) ->
                            val on = if (yearGrid) label == current.year.toString() else (label == java.time.Month.of(current.monthValue).getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH) && year == current.year)
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable { act() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontWeight = FontWeight.Bold, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun Dot(color: Color) { Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color)) }
@Composable
private fun LegendDot(color: Color, label: String) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Dot(color); Text(label, style = MaterialTheme.typography.labelSmall) } }

@Composable
fun CurrencyScreen(workspace: JsonObject, applyChange: ((JsonObject) -> Unit) -> Unit, onRefreshRates: () -> Unit, refreshing: Boolean) {
    var amount by remember { mutableStateOf("1") }
    var from by remember { mutableStateOf("INR") }
    var to by remember { mutableStateOf("USD") }
    val settings = workspace.objectOrEmpty("settings")
    val base = settings.string("baseCurrency", "INR")
    val rates = settings.objectOrEmpty("fxRates")
    val fromRate = if (from == "INR") 1.0 else rates.double(from)
    val toRate = if (to == "INR") 1.0 else rates.double(to)
    val converted = if (fromRate > 0 && toRate > 0) (amount.toDoubleOrNull() ?: 0.0) * fromRate / toRate else null
    PageColumn {
        PageHeading("GLOBAL MONEY", "Convert across borders.", "Ten currencies supported. Rates are editable reference assumptions, not live quotes.")
        NeoCard {
            Text("AMOUNT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            PopField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Amount") })
            Selector("From", from, CURRENCY_INFO.map { "${it.flag} ${it.code}" }, { from = it.takeLast(3) })
            OutlinedButton(onClick = { val temp = from; from = to; to = temp }, modifier = Modifier.fillMaxWidth()) { Text("⇅  Swap currencies") }
            Selector("To", to, CURRENCY_INFO.map { "${it.flag} ${it.code}" }, { to = it.takeLast(3) })
            Divider(Modifier.padding(vertical = 7.dp))
            Text("RESULT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            Text(if (converted == null) "Set both rates in Settings" else money(converted, to), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(if (converted == null) "A conversion rate is missing." else "1 $from = ${"%.6f".format(fromRate / toRate)} $to", style = MaterialTheme.typography.bodySmall)
        }
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reference rates · INR for 1 unit of currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(settings.string("fxRateSource", "Manual assumptions") + settings.string("fxUpdatedAt").takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
                NeoOutlineButton(if (refreshing) "Refreshing…" else "Refresh rates", onRefreshRates, Modifier.width(126.dp))
            }
            CURRENCY_INFO.forEach { info ->
                val rate = rates.double(info.code, if (info.code == "INR") 1.0 else 0.0)
                var text by remember(info.code, rate) { mutableStateOf(if (rate > 0) rate.toString() else "") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Text("${info.flag} ${info.code}", fontWeight = FontWeight.Bold); Text(info.country, style = MaterialTheme.typography.labelSmall) }
                    PopField(value = text, onValueChange = { text = it }, modifier = Modifier.width(140.dp), singleLine = true, label = { Text("INR for 1 unit") }, enabled = info.code != "INR")
                    TextButtonTiny("Save") {
                        val v = text.toDoubleOrNull() ?: return@TextButtonTiny
                        applyChange(fun(root: JsonObject) { val s = root.objectOrEmpty("settings"); val fx = s.objectOrEmpty("fxRates"); fx.addProperty(info.code, if (info.code == "INR") 1.0 else v); s.add("fxRates", fx); root.add("settings", s) })
                    }
                }
                Divider(Modifier.padding(vertical = 5.dp))
            }
            Text("Dashboard base currency: $base", style = MaterialTheme.typography.bodySmall)
            Text("Frankfurter publishes reference rates, not live trading or settlement quotes. You can still edit any rate manually.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
        }
    }
}

@Composable
private fun TextButtonTiny(label: String, onClick: () -> Unit) { Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onClick).padding(5.dp)) }

@Composable
fun BudgetsScreen(workspace: JsonObject, month: String, privacy: Boolean, onAdd: () -> Unit, onDelete: (JsonObject) -> Unit, onExportPack: () -> Unit, onImportPack: () -> Unit) {
    PageColumn {
        PageHeading("SPENDING GUARDRAILS", "Give each rupee a lane.", "Set monthly category limits and watch your progress.")
        run {
            val bs = workspace.arrayOrEmpty("budgets").objects()
            val limits = bs.sumOf { it.double("limit").let { l -> if (l == 0.0) it.double("amount") else l } }
            val used = bs.sumOf { budgetUsed(workspace, month, it) }
            val ratio = if (limits > 0) (used / limits) else 0.0
            val cur = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
            NeoCard(fill = Pop.Lavender) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RingGauge(ratio.toFloat(), if (ratio > 1) Pop.Coral else Pop.Ink, if (limits > 0) "${(ratio * 100).toInt()}%" else "—", diameter = 108.dp, caption = "used")
                    Column(Modifier.weight(1f)) {
                        CardLabel("All budgets · $month")
                        Text(if (privacy) "••••" else compactMoney(used, cur), fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp, maxLines = 1)
                        Text(if (privacy) "of ••••" else "of ${compactMoney(limits, cur)} planned", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        NeoButton("+ Create budget", onClick = onAdd)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoOutlineButton("Export shared pack", onExportPack, Modifier.weight(1f))
            NeoOutlineButton("Import shared pack", onImportPack, Modifier.weight(1f))
        }
        Text("Share category limits and transactions explicitly marked as shared. Import merges these into this device; it does not replace your personal workspace.", style = MaterialTheme.typography.bodySmall)
        val budgets = workspace.arrayOrEmpty("budgets").objects()
        if (budgets.isEmpty()) NeoCard { Text("No budgets set yet", fontWeight = FontWeight.Black); Text("Add a category limit to see how you're doing.", style = MaterialTheme.typography.bodySmall) }
        budgets.forEach { b ->
            val budgetCurrency = b.string("currency", "INR")
            val used = monthlyTransactions(workspace, month).filter { it.string("type") != "income" }.sumOf { tx ->
                val txCurrency = tx.string("currency", "INR")
                if (txCurrency != budgetCurrency) 0.0 else {
                    val splits = tx.arrayOrEmpty("splits").objects()
                    if (splits.isNotEmpty()) splits.filter { it.string("category") == b.string("category") }.sumOf { it.double("amount") }
                    else if (tx.string("category") == b.string("category")) tx.double("amount") else 0.0
                }
            }
            val limit = b.double("limit").let { if (it == 0.0) b.double("amount") else it }
            val ratio = if (limit > 0) used / limit else 0.0
            NeoCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(b.string("category", "Budget"), Modifier.weight(1f), fontWeight = FontWeight.Black); Text(if (privacy) "••••" else "${money(used, b.string("currency", "INR"))} / ${money(limit, b.string("currency", "INR"))}", fontWeight = FontWeight.Bold) }
                ProgressBar(ratio.toFloat(), if (ratio > 1) Red else Green, Modifier.padding(vertical = 9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text(if (ratio > 1) "Over budget by ${money(used - limit, b.string("currency", "INR"))}" else "${(ratio * 100).toInt()}% used", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Text("Remove", color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { onDelete(b) }) }
            }
        }
    }
}

@Composable
fun GoalsScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onContribute: (JsonObject) -> Unit, onDelete: (JsonObject) -> Unit, onEditEmergency: () -> Unit) {
    PageColumn {
        PageHeading("FUTURE YOU", "Goals with momentum.", "Savings targets and debt payoff plans, with progress that moves when you do.")
        run {
            val gs = workspace.arrayOrEmpty("goals").objects().filter { it.string("type") != "debt" }
            val target = gs.sumOf { it.double("target") }
            val saved = gs.sumOf { it.double("saved") }
            val ratio = if (target > 0) saved / target else 0.0
            NeoCard(fill = Pop.Mint) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RingGauge(ratio.toFloat(), Pop.Teal, if (target > 0) "${(ratio * 100).toInt().coerceIn(0, 999)}%" else "—", diameter = 108.dp, caption = "saved")
                    Column(Modifier.weight(1f)) {
                        CardLabel("Savings goals")
                        Text("${gs.size} active", fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(if (privacy) "•••• saved so far" else "${compactMoney(saved, "INR")} saved so far", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        NeoButton("+ Add savings or debt goal", onClick = onAdd)
        val goals = workspace.arrayOrEmpty("goals").objects()
        if (goals.isEmpty()) NeoCard { Text("Start with one meaningful goal", fontWeight = FontWeight.Black); Text("Emergency fund, a trip, a course or a debt payoff.", style = MaterialTheme.typography.bodySmall) }
        goals.forEach { goal ->
            val target = goal.double("target"); val saved = goal.double("saved")
            NeoCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(goal.string("name", "Goal"), Modifier.weight(1f), fontWeight = FontWeight.Black); Text(if (goal.string("type") == "debt") "DEBT" else "SAVING", style = MaterialTheme.typography.labelSmall) }
                Text(if (privacy) "•••• / ••••" else "${money(saved, goal.string("currency", "INR"))} / ${money(target, goal.string("currency", "INR"))}", fontWeight = FontWeight.Bold)
                ProgressBar(if (target > 0) (saved / target).toFloat() else 0f, Green, Modifier.padding(vertical = 8.dp))
                Text("${if (target > 0) (saved / target * 100).toInt().coerceIn(0, 100) else 0}% · ${goal.string("deadline", "No deadline")}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NeoButton(if (goal.string("type") == "debt") "Log repayment" else "Add savings", Modifier.weight(1f), { onContribute(goal) }); TextButtonTiny("Remove") { onDelete(goal) } }
            }
        }
        val ef = workspace.objectOrEmpty("emergencyFund")
        val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
        val monthlyEssentials = expenseTotal(workspace, currentMonth())
        val targetBalance = monthlyEssentials * ef.double("targetMonths", 6.0)
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Emergency fund", Modifier.weight(1f), fontWeight = FontWeight.Black); InlineAction("Edit", onEditEmergency) }
            Text("Balance ${if (privacy) "••••" else money(ef.double("balance"), base)} · Target ${ef.double("targetMonths", 6.0).toInt()} months of current-month spending", style = MaterialTheme.typography.bodySmall)
            ProgressBar(if (targetBalance > 0) (ef.double("balance") / targetBalance).toFloat() else 0f, Gold, Modifier.padding(vertical = 7.dp))
            Text(if (targetBalance > 0) "${(ef.double("balance") / targetBalance * 100).toInt().coerceIn(0, 100)}% of estimated target · based on current-month recorded spending" else "Add expenses and set a reserve balance to estimate your target.", style = MaterialTheme.typography.labelSmall)
            Text("A dedicated buffer for essential expenses and unexpected costs.", style = MaterialTheme.typography.labelSmall)
        }
        val debts = goals.filter { it.string("type") == "debt" && it.double("target") > it.double("saved") }
        if (debts.isNotEmpty()) {
            var strategy by remember { mutableStateOf("Avalanche") }
            var extraPayment by remember { mutableStateOf("0") }
            val payoffMonths = estimateDebtMonths(debts, strategy, extraPayment.toDoubleOrNull() ?: 0.0)
            NeoCard {
                Text("Debt payoff optimizer", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text("Compare a smallest-balance-first snowball with a highest-interest-first avalanche.", style = MaterialTheme.typography.bodySmall)
                Selector("Strategy", strategy, listOf("Avalanche", "Snowball"), { strategy = it })
                Field("Extra monthly payment (${base})", extraPayment, { extraPayment = it })
                Text(payoffMonths?.let { "Estimated payoff · ${it / 12} years ${it % 12} months" } ?: "Add minimum monthly payments to your debt goals to estimate a payoff date.", fontWeight = FontWeight.Black)
                Text("Projection compounds each debt's entered annual interest monthly and assumes payments remain constant.", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun estimateDebtMonths(goals: List<JsonObject>, strategy: String, extra: Double): Int? {
    data class Debt(var remaining: Double, val annualRate: Double, val minimum: Double)
    val rows = goals.map { Debt((it.double("target") - it.double("saved")).coerceAtLeast(0.0), it.double("interestRate"), it.double("monthlyContribution")) }
        .filter { it.remaining > 0 }
        .sortedWith(if (strategy == "Snowball") compareBy<Debt> { it.remaining } else compareByDescending<Debt> { it.annualRate })
    if (rows.isEmpty()) return 0
    val totalMinimum = rows.sumOf { it.minimum }
    if (totalMinimum + extra <= 0) return null
    repeat(1200) { month ->
        rows.forEach { debt -> debt.remaining += debt.remaining * (debt.annualRate.coerceAtLeast(0.0) / 1200.0) }
        var pool = totalMinimum + extra
        rows.forEach { debt ->
            val payment = minOf(debt.remaining, debt.minimum, pool)
            debt.remaining -= payment
            pool -= payment
        }
        rows.forEach { debt ->
            if (pool > 0 && debt.remaining > 0) {
                val payment = minOf(debt.remaining, pool)
                debt.remaining -= payment
                pool -= payment
            }
        }
        if (rows.all { it.remaining <= 0.01 }) return month + 1
    }
    return null
}

@Composable
fun NetWorthScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onDelete: (JsonObject) -> Unit) {
    val items = workspace.arrayOrEmpty("netWorthItems").objects()
    val assets = items.filter { it.string("type", "Asset").equals("Asset", true) }.sumOf { toBase(workspace, it.double("value", it.double("amount")), it.string("currency", "INR")) }
    val liabilities = items.filter { it.string("type").equals("Liability", true) }.sumOf { toBase(workspace, it.double("value", it.double("amount")), it.string("currency", "INR")) }
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    PageColumn {
        PageHeading("THE WHOLE PICTURE", "Know what you own.", "Track cash, investments, gold, property, loans and other liabilities.")
        if (assets + liabilities > 0) NeoCard {
            CardLabel("Assets vs liabilities")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutChart(listOf(Pop.Teal to assets, Pop.Coral to liabilities), centerTop = if (privacy) "••••" else compactMoney(assets - liabilities, base), centerBottom = "net worth", diameter = 132.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendRow(Pop.Teal, "Assets", if (privacy) "••" else compactMoney(assets, base))
                    LegendRow(Pop.Coral, "Liabilities", if (privacy) "••" else compactMoney(liabilities, base))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { MetricTile("Assets", if (privacy) "••••" else money(assets, base)) }; Box(Modifier.weight(1f)) { MetricTile("Liabilities", if (privacy) "••••" else money(liabilities, base)) } }
        MetricTile("Net worth", if (privacy) "••••" else money(assets - liabilities, base), "Assets minus liabilities")
        NeoButton("+ Add asset or liability", onClick = onAdd)
        items.forEach { item -> NeoCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.string("name", "Item"), fontWeight = FontWeight.Black); Text(item.string("type", "Asset"), style = MaterialTheme.typography.bodySmall) }; Text(if (privacy) "••••" else money(item.double("value", item.double("amount")), item.string("currency", "INR")), fontWeight = FontWeight.Black); Text("×", modifier = Modifier.clickable { onDelete(item) }.padding(8.dp)) } } }
    }
}

@Composable
fun SimulatorScreen(workspace: JsonObject) {
    var monthlyIncome by remember { mutableStateOf(incomeTotal(workspace).toString()) }
    var expenses by remember { mutableStateOf(expenseTotal(workspace).toString()) }
    var savings by remember { mutableStateOf("5000") }
    var years by remember { mutableStateOf("3") }
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val monthly = (savings.toDoubleOrNull() ?: 0.0)
    val months = ((years.toIntOrNull() ?: 1).coerceIn(1, 50)) * 12
    val total = monthly * months
    val affordability = (monthlyIncome.toDoubleOrNull() ?: 0.0) - (expenses.toDoubleOrNull() ?: 0.0) - monthly
    PageColumn {
        PageHeading("PLAY WITH THE POSSIBILITIES", "What if you changed one thing?", "Explore savings, spending and purchase scenarios. These are estimates, not guarantees.")
        NeoCard(fill = Pop.Sun) {
            CardLabel("Projected savings growth")
            LineChart(
                labels = (1..6).map { i -> String.format(Locale.ENGLISH, "%.1fy", months * i / 6.0 / 12.0) },
                series = listOf(ChartSeries("Saved", Pop.Ink, (1..6).map { i -> monthly * months * i / 6.0 })),
                currency = base
            )
        }
        NeoCard {
            Field("Monthly income", monthlyIncome, { monthlyIncome = it })
            Field("Monthly expenses", expenses, { expenses = it })
            Field("Extra monthly savings", savings, { savings = it })
            Field("Years", years, { years = it })
        }
        MetricTile("Projected contributions", money(total, base), "$months months · before investment returns")
        MetricTile("Monthly buffer after savings", money(affordability, base), if (affordability >= 0) "Scenario fits these assumptions" else "This savings target exceeds your estimated buffer")
    }
}

private val GainDark = Color(0xFF0B6B3A)
private val LossDark = Color(0xFFB3202A)

private fun kindFill(kind: String): Color = when (kind) {
    "SIP" -> Pop.Lavender
    "Mutual fund" -> Pop.Peach
    "EPF" -> Pop.Mint
    "PPF" -> Color(0xFFB8E3F2)
    "NPS" -> Color(0xFFFFE08A)
    "FD" -> Color(0xFFF9C6D0)
    "RD" -> Color(0xFFD9F0A3)
    "Gold" -> Pop.Sun
    else -> Color(0xFFE4DFF5)
}

/** Dated cash flows (negative = money in) for one product, before the closing value is added. */
private fun productFlows(p: JsonObject, invested: Double): MutableList<Pair<LocalDate, Double>> {
    val flows = p.arrayOrEmpty("contributions").objects().mapNotNull { c ->
        val date = runCatching { LocalDate.parse(c.string("date")) }.getOrNull() ?: return@mapNotNull null
        date to -kotlin.math.abs(c.double("amount"))
    }.toMutableList()
    if (flows.isEmpty() && invested > 0) {
        val start = runCatching { LocalDate.parse(p.string("startDate")) }.getOrNull() ?: LocalDate.now()
        flows.add(start to -invested)
    }
    return flows
}

@Composable
private fun EmptyPortfolioArt(modifier: Modifier = Modifier) {
    val bars = listOf(Pop.Lavender to 0.38f, Pop.Peach to 0.62f, Pop.Sun to 0.85f)
    Row(modifier.height(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEach { (color, frac) ->
            Box(Modifier.size(width = 34.dp, height = 110.dp * frac).clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 6.dp, bottomEnd = 6.dp)).background(color))
        }
        Box(Modifier.size(34.dp).clip(CircleShape).background(Pop.Coral))
    }
}

@Composable
fun IndiaFinanceScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onDelete: (JsonObject) -> Unit, onContribute: (JsonObject) -> Unit) {
    val products = workspace.arrayOrEmpty("financeProducts").objects()
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val investedTotal = products.sumOf { toBase(workspace, it.double("invested"), it.string("currency", "INR")) }
    val currentTotal = products.sumOf { toBase(workspace, it.double("currentValue", it.double("value")), it.string("currency", "INR")) }
    val gainTotal = currentTotal - investedTotal
    val returnPct = if (investedTotal > 0) gainTotal / investedTotal * 100.0 else null
    // Portfolio-level XIRR: every dated contribution (converted to base) plus today's total value.
    val portfolioFlows = products.flatMap { p ->
        val cur = p.string("currency", "INR")
        productFlows(p, p.double("invested", p.double("amount"))).map { (d, amt) -> d to toBase(workspace, amt, cur) }
    }.toMutableList().also { it.add(LocalDate.now() to currentTotal) }
    val portfolioXirr = estimateXirr(portfolioFlows)
    var selectedKind by remember { mutableStateOf("All") }
    val standardKinds = listOf("SIP", "Mutual fund", "EPF", "PPF", "NPS", "FD", "RD", "Gold")
    val kinds = standardKinds + products.map { it.string("kind", it.string("type", "Product")) }.filter { it !in standardKinds }.distinct()
    val shown = if (selectedKind == "All") products else products.filter { it.string("kind", it.string("type", "Product")) == selectedKind }
    val gainFg = if (gainTotal >= 0) GainDark else LossDark

    PageColumn {
        NeoTitle("INDIA / WEALTH TRACKER", "Your long-term money, under control.", "SIPs, mutual funds, EPF, PPF, NPS, deposits and gold in one place. Values are entered by you.")

        NeoCard(fill = Pop.Lavender) {
            Text("PORTFOLIO VALUE", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .7f))
            Spacer(Modifier.height(6.dp))
            val valueText = if (privacy) "••••••" else money(currentTotal, base)
            val numSize = when { valueText.length <= 9 -> 46.sp; valueText.length <= 13 -> 38.sp; else -> 30.sp }
            Text(valueText, fontSize = numSize, lineHeight = numSize, fontWeight = FontWeight.Black, letterSpacing = (-1).sp, color = Pop.Ink, maxLines = 1, softWrap = false)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (privacy) "••••" else (if (gainTotal >= 0) "▲ " else "▼ ") + money(kotlin.math.abs(gainTotal), base),
                    color = gainFg, fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .7f)).padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text("absolute gain", style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .75f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { MetricTile("Total returns", if (privacy) "••%" else returnPct?.let { "%+.2f%%".format(it) } ?: "—", "On invested amount", fill = if (gainTotal >= 0) Pop.Mint else Color(0xFFF9C6D0)) }
            Box(Modifier.weight(1f)) { MetricTile("XIRR", if (privacy) "••%" else portfolioXirr?.let { "%.2f%%".format(it) } ?: "—", "Annualised, all products", fill = Pop.Sun) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { MetricTile("Invested", if (privacy) "••••" else money(investedTotal, base), "Total contributed") }
            Box(Modifier.weight(1f)) { MetricTile("Tracked", products.size.toString(), if (products.size == 1) "product" else "products", fill = Pop.Peach) }
        }
        NeoButton("+  Add investment", modifier = Modifier.fillMaxWidth(), onClick = onAdd)
        if (products.isNotEmpty() && currentTotal > 0) BreakdownCard("Allocation by product", products.groupBy { it.string("kind", it.string("type", "Product")) }.map { (k, v) -> k to v.sumOf { p -> toBase(workspace, p.double("currentValue", p.double("value")), p.string("currency", "INR")) } }.sortedByDescending { it.second }, privacy, base, "value", "")

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PopChip("All", selectedKind == "All", { selectedKind = "All" })
            kinds.forEach { kind -> PopChip(kind, selectedKind == kind, { selectedKind = kind }) }
        }

        if (products.isEmpty()) {
            NeoCard {
                EmptyPortfolioArt()
                Spacer(Modifier.height(16.dp))
                Text("NO ASSETS TRACKED.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Your portfolio starts here.", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Add your first investment to begin tracking your financial journey. FinCip does not connect to providers; you enter balances from your statements.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
            }
        } else if (shown.isEmpty()) {
            Text("Nothing under $selectedKind yet. Tap Add investment and pick $selectedKind as the product type.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
        }

        shown.forEach { p ->
            val invested = p.double("invested", p.double("amount"))
            val current = p.double("currentValue", p.double("value"))
            val gain = current - invested
            val currency = p.string("currency", "INR")
            val kind = p.string("kind", p.string("type", "Product"))
            val flows = productFlows(p, invested)
            flows.add(LocalDate.now() to current)
            val irr = estimateXirr(flows)
            val years = runCatching { ChronoUnit.DAYS.between(LocalDate.parse(p.string("startDate")), LocalDate.now()).toDouble() / 365.25 }.getOrNull() ?: 0.0
            val cagr = if (invested > 0 && current >= 0 && years > 0.01) ((current / invested).pow(1.0 / years) - 1.0) * 100.0 else null
            NeoCard(fill = kindFill(kind)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(kind.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .7f))
                        Text(p.string("name", "Investment"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Pop.Ink)
                        Text(p.string("provider", "Provider not specified"), style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .75f))
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = .55f)).clickable { onDelete(p) }, contentAlignment = Alignment.Center) {
                        Text("×", fontWeight = FontWeight.Black, color = Pop.Ink, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(if (privacy) "••••" else money(current, currency), fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp, color = Pop.Ink, maxLines = 1)
                Text(
                    "Invested ${if (privacy) "••••" else money(invested, currency)}  ·  ${if (gain >= 0) "▲" else "▼"} ${if (privacy) "••••" else money(kotlin.math.abs(gain), currency)}",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (gain >= 0) GainDark else LossDark
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallPill("XIRR ${irr?.let { "%.2f%%".format(it) } ?: "—"}")
                    SmallPill("CAGR ${cagr?.let { "%.2f%%".format(it) } ?: "—"}")
                    if (p.double("monthlyContribution") > 0) SmallPill("Monthly ${money(p.double("monthlyContribution"), currency)}")
                }
                if (p.string("maturityDate").isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("MATURITY  ${p.string("maturityDate")}", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = Pop.Ink.copy(alpha = .8f))
                }
                Spacer(Modifier.height(12.dp))
                NeoOutlineButton("+ Add contribution", { onContribute(p) }, Modifier.fillMaxWidth())
            }
        }
        Text("XIRR is an annualized estimate from dated contributions and current value. CAGR assumes one initial investment; for recurring SIPs, XIRR is usually more informative. These calculations depend on the dates and values you enter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
    }
}

private fun estimateXirr(flows: List<Pair<LocalDate, Double>>): Double? {
    if (flows.size < 2 || flows.none { it.second < 0 } || flows.none { it.second > 0 }) return null
    val start = flows.minOf { it.first }
    fun npv(rate: Double): Double = flows.sumOf { (date, amount) ->
        val years = ChronoUnit.DAYS.between(start, date).toDouble() / 365.25
        amount / (1.0 + rate).pow(years)
    }
    var low = -0.9999
    var high = 1.0
    var fLow = npv(low)
    var fHigh = npv(high)
    while (fLow * fHigh > 0 && high < 1024.0) { high = high * 2.0 + 0.5; fHigh = npv(high) }
    if (!fLow.isFinite() || !fHigh.isFinite() || fLow * fHigh > 0) return null
    repeat(180) {
        val mid = (low + high) / 2.0
        val fMid = npv(mid)
        if (!fMid.isFinite()) return null
        if (kotlin.math.abs(fMid) < 0.000001) return mid * 100.0
        if (fLow * fMid <= 0) { high = mid; fHigh = fMid } else { low = mid; fLow = fMid }
    }
    return ((low + high) / 2.0 * 100.0).takeIf { it.isFinite() }
}

@Composable
private fun SmallPill(text: String) { Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .7f), contentColor = Pop.Ink) { Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1) } }

@Composable
fun TaxScreen(
    country: String, onCountry: (String) -> Unit,
    taxYear: String, onTaxYear: (String) -> Unit,
    income: String, onIncome: (String) -> Unit,
    deductions: String, onDeductions: (String) -> Unit,
    standardDeduction: String, onStandardDeduction: (String) -> Unit,
    allowance: String, onAllowance: (String) -> Unit,
    rate: String, onRate: (String) -> Unit,
    regime: String, onRegime: (String) -> Unit,
    ageGroup: String, onAgeGroup: (String) -> Unit,
    resident: Boolean, onResident: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    val countries = listOf(
        "IN" to "India · INR", "US" to "United States · USD", "GB" to "United Kingdom · GBP",
        "DE" to "Germany · EUR", "JP" to "Japan · JPY", "CN" to "China · CNY",
        "CH" to "Switzerland · CHF", "CA" to "Canada · CAD", "AU" to "Australia · AUD", "SG" to "Singapore · SGD"
    )
    val selectedCountry = countries.firstOrNull { it.first == country } ?: countries.first()
    val currency = when (country) {
        "US" -> "USD"; "GB" -> "GBP"; "DE" -> "EUR"; "JP" -> "JPY"; "CN" -> "CNY"
        "CH" -> "CHF"; "CA" -> "CAD"; "AU" -> "AUD"; "SG" -> "SGD"; else -> "INR"
    }
    val india = country == "IN"
    val gross = income.toDoubleOrNull() ?: 0.0
    val taxable = if (india) {
        max(0.0, gross - (standardDeduction.toDoubleOrNull() ?: 0.0) - if (regime == "old") (deductions.toDoubleOrNull() ?: 0.0) else 0.0)
    } else {
        max(0.0, gross - (allowance.toDoubleOrNull() ?: 0.0) - (deductions.toDoubleOrNull() ?: 0.0))
    }
    val indiaEstimate = if (india) indiaEstimate(taxable, regime, ageGroup, resident) else null
    val globalRate = (rate.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val globalTax = taxable * globalRate / 100.0
    val estimated = indiaEstimate?.total ?: globalTax
    PageColumn {
        PageHeading("GLOBAL TAX PLANNING", "A clearer tax picture.", "Choose a jurisdiction and build an annual estimate in local currency. India uses a simplified slab model; other countries use your own effective-rate assumptions.")
        NeoCard {
            Selector(
                label = "Tax jurisdiction",
                value = selectedCountry.second,
                options = countries.map { it.second },
                onSelect = { label ->
                onCountry(countries.first { it.second == label }.first)
                  }
           )
            Field("Tax year / period", taxYear, onTaxYear, hint = "Use the tax year or fiscal period that applies to you.")
            Field("Annual gross income ($currency)", income, onIncome)
            if (india) {
                Selector("Tax regime", regime, listOf("new", "old"), onRegime)
                Field("Standard deduction ($currency)", standardDeduction, onStandardDeduction)
                if (regime == "old") Field("Other eligible deductions / exemptions ($currency)", deductions, onDeductions)
                Selector("Age category", ageGroup, listOf("below60", "senior60", "super80"), onAgeGroup)
                FilterChip(selected = resident, onClick = { onResident(!resident) }, label = { Text(if (resident) "Resident individual · rebate eligible" else "Non-resident · no resident rebate") })
            } else {
                Field("Personal allowance / tax-free threshold ($currency)", allowance, onAllowance)
                Field("Other deductible amount ($currency)", deductions, onDeductions)
                Field("Estimated effective income-tax rate (%)", rate, onRate, hint = "Enter your own estimated effective rate, based on local tax guidance.")
            }
            Text("Taxable planning income: ${money(taxable, currency)}", fontWeight = FontWeight.Bold)
            NeoButton("Save tax assumptions", onClick = onSave)
        }
        NeoCard {
            Text("ESTIMATED ANNUAL TAX · $taxYear", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            Text(money(estimated, currency), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Divider(Modifier.padding(vertical = 7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Taxable income", Modifier.weight(1f)); Text(money(taxable, currency), fontWeight = FontWeight.Bold) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text(if (india) "Tax before rebate" else "Entered effective rate"); Spacer(Modifier.weight(1f)); Text(if (india) money(indiaEstimate?.beforeRebate ?: 0.0, currency) else "${"%.1f".format(globalRate)}%", fontWeight = FontWeight.Bold) }
            if (india) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Estimated rebate"); Spacer(Modifier.weight(1f)); Text("−${money(indiaEstimate?.rebate ?: 0.0, currency)}", color = Green, fontWeight = FontWeight.Bold) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Health & education cess"); Spacer(Modifier.weight(1f)); Text(money(indiaEstimate?.cess ?: 0.0, currency), fontWeight = FontWeight.Bold) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Estimated take-home"); Spacer(Modifier.weight(1f)); Text(money(max(0.0, gross - globalTax), currency), color = Green, fontWeight = FontWeight.Bold) }
            }
        }
        Text(
            if (india) "India estimate is simplified: it does not model surcharge, special-rate income, every deduction, TDS, advance tax or filing details. Verify current slabs and eligibility before filing."
            else "This is a user-assumption estimate, not a country-specific statutory calculator. It does not automatically model progressive brackets, regional taxes, social insurance, tax credits, residency tests or capital gains. Confirm rules for ${selectedCountry.second} and $taxYear with the official tax authority.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)
        )
    }
}

private data class IndiaTaxEstimate(val beforeRebate: Double, val rebate: Double, val cess: Double, val total: Double)

private fun indiaEstimate(taxable: Double, regime: String, ageGroup: String, resident: Boolean): IndiaTaxEstimate {
    val slabs = if (regime == "new") listOf(400000.0 to 0.0, 800000.0 to .05, 1200000.0 to .10, 1600000.0 to .15, 2000000.0 to .20, 2400000.0 to .25, Double.POSITIVE_INFINITY to .30)
    else when (ageGroup) {
        "super80" -> listOf(500000.0 to 0.0, 1000000.0 to .20, Double.POSITIVE_INFINITY to .30)
        "senior60" -> listOf(300000.0 to 0.0, 500000.0 to .05, 1000000.0 to .20, Double.POSITIVE_INFINITY to .30)
        else -> listOf(250000.0 to 0.0, 500000.0 to .05, 1000000.0 to .20, Double.POSITIVE_INFINITY to .30)
    }
    var previous = 0.0
    var tax = 0.0
    slabs.forEach { (upper, slabRate) ->
        val slice = (minOf(taxable, upper) - previous).coerceAtLeast(0.0)
        tax += slice * slabRate
        previous = upper
    }
    val rebate = if (resident && regime == "new") {
        if (taxable <= 1200000.0) minOf(60000.0, tax) else minOf(tax, max(0.0, tax - (taxable - 1200000.0)))
    } else if (resident && regime == "old" && taxable <= 500000.0) minOf(12500.0, tax) else 0.0
    val afterRebate = max(0.0, tax - rebate)
    val cess = afterRebate * .04
    return IndiaTaxEstimate(tax, rebate, cess, afterRebate + cess)
}

@Composable
fun PortfolioScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onDelete: (JsonObject) -> Unit) {
    val holdings = workspace.arrayOrEmpty("investments").objects()
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val invested = holdings.sumOf { toBase(workspace, it.double("units", 1.0) * it.double("buyPrice"), it.string("currency", base)) }
    val value = holdings.sumOf { toBase(workspace, it.double("units", 1.0) * it.double("currentPrice"), it.string("currency", base)) }
    PageColumn {
        PageHeading("YOUR ASSET MIX", "Track the long game.", "Stocks, mutual funds, ETFs and crypto. Prices are entered manually.")
        if (holdings.isNotEmpty() && value > 0) BreakdownCard("Allocation by asset type", holdings.groupBy { it.string("type", "Asset") }.map { (k, v) -> k to v.sumOf { h -> toBase(workspace, h.double("units", 1.0) * h.double("currentPrice"), h.string("currency", base)) } }.sortedByDescending { it.second }, privacy, base, "total", "")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { MetricTile("Invested", if (privacy) "••••" else money(invested, base)) }; Box(Modifier.weight(1f)) { MetricTile("Current value", if (privacy) "••••" else money(value, base)) } }
        MetricTile("Unrealized change", if (privacy) "••••" else money(value - invested, base), "Based on your last entered prices")
        NeoButton("+ Add holding", onClick = onAdd)
        holdings.forEach { h ->
            val units = h.double("units", 1.0); val cost = units * h.double("buyPrice"); val current = units * h.double("currentPrice")
            NeoCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(h.string("asset", h.string("title", "Holding")), fontWeight = FontWeight.Black); Text("${h.string("type", "Asset")} · ${units} units", style = MaterialTheme.typography.bodySmall) }; Text(if (privacy) "••••" else money(current, h.string("currency", base)), fontWeight = FontWeight.Black); Text("×", modifier = Modifier.clickable { onDelete(h) }.padding(8.dp)) }; Text("P/L ${money(current - cost, h.string("currency", base))}", color = if (current >= cost) Green else Red, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ForecastScreen(workspace: JsonObject, privacy: Boolean) {
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val balance = incomeTotal(workspace) - expenseTotal(workspace)
    val rules = workspace.arrayOrEmpty("recurringRules").objects()
    val recurringIncome = rules.filter { it.string("type") == "income" }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", base)) }
    val recurringExpenses = rules.filter { it.string("type") != "income" }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", base)) }
    val next = balance + recurringIncome - recurringExpenses
    PageColumn {
        PageHeading("LOOKING AHEAD", "Your next-month outlook.", "A simple projection from your recorded balance and recurring rules. One-time spending is not predicted.")
        NeoCard(fill = Pop.Lavender) {
            CardLabel("Balance bridge")
            MiniBars(
                values = if (privacy) listOf(0.0, 0.0, 0.0, 0.0) else listOf(kotlin.math.abs(balance), recurringIncome, recurringExpenses, kotlin.math.abs(next)),
                labels = listOf("Now", "+ In", "− Out", "Next"), highlight = 3, accent = Pop.Ink, chartHeight = 110.dp
            )
        }
        MetricTile("Current recorded net", if (privacy) "••••" else money(balance, base))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { MetricTile("Recurring income", if (privacy) "••••" else money(recurringIncome, base)) }; Box(Modifier.weight(1f)) { MetricTile("Recurring costs", if (privacy) "••••" else money(recurringExpenses, base)) } }
        NeoCard { Text("Projected balance", style = MaterialTheme.typography.labelMedium); Text(if (privacy) "••••" else money(next, base), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("Assumes each saved rule occurs once next month.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun InsightsScreen(workspace: JsonObject, month: String, privacy: Boolean) {
    val tx = monthlyTransactions(workspace, month).filter { it.string("type") != "income" }
    val groups = tx.groupBy { it.string("category", "Other") }.mapValues { (_, v) -> v.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) } }.toList().sortedByDescending { it.second }
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    PageColumn {
        PageHeading("PATTERNS, NOT JUDGMENT", "Understand your spending.", "See category patterns for the selected month and spot the largest areas.")
        BreakdownCard("Category split · $month", groups, privacy, base, "spent", "No spending data for $month yet.")
        WeeklySpendCard(workspace, month, privacy)
        MetricTile("Total spending", if (privacy) "••••" else money(expenseTotal(workspace, month), base), month)
        if (groups.isEmpty()) NeoCard { Text("No spending data yet", fontWeight = FontWeight.Black); Text("Transactions will create spending insights.", style = MaterialTheme.typography.bodySmall) }
        groups.forEachIndexed { index, pair ->
            val total = expenseTotal(workspace, month)
            NeoCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}. ${pair.first}", Modifier.weight(1f), fontWeight = FontWeight.Black); Text(if (privacy) "••••" else money(pair.second, base), fontWeight = FontWeight.Bold) }
                ProgressBar(if (total > 0) (pair.second / total).toFloat() else 0f, listOf(Green, Gold, Blue, Red)[index % 4], Modifier.padding(vertical = 7.dp))
            }
        }
    }
}

@Composable
fun RulesScreen(workspace: JsonObject, onAdd: () -> Unit, onApply: () -> Unit, onDelete: (JsonObject) -> Unit) {
    PageColumn {
        PageHeading("AUTOMATE THE BORING BITS", "Smart categorization rules.", "Rules match words in transaction titles and update their categories on your request.")
        run {
            val rs = workspace.arrayOrEmpty("smartRules").objects()
            val hits = transactions(workspace).count { t -> rs.any { r -> r.string("keyword").isNotBlank() && t.string("title").contains(r.string("keyword"), true) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { MetricTile("Rules", rs.size.toString(), "active", fill = Pop.Lavender) }
                Box(Modifier.weight(1f)) { MetricTile("Matches", hits.toString(), "transactions", fill = Pop.Peach) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NeoButton("+ Add rule", Modifier.weight(1f), onAdd); NeoOutlineButton("Apply rules", onApply, Modifier.weight(1f)) }
        workspace.arrayOrEmpty("smartRules").objects().forEach { rule -> NeoCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Contains: ${rule.string("keyword")}", fontWeight = FontWeight.Black); Text("Set category → ${rule.string("category")}", style = MaterialTheme.typography.bodySmall) }; Text("×", modifier = Modifier.clickable { onDelete(rule) }.padding(8.dp)) } } }
    }
}

@Composable
fun ReceiptScreen(workspace: JsonObject, onAdd: () -> Unit, onOpen: (JsonObject) -> Unit, onDelete: (JsonObject) -> Unit) {
    PageColumn {
        PageHeading("KEEP THE PAPER TRAIL", "Receipts, safely stored.", "Choose a photo, image or PDF. Files are copied into this app's private storage.")
        MetricTile("Receipts stored", workspace.arrayOrEmpty("receipts").size().toString(), "Private to this device", fill = Pop.Mint)
        NeoButton("+ Add receipt / invoice", onClick = onAdd)
        val receipts = workspace.arrayOrEmpty("receipts").objects()
        if (receipts.isEmpty()) NeoCard { Text("Your receipt vault is empty", fontWeight = FontWeight.Black); Text("Add a receipt to keep proof of purchase close to your records.", style = MaterialTheme.typography.bodySmall) }
        receipts.forEach { r -> NeoCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(r.string("title", "Receipt"), fontWeight = FontWeight.Black); Text(r.string("fileName", r.string("createdAt", "Saved locally")), style = MaterialTheme.typography.bodySmall); Text(r.string("note"), style = MaterialTheme.typography.labelSmall) }; Text("Open", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpen(r) }.padding(6.dp)); Text("Remove", color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { onDelete(r) }.padding(6.dp)) } } }
    }
}

@Composable
fun SecurityScreen(workspace: JsonObject, onPrivacy: (Boolean) -> Unit, onImport: () -> Unit, onBackup: () -> Unit, onPlainBackup: () -> Unit) {
    val privacy = workspace.objectOrEmpty("settings").bool("privacyMode")
    PageColumn {
        PageHeading("YOUR DATA, YOUR KEYS", "Private by design.", "Your local workspace is encrypted with an Android Keystore key. Exported backups are password encrypted.")
        NeoCard(fill = Pop.Teal) {
            CardLabel("Encryption")
            Text("AES-256-GCM", fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(6.dp))
            Text("Local workspace sealed with an Android Keystore key.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SmallPill("On-device"); SmallPill(if (privacy) "Privacy ON" else "Privacy OFF") }
        }
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Privacy mode", fontWeight = FontWeight.Black); Text("Mask financial amounts on screen.", style = MaterialTheme.typography.bodySmall) }; FilterChip(selected = privacy, onClick = { onPrivacy(!privacy) }, label = { Text(if (privacy) "ON" else "OFF") }) }
        }
        NeoCard {
            Text("Encrypted backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Create a portable AES-256-GCM backup. The format is compatible with FinCip's browser encrypted backup format.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp)); NeoButton("Create encrypted backup", onClick = onBackup)
            Spacer(Modifier.height(6.dp)); NeoOutlineButton("Export portable JSON", onPlainBackup)
            Text("JSON is not encrypted. Use it only in a trusted private location. For receipts and sensitive backup portability, use the encrypted export.", style = MaterialTheme.typography.labelSmall)
        }
        NeoCard {
            Text("Restore / import", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Import a plain JSON export or password-encrypted FinCip backup from your old web version.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp)); NeoOutlineButton("Choose backup file", onImport)
        }
        Text("Keep your backup password safe. There is no password recovery path. Never store bank passwords, OTPs, card PINs or CVVs in FinCip.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
    }
}

@Composable
fun SettingsScreen(workspace: JsonObject, vibe: String, onVibe: (String) -> Unit, dark: Boolean, onDark: (Boolean) -> Unit, onBaseCurrency: (String) -> Unit, onCsv: () -> Unit, onPdf: () -> Unit, dashboardOrder: List<String>, onDashboardOrder: (List<String>) -> Unit, onPrivacyDefault: (Boolean) -> Unit = {}, sound: Boolean = true, onSound: (Boolean) -> Unit = {}, haptic: Boolean = true, onHaptic: (Boolean) -> Unit = {}, section: String = "all") {
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val dashboardLabels = mapOf("balance" to "Total balance", "income" to "Monthly income", "expenses" to "Monthly spending", "savings" to "Savings rate")
    val visibleDashboardOrder = (dashboardOrder.filter { it in dashboardLabels.keys }.distinct() + dashboardLabels.keys.filterNot { it in dashboardOrder }).take(4)
    PageColumn {
        PageHeading("MAKE THE APP FEEL LIKE YOURS", "Set your FinCip vibe.", "Theme style, dashboard layout, currency, sound and privacy.")
        if (section == "all" || section == "dashboard") {
            NeoCard {
                Text("Dashboard layout", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text("Choose the order of the four summary cards on Home.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                visibleDashboardOrder.forEachIndexed { index, key ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", fontWeight = FontWeight.Black, modifier = Modifier.width(28.dp))
                        Text(dashboardLabels[key] ?: key, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("↑", modifier = Modifier.clickable(enabled = index > 0) {
                            val next = visibleDashboardOrder.toMutableList(); val value = next[index - 1]; next[index - 1] = next[index]; next[index] = value; onDashboardOrder(next)
                        }.padding(horizontal = 10.dp, vertical = 7.dp), color = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .25f), fontWeight = FontWeight.Black)
                        Text("↓", modifier = Modifier.clickable(enabled = index < visibleDashboardOrder.lastIndex) {
                            val next = visibleDashboardOrder.toMutableList(); val value = next[index + 1]; next[index + 1] = next[index]; next[index] = value; onDashboardOrder(next)
                        }.padding(horizontal = 10.dp, vertical = 7.dp), color = if (index < visibleDashboardOrder.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .25f), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        if (section == "all" || section == "currency") {
            NeoCard {
                Text("Currency & locale", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Selector("Dashboard currency", base, CURRENCY_INFO.map { it.code }, onBaseCurrency)
                Text("Add missing conversion assumptions in Money → Currency converter.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (section == "all" || section == "sound") {
            NeoCard {
                Text("Sound & touch", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Tap sounds", fontWeight = FontWeight.Bold); Text("Soft click when you press buttons and tabs.", style = MaterialTheme.typography.bodySmall) }; androidx.compose.material3.Switch(sound, onSound) }
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Haptic feedback", fontWeight = FontWeight.Bold); Text("A light vibration on taps.", style = MaterialTheme.typography.bodySmall) }; androidx.compose.material3.Switch(haptic, onHaptic) }
                Text("Tap sounds also follow your phone's Touch sounds setting.", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (section == "all" || section == "privacy") {
            NeoCard {
                Text("Privacy defaults", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                val privacyOn = workspace.objectOrEmpty("settings").bool("privacyMode")
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Hide amounts", fontWeight = FontWeight.Bold); Text("Mask balances and totals on every screen.", style = MaterialTheme.typography.bodySmall) }; androidx.compose.material3.Switch(privacyOn, onPrivacyDefault) }
            }
        }
        if (section == "all" || section == "data") {
            NeoCard {
                Text("Portable reports", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NeoOutlineButton("Export CSV", onCsv, Modifier.weight(1f)); NeoButton("Save PDF", onClick = onPdf, modifier = Modifier.weight(1f)) }
            }
        }
        Text("Clarity over complexity · FinCip Native", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun HouseholdScreen(
    url: String, onUrl: (String) -> Unit, anon: String, onAnon: (String) -> Unit, email: String, onEmail: (String) -> Unit, password: String, onPassword: (String) -> Unit,
    session: JsonObject, sharedBudgets: String, category: String, onCategory: (String) -> Unit, limit: String, onLimit: (String) -> Unit, currency: String, onCurrency: (String) -> Unit,
    householdName: String, onHouseholdName: (String) -> Unit, inviteCode: String, onInviteCode: (String) -> Unit, busy: Boolean,
    onSignIn: () -> Unit, onPush: () -> Unit, onPull: () -> Unit, onCreateHousehold: () -> Unit, onJoinHousehold: () -> Unit, onRefreshShared: () -> Unit, onAddSharedBudget: () -> Unit, onSignOut: () -> Unit
) {
    PageColumn {
        PageHeading("OPTIONAL PRIVATE CLOUD", "Your household, together.", "Connect your own Supabase project for account sync and shared budget limits.")
        NeoCard {
            Text("Supabase configuration", fontWeight = FontWeight.Black)
            Field("Project URL", url, onUrl, hint = "https://your-project.supabase.co")
            Field("Public anon key", anon, onAnon)
            Field("Email", email, onEmail)
            Field("Password", password, onPassword)
            NeoButton(if (busy) "Working…" else if (session.string("accessToken").isBlank()) "Sign in" else "Connected as ${session.string("userId").take(8)}…", onClick = onSignIn, enabled = !busy)
        }
        if (session.string("accessToken").isNotBlank()) {
            NeoCard {
                Text("Workspace sync", fontWeight = FontWeight.Black)
                Text("Sync sends the full JSON workspace to your Supabase account. It is not end-to-end encrypted. Push replaces the current cloud snapshot; pull replaces this device's workspace.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NeoOutlineButton("Push", onPush, Modifier.weight(1f)); NeoButton("Pull", onClick = onPull, modifier = Modifier.weight(1f)) }
                Text("Sign out", modifier = Modifier.clickable(onClick = onSignOut).padding(6.dp), color = MaterialTheme.colorScheme.error)
            }
            NeoCard {
                Text("Household", fontWeight = FontWeight.Black)
                Text("Current household ID: ${session.string("householdId").ifBlank { "Not joined" }}", style = MaterialTheme.typography.bodySmall)
                Field("New household name", householdName, onHouseholdName)
                NeoButton("Create household", onClick = onCreateHousehold)
                Field("Invite code", inviteCode, onInviteCode)
                NeoOutlineButton("Join household", onJoinHousehold)
            }
            if (session.string("householdId").isNotBlank()) {
                NeoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Shared budgets", Modifier.weight(1f), fontWeight = FontWeight.Black); Text("Refresh", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onRefreshShared)) }
                    Field("Category", category, onCategory)
                    Field("Monthly limit", limit, onLimit)
                    Selector("Currency", currency, CURRENCY_INFO.map { it.code }, onCurrency)
                    NeoButton("Add shared budget", onClick = onAddSharedBudget)
                    Text(sharedBudgets.take(1200), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Text("Run the Supabase SQL file included in this Android project before connecting. This native client uses Auth and row-level-security policies from that schema.", style = MaterialTheme.typography.bodySmall)
        }
    }
}


fun profileOf(workspace: JsonObject): ProfileData {
    val p = workspace.objectOrEmpty("profile")
    return ProfileData(p.string("name"), p.string("handle"), p.string("tagline"), p.double("avatarSeed").toLong(), p.string("avatarMode", "seed"), p.double("preset").toInt(), p.double("photoStamp").toLong())
}

@Composable
fun ProfileScreen(workspace: JsonObject, onEdit: () -> Unit, onBackup: () -> Unit) {
    val profile = profileOf(workspace)
    val txs = transactions(workspace)
    val xp = txs.size * 10 + workspace.arrayOrEmpty("budgets").size() * 25 + workspace.arrayOrEmpty("goals").size() * 25 + workspace.arrayOrEmpty("financeProducts").size() * 25
    val streak = entryStreak(workspace)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NeoCard {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Pop.Lavender, Pop.Coral, Pop.Sun))))
                Box(Modifier.padding(top = 70.dp).align(Alignment.TopCenter).background(MaterialTheme.colorScheme.surface, CircleShape).padding(5.dp)) {
                    com.fincip.nativeapp.ui.UserAvatar(profile, size = 84.dp)
                }
            }
            Spacer(Modifier.height(48.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(profile.name, fontWeight = FontWeight.Black, fontSize = 24.sp)
                if (profile.handle.isNotBlank()) Text("@" + profile.handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                if (profile.tagline.isNotBlank()) Text(profile.tagline, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeoButton("Edit profile", Modifier.weight(1f), onEdit)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Entries" to txs.size.toString(), "Streak" to "${streak}d", "Level" to (xp / 100 + 1).toString()).forEach { (k, v) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(k.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                }
            }
        }
        NeoButton("Backup & privacy", Modifier.fillMaxWidth(), onBackup)
    }
}
