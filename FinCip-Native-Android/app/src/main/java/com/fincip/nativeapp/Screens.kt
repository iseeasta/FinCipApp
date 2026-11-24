package com.fincip.nativeapp

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
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
    monthlyTransactions(workspace, month).filter { com.fincip.nativeapp.data.isSpend(it) }
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
    val spendTx = monthlyTransactions(workspace, month).filter { com.fincip.nativeapp.data.isSpend(it) }
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
    return monthlyTransactions(workspace, month).filter { com.fincip.nativeapp.data.isSpend(it) }.sumOf { tx ->
        if (tx.string("currency", "INR") != cur) 0.0 else {
            val splits = tx.arrayOrEmpty("splits").objects()
            if (splits.isNotEmpty()) splits.filter { it.string("category") == b.string("category") }.sumOf { it.double("amount") }
            else if (tx.string("category") == b.string("category")) tx.double("amount") else 0.0
        }
    }
}


/** Consecutive days (ending today, or yesterday if nothing logged yet today) with at least one entry. */
fun entryStreak(workspace: JsonObject): Int {
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
fun OverviewScreen(workspace: JsonObject, month: String, privacy: Boolean, savingsRate: Double, balance: Double, onAdd: () -> Unit, onMascot: () -> Unit, onOpen: (String) -> Unit = {}, onNewGoal: () -> Unit = {}, onEditTx: (JsonObject) -> Unit = {}, onWidgets: (List<String>) -> Unit = {}) {
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val income = incomeTotal(workspace, month)
    val expenses = expenseTotal(workspace, month)
    val allTransactions = transactions(workspace)
    val takeout = monthlyTransactions(workspace, month).filter { t ->
        com.fincip.nativeapp.data.isSpend(t) && (t.string("category").contains("food", true) || t.string("title").contains("takeout", true) || t.string("title").contains("restaurant", true))
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
    AnalyticsOverview(workspace, month, privacy, base, onAdd, onOpen, onNewGoal, onEditTx, petSlot = {
        run {
            val petMood = when { ramen -> PetMood.RAMEN; achievedGoal -> PetMood.CELEBRATE; savingMood -> PetMood.THRIVING; expenses > income && income > 0 -> PetMood.STRESSED; else -> PetMood.HAPPY }
            val streak = entryStreak(workspace)
            val xp = allTransactions.size * 10 + workspace.arrayOrEmpty("budgets").size() * 25 + workspace.arrayOrEmpty("goals").size() * 25 + workspace.arrayOrEmpty("financeProducts").size() * 25 + (if (achievedGoal) 200 else 0)
            val level = xp / 100 + 1
            val condition = when { ramen -> "ramen"; achievedGoal -> "goal"; expenses > income && income > 0 -> "stress"; savingMood -> "saving"; streak >= 7 -> "streak"; allTransactions.isEmpty() -> "empty"; else -> "neutral" }
            PetHomeCard(workspace, petMood, condition, streak, onMascot)
        }
    }, widgets = homeWidgetsOf(workspace), onWidgets = onWidgets)
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
    val income = com.fincip.nativeapp.data.isInflow(item)
    NeoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.fincip.nativeapp.ui.CategoryBadge(if (item.string("type") == "transfer") "Transfer" else item.string("category", "Other"), 46.dp)
            Column(Modifier.weight(1f)) {
                Text(item.string("title", "Transaction"), fontWeight = FontWeight.Black)
                Text("${item.string("category", "Other")} · ${item.string("date")} · ${item.string("account", "Account")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                val extra = listOf(item.string("person"), item.string("project")).filter { it.isNotBlank() }.joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(com.fincip.nativeapp.ui.directionLabel(item.string("type")), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp, color = (if (income) Green else Red).copy(alpha = .8f))
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
            val out = all.filter { com.fincip.nativeapp.data.isSpend(it) }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
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
    if (picker) com.fincip.nativeapp.ui.MonthPickerDialog(monthDate, transactions(workspace).map { it.string("date").take(7) }.toSet(), { onMonthChange(it.toString()); picker = false }, { picker = false })
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
                            val hasExpense = dayTx.any { com.fincip.nativeapp.data.isSpend(it) }
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
private fun CurrencyPill(code: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val info = CURRENCY_INFO.firstOrNull { it.code == code }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .85f)).clickable { open = true }.padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(info?.flag ?: "", fontSize = 20.sp)
            Text(code, fontWeight = FontWeight.Black, color = Pop.Ink, maxLines = 1, softWrap = false)
            Text("▾", color = Pop.Ink.copy(alpha = .6f))
        }
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            CURRENCY_INFO.forEach { c ->
                androidx.compose.material3.DropdownMenuItem(text = { Text("${c.flag}  ${c.code}  ·  ${c.country}") }, onClick = { open = false; onPick(c.code) })
            }
        }
    }
}

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
    val scheme = MaterialTheme.colorScheme
    PageColumn {
        PageHeading("GLOBAL MONEY", "Convert across borders.", "${CURRENCY_INFO.size} currencies supported. Rates are reference values, not live quotes, and you can edit any of them.")
        NeoCard(fill = Pop.Mint) {
            Text("YOU CONVERT", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .6f))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .55f)).padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.text.BasicTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(14) }, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, color = Pop.Ink, fontFamily = com.fincip.nativeapp.ui.PopFont),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Pop.Ink), modifier = Modifier.weight(1f)
                )
                CurrencyPill(from) { from = it }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Pop.Ink).clickable { val t = from; from = to; to = t }, contentAlignment = Alignment.Center) { Text("⇅", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .85f)).padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (converted == null) "—" else money(converted, to), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Pop.Ink, maxLines = 1)
                    Text(if (converted == null) "A rate is missing. Add it below." else "1 $from = ${"%.6f".format(Locale.ENGLISH, fromRate / toRate)} $to", style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .7f))
                }
                CurrencyPill(to) { to = it }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1", "10", "100", "1000", "10000", "100000").forEach { q -> PopChip(q, amount == q, { amount = q }) }
            }
        }
        NeoCard {
            Text("Reference rates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("INR for 1 unit  ·  " + settings.string("fxRateSource", "Manual assumptions") + settings.string("fxUpdatedAt").takeIf { it.isNotBlank() }?.let { "  ·  $it" }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .65f))
            Spacer(Modifier.height(10.dp))
            NeoOutlineButton(if (refreshing) "Refreshing…" else "↻  Refresh rates", onRefreshRates, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            CURRENCY_INFO.forEach { info ->
                val rate = rates.double(info.code, if (info.code == "INR") 1.0 else 0.0)
                val shownRate = if (rate > 0) "%.4f".format(Locale.ENGLISH, rate) else ""
                var text by remember(info.code, rate) { mutableStateOf(shownRate) }
                val parsed = text.toDoubleOrNull()
                val changed = info.code != "INR" && text != shownRate && parsed != null && parsed > 0
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(info.flag, fontSize = 22.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(info.code, fontWeight = FontWeight.Black)
                        Text(info.country, style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .6f), maxLines = 1)
                    }
                    if (info.code == "INR") {
                        Text("Base", Modifier.clip(RoundedCornerShape(50)).background(scheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    } else {
                        androidx.compose.material3.OutlinedTextField(
                            value = text, onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(12) }, singleLine = true,
                            prefix = { Text("₹") }, modifier = Modifier.width(124.dp), shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                        )
                        Box(Modifier.width(58.dp), contentAlignment = Alignment.Center) {
                            if (changed) Text("Save", Modifier.clip(RoundedCornerShape(50)).background(scheme.primary).clickable {
                                val v = parsed ?: return@clickable
                                applyChange(fun(root: JsonObject) { val st = root.objectOrEmpty("settings"); val fx = st.objectOrEmpty("fxRates"); fx.addProperty(info.code, v); st.add("fxRates", fx); root.add("settings", st) })
                            }.padding(horizontal = 14.dp, vertical = 8.dp), color = scheme.onPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            Text("Dashboard base currency: $base. Frankfurter publishes reference rates, not live trading or settlement quotes.", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 6.dp))
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
            val used = monthlyTransactions(workspace, month).filter { com.fincip.nativeapp.data.isSpend(it) }.sumOf { tx ->
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
fun GoalsScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onContribute: (JsonObject) -> Unit, onDelete: (JsonObject) -> Unit, onEditEmergency: () -> Unit, onEdit: (JsonObject) -> Unit = {}) {
    val today = java.time.LocalDate.now()
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val goals = workspace.arrayOrEmpty("goals").objects()
    val savings = goals.filter { it.string("type") != "debt" }
    val debtsAll = goals.filter { it.string("type") == "debt" }
    PageColumn {
        PageHeading("FUTURE YOU", "Goals with momentum.", "Savings targets and debt payoff plans, with a real finish date for each.")
        run {
            val target = savings.sumOf { it.double("target") }
            val saved = savings.sumOf { it.double("saved") }
            val ratio = if (target > 0) saved / target else 0.0
            NeoCard(fill = Pop.Mint) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RingGauge(ratio.toFloat(), Pop.Forest, if (target > 0) "${(ratio * 100).toInt().coerceIn(0, 999)}%" else "—", diameter = 108.dp, caption = "saved")
                    Column(Modifier.weight(1f)) {
                        CardLabel("Savings goals")
                        Text("${savings.size} active", fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(if (privacy) "•••• saved so far" else "${compactMoney(saved, base)} of ${compactMoney(target, base)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        val monthly = savings.sumOf { it.double("monthlyContribution") }
                        if (monthly > 0) Text(if (privacy) "•••• going in each month" else "${compactMoney(monthly, base)} going in each month", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (debtsAll.isNotEmpty()) {
            val owed = debtsAll.sumOf { it.double("target") }
            val repaid = debtsAll.sumOf { it.double("saved") }
            val ratio = if (owed > 0) repaid / owed else 0.0
            val plans = debtsAll.filter { it.double("target") > it.double("saved") }.map { d ->
                com.fincip.nativeapp.data.GoalMath.simulateDebt(d.double("target") - d.double("saved"), d.double("monthlyContribution"), d.double("interestRate"))
            }
            val stuck = plans.any { it == null }
            val months = plans.filterNotNull().maxOfOrNull { it.months }
            val interest = plans.filterNotNull().sumOf { it.totalInterest }
            NeoCard(fill = Pop.Peach) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RingGauge(ratio.toFloat(), Pop.Coral, "${(ratio * 100).toInt().coerceIn(0, 100)}%", diameter = 108.dp, caption = "repaid")
                    Column(Modifier.weight(1f)) {
                        CardLabel("Debt")
                        Text(if (privacy) "••••" else "${compactMoney((owed - repaid).coerceAtLeast(0.0), base)} left", fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(
                            when {
                                plans.isEmpty() -> "All cleared. Nice."
                                stuck -> "Some payments don't cover the interest."
                                months != null -> "Debt-free around ${today.plusMonths(months.toLong()).month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${today.plusMonths(months.toLong()).year}"
                                else -> "Add monthly payments for a finish date."
                            }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold
                        )
                        if (!privacy && interest > 0 && !stuck) Text("≈ ${compactMoney(interest, base)} interest still to pay", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        NeoButton("+ Add savings or debt goal", onClick = onAdd)
        if (goals.isEmpty()) NeoCard { Text("Start with one meaningful goal", fontWeight = FontWeight.Black); Text("Emergency fund, a trip, a course or a debt payoff.", style = MaterialTheme.typography.bodySmall) }
        goals.forEach { goal ->
            val isDebt = goal.string("type") == "debt"
            val cur = goal.string("currency", "INR")
            val target = goal.double("target"); val saved = goal.double("saved")
            val remaining = (target - saved).coerceAtLeast(0.0)
            val pct = if (target > 0) (saved / target).toFloat().coerceIn(0f, 1f) else 0f
            val mo = goal.double("monthlyContribution")
            val apr = goal.double("interestRate")
            val dl = runCatching { java.time.LocalDate.parse(goal.string("deadline")) }.getOrNull()
            val purposeRaw = goal.string("category")
            val purpose = (if (isDebt) com.fincip.nativeapp.data.GoalPurposes.debt else com.fincip.nativeapp.data.GoalPurposes.saving).firstOrNull { it == purposeRaw } ?: "Other"
            val accent = if (isDebt) Pop.Coral else Pop.Forest
            val etaMonths: Int? = if (remaining <= 0) 0 else if (isDebt) com.fincip.nativeapp.data.GoalMath.simulateDebt(remaining, mo, apr)?.months else com.fincip.nativeapp.data.GoalMath.monthsToSave(remaining, mo)
            val monthsToDeadline = dl?.let { com.fincip.nativeapp.data.GoalMath.monthsUntil(it, today) }
            val needed = if (dl != null && monthsToDeadline != null && monthsToDeadline > 0 && remaining > 0) com.fincip.nativeapp.data.GoalMath.requiredMonthly(remaining, monthsToDeadline) else null
            val (status, statusColor) = when {
                remaining <= 0 -> "DONE" to Pop.Lime
                etaMonths == null -> (if (mo <= 0) "NO PLAN" else "STUCK") to Pop.Sun
                dl != null && monthsToDeadline != null && etaMonths > monthsToDeadline.coerceAtLeast(0) -> "BEHIND" to Pop.Peach
                else -> "ON TRACK" to Pop.Mint
            }
            NeoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(statusColor.copy(alpha = .55f)), contentAlignment = Alignment.Center) { Text(com.fincip.nativeapp.data.GoalPurposes.icon(purpose), fontSize = 22.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(goal.string("name", "Goal"), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text("${if (isDebt) "Debt" else "Saving"} · $purpose", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(status, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Pop.Ink, modifier = Modifier.clip(RoundedCornerShape(50)).background(statusColor).padding(horizontal = 10.dp, vertical = 5.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (privacy) "••••" else money(saved, cur), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(if (privacy) "  of ••••" else "  of ${money(target, cur)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 3.dp).weight(1f))
                    Text("${(pct * 100).toInt()}%", fontWeight = FontWeight.Black, color = accent)
                }
                ProgressBar(pct, accent, Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf(25, 50, 75, 100).forEach { m ->
                        val hit = pct * 100 >= m
                        Text(if (hit) "●  $m%" else "○  $m%", Modifier.weight(1f), fontSize = 11.sp, fontWeight = if (hit) FontWeight.Black else FontWeight.Medium, color = if (hit) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = .45f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    @Composable fun Stat(label: String, value: String, mod: Modifier) = Column(mod.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)).padding(10.dp)) {
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)); Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                    }
                    Stat(if (isDebt) "LEFT" else "TO GO", if (privacy) "••••" else compactMoney(remaining, cur), Modifier.weight(1f))
                    Stat(if (isDebt) "PAYING" else "MONTHLY", if (privacy) "••••" else if (mo > 0) compactMoney(mo, cur) else "—", Modifier.weight(1f))
                    Stat(if (remaining <= 0) "STATUS" else "FINISH", if (remaining <= 0) "Done" else etaMonths?.let { today.plusMonths(it.toLong()).let { d -> "${d.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }} ${d.year}" } } ?: "—", Modifier.weight(1f))
                }
                if (isDebt && remaining > 0) {
                    val plan = com.fincip.nativeapp.data.GoalMath.simulateDebt(remaining, mo, apr)
                    Text(
                        when {
                            mo <= 0 -> "Set a monthly payment to see the payoff date."
                            plan == null -> "Your payment is below the monthly interest, so this never shrinks."
                            else -> "Interest still to pay ≈ ${if (privacy) "••••" else money(plan.totalInterest, cur)} at ${apr}% p.a."
                        }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (!isDebt && remaining > 0 && needed != null) {
                    Text(if (mo >= needed - 0.5) "Saving enough to hit ${dl}." else "Needs ${if (privacy) "••••" else money(Math.ceil(needed), cur)} a month to hit ${dl}.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                } else if (dl != null && remaining > 0) Text("Target date ${dl}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NeoButton(if (isDebt) "Log repayment" else "Add savings", Modifier.weight(1f), { onContribute(goal) })
                    TextButtonTiny("Edit") { onEdit(goal) }
                    TextButtonTiny("Remove") { onDelete(goal) }
                }
            }
        }
        val ef = workspace.objectOrEmpty("emergencyFund")
        val monthlyEssentials = expenseTotal(workspace, currentMonth())
        val targetBalance = monthlyEssentials * ef.double("targetMonths", 6.0)
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("🛟  Emergency fund", Modifier.weight(1f), fontWeight = FontWeight.Black); InlineAction("Edit", onEditEmergency) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                val efPct = if (targetBalance > 0) (ef.double("balance") / targetBalance).toFloat() else 0f
                RingGauge(efPct, Gold, if (targetBalance > 0) "${(efPct * 100).toInt().coerceIn(0, 999)}%" else "—", diameter = 84.dp)
                Column {
                    Text(if (privacy) "••••" else money(ef.double("balance"), base), fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("Target ${ef.double("targetMonths", 6.0).toInt()} months of spending", style = MaterialTheme.typography.bodySmall)
                    val perMonth = if (ef.double("targetMonths", 6.0) > 0 && monthlyEssentials > 0) monthlyEssentials else 0.0
                    if (perMonth > 0) Text("Covers ${"%.1f".format(ef.double("balance") / perMonth)} months at this month's pace", style = MaterialTheme.typography.labelSmall)
                    else Text("Add expenses and a reserve balance to estimate your target.", style = MaterialTheme.typography.labelSmall)
                }
            }
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
fun NetWorthScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onDelete: (JsonObject) -> Unit, onEdit: (JsonObject) -> Unit = {}) {
    val items = workspace.arrayOrEmpty("netWorthItems").objects()
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val scheme = MaterialTheme.colorScheme
    fun isLiab(it: JsonObject) = it.string("type").equals("Liability", true)
    fun amt(it: JsonObject) = toBase(workspace, it.double("value", it.double("amount")), it.string("currency", "INR"))
    fun grp(it: JsonObject) = com.fincip.nativeapp.data.NetWorthGroups.groupOf(it.string("name"), it.string("type", "Asset"), it.string("group"))
    val assetItems = items.filter { !isLiab(it) }
    val debtItems = items.filter { isLiab(it) }
    val assets = assetItems.sumOf { amt(it) }
    val liabilities = debtItems.sumOf { amt(it) }
    val net = assets - liabilities
    val ratio = com.fincip.nativeapp.data.NetWorthGroups.debtRatio(assets, liabilities)
    val band = com.fincip.nativeapp.data.NetWorthGroups.band(ratio, liabilities)
    fun hide(text: String) = if (privacy) "••••" else text
    val assetGroups = assetItems.groupBy { grp(it) }.map { (g, v) -> g to v.sumOf { amt(it) } }.sortedByDescending { it.second }
    val debtGroups = debtItems.groupBy { grp(it) }.map { (g, v) -> g to v.sumOf { amt(it) } }.sortedByDescending { it.second }

    PageColumn {
        PageHeading("THE WHOLE PICTURE", "Know what you own.", "Everything you own, minus everything you owe, in one picture.")

        // ---- Hero
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Pop.Forest, Color(0xFF14463A), Pop.Ink)))) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Pop.Lime.copy(alpha = .13f), 110.dp.toPx(), Offset(size.width - 20.dp.toPx(), 10.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .06f), 80.dp.toPx(), Offset(10.dp.toPx(), size.height))
            }
            Column(Modifier.padding(22.dp)) {
                Text("NET WORTH", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = .7f))
                Spacer(Modifier.height(6.dp))
                val valueText = hide(money(net, base))
                val numSize = when { valueText.length <= 9 -> 44.sp; valueText.length <= 13 -> 36.sp; else -> 28.sp }
                Text(valueText, fontSize = numSize, lineHeight = numSize, fontWeight = FontWeight.Black, letterSpacing = (-1).sp, color = if (net < 0) Color(0xFFFFB4AB) else Color.White, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .12f)).padding(12.dp)) {
                        Text("▲ ASSETS", fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Pop.Lime)
                        Text(hide(compactMoney(assets, base)), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .12f)).padding(12.dp)) {
                        Text("▼ LIABILITIES", fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB4AB))
                        Text(hide(compactMoney(liabilities, base)), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                    }
                }
                if (assets + liabilities > 0) {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50))) {
                        Box(Modifier.fillMaxHeight().weight((assets / (assets + liabilities)).toFloat().coerceAtLeast(0.02f)).background(Pop.Lime))
                        Box(Modifier.fillMaxHeight().weight((liabilities / (assets + liabilities)).toFloat().coerceAtLeast(0.02f)).background(Color(0xFFE0614F)))
                    }
                }
            }
        }
        NeoButton("+  Add asset or liability", modifier = Modifier.fillMaxWidth(), onClick = onAdd)

        if (items.isEmpty()) {
            NeoCard {
                Text("🏦  🏠  📈  🪙", fontSize = 34.sp)
                Spacer(Modifier.height(8.dp))
                Text("NOTHING ADDED YET.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Add your savings, investments, gold, property and any loans. FinCip splits them into groups and shows how healthy the balance is. Everything stays on this device.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .7f), modifier = Modifier.padding(top = 4.dp))
            }
        } else {
            // ---- Health gauge
            NeoCard(fill = Pop.Sun.copy(alpha = .4f)) {
                CardLabel("Balance health")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RingGauge((ratio ?: 0.0).toFloat().div(100f).coerceIn(0f, 1f), if ((ratio ?: 0.0) > 60) Pop.Coral else Pop.Teal, if (ratio == null) "—" else if (privacy) "••" else "${ratio.toInt()}%", diameter = 104.dp, caption = "debt / assets")
                    Column(Modifier.weight(1f)) {
                        Text(band, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(
                            when {
                                liabilities <= 0 -> "You have no liabilities recorded."
                                net < 0 -> "Your debts are larger than your assets right now."
                                else -> "For every ₹100 you own, you owe about ₹${(ratio ?: 0.0).toInt()}."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text("A rough rule of thumb from the values you entered, not financial advice.", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(top = 6.dp))
            }

            // ---- Where your assets sit
            if (assetGroups.isNotEmpty() && assets > 0) {
                NeoCard {
                    CardLabel("Where your money sits")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DonutChart(assetGroups.mapIndexed { i, (_, v) -> ChartColors[i % ChartColors.size] to v }, centerTop = hide(compactMoney(assets, base)), centerBottom = "assets", diameter = 132.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            assetGroups.take(6).forEachIndexed { i, (g, v) -> LegendRow(ChartColors[i % ChartColors.size], "${com.fincip.nativeapp.data.NetWorthGroups.icon(g)} $g", if (privacy) "••" else "${(v / assets * 100).toInt()}%") }
                        }
                    }
                }
            }
            // ---- What you owe
            if (debtGroups.isNotEmpty() && liabilities > 0) {
                NeoCard {
                    CardLabel("What you owe")
                    debtGroups.forEach { (g, v) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${com.fincip.nativeapp.data.NetWorthGroups.icon(g)} $g", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(hide(money(v, base)), fontWeight = FontWeight.Black)
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp).height(8.dp).clip(RoundedCornerShape(50)).background(scheme.surfaceVariant)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((v / liabilities).toFloat().coerceIn(0.03f, 1f)).background(Pop.Coral))
                        }
                    }
                }
            }

            // ---- Itemised lists
            listOf("ASSETS" to assetItems, "LIABILITIES" to debtItems).forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    Text(title, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = scheme.onBackground.copy(alpha = .6f))
                    list.sortedByDescending { amt(it) }.forEach { item ->
                        val g = grp(item)
                        val liab = isLiab(item)
                        NeoCard(modifier = Modifier.clickable { onEdit(item) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(46.dp).clip(CircleShape).background(if (liab) Color(0xFFF9C6D0) else Pop.Mint), contentAlignment = Alignment.Center) { Text(com.fincip.nativeapp.data.NetWorthGroups.icon(g), fontSize = 22.sp) }
                                Column(Modifier.weight(1f)) {
                                    Text(item.string("name", "Item"), fontWeight = FontWeight.Black, maxLines = 1)
                                    Text(g, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .65f))
                                }
                                Text(hide((if (liab) "−" else "") + money(item.double("value", item.double("amount")), item.string("currency", "INR"))), fontWeight = FontWeight.Black, color = if (liab) LossDark else scheme.onSurface, maxLines = 1)
                                Box(Modifier.size(32.dp).clip(CircleShape).background(scheme.surfaceVariant).clickable { onDelete(item) }, contentAlignment = Alignment.Center) { Text("×", fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }
            }
            Text("Tap any item to edit it. Values are what you enter, converted with your saved reference rates.", style = MaterialTheme.typography.labelSmall, color = scheme.onBackground.copy(alpha = .55f))
        }
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
    "ELSS" -> Color(0xFFCFE8FF)
    "Stocks" -> Color(0xFFFFD3B0)
    "NSC" -> Color(0xFFE3D3F7)
    "SGB" -> Color(0xFFFFEBA8)
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
fun IndiaFinanceScreen(workspace: JsonObject, privacy: Boolean, onAdd: () -> Unit, onDelete: (JsonObject) -> Unit, onContribute: (JsonObject) -> Unit, onEdit: (JsonObject) -> Unit = {}) {
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
    val standardKinds = com.fincip.nativeapp.data.IndiaKinds.all.map { it.id }.filter { it != "Other" }
    val kinds = standardKinds + products.map { it.string("kind", it.string("type", "Product")) }.filter { it !in standardKinds }.distinct()
    var sortBy by remember { mutableStateOf("Value") }
    fun valOf(p: JsonObject) = toBase(workspace, p.double("currentValue", p.double("value")), p.string("currency", "INR"))
    fun gainPct(p: JsonObject): Double { val i = p.double("invested", p.double("amount")); return if (i > 0) (p.double("currentValue", p.double("value")) - i) / i else -9.0 }
    val filtered = if (selectedKind == "All") products else products.filter { it.string("kind", it.string("type", "Product")) == selectedKind }
    val shown = when (sortBy) {
        "Gain %" -> filtered.sortedByDescending { gainPct(it) }
        "Newest" -> filtered.sortedByDescending { it.string("startDate") }
        "Maturity" -> filtered.sortedBy { it.string("maturityDate").ifBlank { "9999" } }
        else -> filtered.sortedByDescending { valOf(it) }
    }
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

        if (products.isNotEmpty()) {
            MixBarCard(workspace, products, privacy)
            InvestedGrowthCard(portfolioFlows.filter { it.second < 0 }, base, privacy)
            IndiaInsightsCard(workspace, products, privacy, base)
            FyTrackerCard(workspace, products, privacy, base)
        }
        SipCalculatorCard(base)

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PopChip("All", selectedKind == "All", { selectedKind = "All" })
            kinds.forEach { kind -> PopChip(kind, selectedKind == kind, { selectedKind = kind }) }
        }
        if (products.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            listOf("Value", "Gain %", "Newest", "Maturity").forEach { PopChip(it, sortBy == it, { sortBy = it }) }
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
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .65f)), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(com.fincip.nativeapp.ui.kindIcon(kind), null, tint = Pop.Ink)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(kind.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .7f))
                        Text(p.string("name", "Investment"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Pop.Ink)
                        Text(p.string("provider", "Provider not specified"), style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .75f))
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = .55f)).clickable { onEdit(p) }, contentAlignment = Alignment.Center) { Text("✎", fontWeight = FontWeight.Black, color = Pop.Ink, fontSize = 16.sp) }
                    Spacer(Modifier.width(6.dp))
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
                if (currentTotal > 0) {
                    val share = (valOf(p) / currentTotal).toFloat().coerceIn(0f, 1f)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .55f))) {
                        if (share > 0.01f) Box(Modifier.fillMaxHeight().fillMaxWidth(share).background(Pop.Ink.copy(alpha = .75f)))
                    }
                    Text("${(share * 100).toInt()}% of portfolio", style = MaterialTheme.typography.labelSmall, color = Pop.Ink.copy(alpha = .7f), modifier = Modifier.padding(top = 3.dp))
                }
                runCatching { LocalDate.parse(p.string("maturityDate")) }.getOrNull()?.let { md ->
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), md)
                    Spacer(Modifier.height(6.dp))
                    SmallPill(if (days < 0) "Matured ${-days} days ago" else if (days == 0L) "Matures today" else if (days < 120) "Matures in $days days" else "Matures in %.1f years".format(days / 365.25))
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

private val TaxDot = listOf(Color(0xFF2E9E6B), Color(0xFFE0614F), Color(0xFF7B6CF6), Color(0xFFF2B84B))

@Composable
private fun RateCurve(points: List<Pair<Double, Double>>, currentIndex: Int, line: Color, ink: Color) {
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        if (points.size < 2) return@Canvas
        val maxY = (points.maxOf { it.second } * 1.15).coerceAtLeast(5.0)
        val w = size.width; val h = size.height
        fun pt(i: Int) = Offset(w * i / (points.size - 1), h - (points[i].second / maxY * h).toFloat())
        for (k in 1..3) drawLine(ink.copy(alpha = .08f), Offset(0f, h * k / 4), Offset(w, h * k / 4), 1.dp.toPx())
        for (i in 0 until points.size - 1) drawLine(line, pt(i), pt(i + 1), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val c = pt(currentIndex.coerceIn(0, points.lastIndex))
        drawCircle(Color.White, 8.dp.toPx(), c); drawCircle(line, 5.dp.toPx(), c)
    }
}

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
    onSave: () -> Unit,
    workspace: JsonObject = JsonObject()
) {
    val scheme = MaterialTheme.colorScheme
    val info = com.fincip.nativeapp.data.TaxEngine.info(country)
    val cur = info.currency
    var socialText by remember(country) { mutableStateOf("") }
    val gross = income.toDoubleOrNull() ?: 0.0
    val india = country == "IN"
    val input = com.fincip.nativeapp.data.TaxInput(
        country, gross, status = if (regime == "joint") "joint" else "single", extraDeductions = deductions.toDoubleOrNull() ?: 0.0,
        regime = if (regime == "old") "old" else "new", ageGroup = ageGroup, resident = resident,
        regionalPct = rate.toDoubleOrNull() ?: 0.0, socialPct = socialText.toDoubleOrNull()
    )
    val res = com.fincip.nativeapp.data.TaxEngine.compute(input)
    fun pct(v: Double) = "%.1f%%".format(Locale.ENGLISH, v)

    PageColumn {
        PageHeading("GLOBAL TAX PLANNING", "See where your pay goes.", "Real tax rules for ${com.fincip.nativeapp.data.TaxEngine.countries.size} countries, with the take-home, bracket and comparison visuals. A planning estimate, not a tax return.")
        // ---- Country picker
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.fincip.nativeapp.data.TaxEngine.countries.forEach { c ->
                PopChip("${c.flag} ${c.name}", c.code == country, { onCountry(c.code); onRegime(if (c.code == "IN") "new" else "single"); onRate("0") })
            }
        }
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.flag, fontSize = 30.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${info.name} · ${info.taxYear}", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(info.rulesNote, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .65f))
                }
            }
            if (info.approximate) Text("Approximate mode: some parts of this country's system are simplified. Check the notes below.", Modifier.padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(Pop.Sun.copy(alpha = .35f)).padding(10.dp), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Field("Annual gross income ($cur)", income, onIncome)
            if (india) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PopChip("New regime", regime != "old", { onRegime("new") }); PopChip("Old regime", regime == "old", { onRegime("old") }) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("below60" to "Under 60", "senior60" to "60 to 79", "super80" to "80+").forEach { (k, l) -> PopChip(l, ageGroup == k, { onAgeGroup(k) }) }
                    PopChip(if (resident) "Resident" else "Non-resident", resident, { onResident(!resident) })
                }
                if (regime == "old") { Spacer(Modifier.height(8.dp)); Field("Deductions: 80C, 80D, HRA, home loan… ($cur)", deductions, onDeductions) }
            } else {
                if (info.hasStatus) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PopChip(if (country == "DE") "Single (Grundtarif)" else "Single", regime != "joint", { onRegime("single") }); PopChip(if (country == "DE") "Joint (splitting)" else "Married filing jointly", regime == "joint", { onRegime("joint") }) }
                }
                Spacer(Modifier.height(8.dp))
                Field(when (country) { "US" -> "Itemised deductions, if above standard ($cur)"; "GB" -> "Pension contributions ($cur)"; "CA" -> "RRSP and other deductions ($cur)"; "DE" -> "Work expenses above €1,230 ($cur)"; else -> "Other deductible amounts ($cur)" }, deductions, onDeductions)
                info.regionalLabel?.let { Spacer(Modifier.height(8.dp)); Field(it, rate, onRate) }
                info.socialLabel?.let { Spacer(Modifier.height(8.dp)); Field(it, socialText, { socialText = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, hint = "Leave blank for the built-in estimate") }
            }
            Spacer(Modifier.height(12.dp))
            NeoButton("Save these assumptions", modifier = Modifier.fillMaxWidth(), onClick = onSave)
        }

        if (gross <= 0) {
            NeoCard { Text("Enter your annual gross income", fontWeight = FontWeight.Black); Text("Then you will see take-home pay, bracket ladder, marginal rate and how ${info.name} compares with other countries.", style = MaterialTheme.typography.bodySmall) }
        } else {
            // ---- Hero: what you keep
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Pop.Forest, Color(0xFF14463A), Pop.Ink)))) {
                Column(Modifier.padding(22.dp)) {
                    Text("YOU KEEP · ${info.taxYear}", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = .7f))
                    val keep = money(res.takeHome, cur)
                    Text(keep, fontSize = if (keep.length > 12) 30.sp else 40.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, softWrap = false)
                    Text("${money(res.takeHome / 12, cur)} a month  ·  ${pct(100 - res.effectivePct)} of gross", color = Pop.Lime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    val parts = listOf(res.takeHome to "Take-home", res.incomeTax to "Income tax", res.social to "Social / payroll", res.regional to "Regional").filter { it.first > 0.5 }
                    Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(50))) {
                        parts.forEachIndexed { idx, (v, _) -> Box(Modifier.fillMaxHeight().weight((v / res.gross).toFloat().coerceAtLeast(0.01f)).background(if (idx == 0) Pop.Lime else TaxDot[(idx) % TaxDot.size])) }
                    }
                    Spacer(Modifier.height(10.dp))
                    parts.forEachIndexed { idx, (v, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (idx == 0) Pop.Lime else TaxDot[idx % TaxDot.size]))
                            Text("  $label", Modifier.weight(1f), color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodySmall)
                            Text("${compactMoney(v, cur)}  ·  ${"%.0f".format(Locale.ENGLISH, v / res.gross * 100)}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            // ---- Rates
            NeoCard {
                CardLabel("Your tax rates")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    RingGauge((res.effectivePct / 100).toFloat().coerceIn(0f, 1f), Pop.Teal, pct(res.effectivePct), diameter = 104.dp, caption = "effective")
                    RingGauge((res.marginalPct / 100).toFloat().coerceIn(0f, 1f), Pop.Coral, pct(res.marginalPct), diameter = 104.dp, caption = "marginal")
                }
                Spacer(Modifier.height(8.dp))
                Text("Effective is the share of your total pay that goes to tax and payroll contributions. Marginal is what the next unit you earn is taxed at, including contributions.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .7f))
            }
            // ---- Line items
            NeoCard {
                CardLabel("Where it goes")
                Row { Text("Gross income", Modifier.weight(1f)); Text(money(res.gross, cur), fontWeight = FontWeight.Bold) }
                if (res.deduction > 0) Row { Text("Deductions and allowances", Modifier.weight(1f)); Text("−${money(res.deduction, cur)}", fontWeight = FontWeight.Bold, color = Green) }
                Row { Text("Taxable income", Modifier.weight(1f)); Text(money(res.taxable, cur), fontWeight = FontWeight.Black) }
                Divider(Modifier.padding(vertical = 8.dp))
                res.lines.forEach { l -> Row(Modifier.padding(vertical = 2.dp)) { Text(l.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text((if (l.amount < 0) "−" else "") + money(kotlin.math.abs(l.amount), cur), fontWeight = FontWeight.Bold, color = if (l.amount < 0) Green else scheme.onSurface) } }
                Divider(Modifier.padding(vertical = 8.dp))
                Row { Text("Total tax and contributions", Modifier.weight(1f), fontWeight = FontWeight.Black); Text(money(res.total, cur), fontWeight = FontWeight.Black) }
            }
            // ---- Bracket ladder
            val ladder = res.slices.filter { it.from < maxOf(res.taxable * 1.35, res.slices.firstOrNull { s -> s.rate > 0 }?.to ?: 0.0) }.take(9)
            if (ladder.size > 1) NeoCard {
                CardLabel("Bracket ladder")
                val maxTaxed = ladder.maxOf { it.taxed }.coerceAtLeast(1.0)
                ladder.forEach { sl ->
                    val here = res.taxable > sl.from && res.taxable <= sl.to
                    val top = if (sl.to.isInfinite()) "+" else "–${compactMoney(sl.to, cur)}"
                    Column(Modifier.padding(vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${compactMoney(sl.from, cur)}$top", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = if (here) FontWeight.Black else FontWeight.Medium)
                            if (here) Text("YOU ARE HERE", Modifier.clip(RoundedCornerShape(50)).background(scheme.primary).padding(horizontal = 8.dp, vertical = 2.dp), color = scheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text("  ${"%.1f".format(Locale.ENGLISH, sl.rate * 100).removeSuffix(".0")}%", fontWeight = FontWeight.Black)
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 3.dp).height(10.dp).clip(RoundedCornerShape(50)).background(scheme.surfaceVariant)) {
                            if (sl.taxed > 0) Box(Modifier.fillMaxHeight().fillMaxWidth((sl.taxed / maxTaxed).toFloat().coerceIn(0.03f, 1f)).background(if (here) Pop.Coral else Pop.Teal))
                        }
                        if (sl.taxed > 0) Text("${compactMoney(sl.taxed, cur)} taxed here → ${compactMoney(sl.tax, cur)}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .6f))
                    }
                }
            }
            // ---- Curve + what-if
            val mult = listOf(.25, .5, .75, 1.0, 1.25, 1.5, 2.0, 3.0)
            val curve = mult.map { m -> (gross * m) to com.fincip.nativeapp.data.TaxEngine.compute(input.copy(gross = gross * m)).effectivePct }
            val extra = gross * 0.10
            val more = com.fincip.nativeapp.data.TaxEngine.compute(input.copy(gross = gross + extra))
            val keepExtra = more.takeHome - res.takeHome
            NeoCard {
                CardLabel("Effective rate as income grows")
                RateCurve(curve.map { it.first to it.second }, 3, Pop.Coral, scheme.onSurface)
                Row { Text("${compactMoney(curve.first().first, cur)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f)); Text("${compactMoney(curve.last().first, cur)}", style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.height(8.dp))
                Text("A 10% raise (${money(extra, cur)}) would add ${money(keepExtra, cur)} to your take-home, which is ${"%.0f".format(Locale.ENGLISH, if (extra > 0) keepExtra / extra * 100 else 0.0)}% of the raise.", fontWeight = FontWeight.Bold)
            }
            // ---- Compare countries
            val baseAmt = toBase(workspace, gross, cur)
            val compare = com.fincip.nativeapp.data.TaxEngine.countries.mapNotNull { c ->
                val unit = toBase(workspace, 1.0, c.currency)
                if (unit <= 0) null else {
                    val g2 = baseAmt / unit
                    val r2 = if (c.code == country) res else com.fincip.nativeapp.data.TaxEngine.compute(com.fincip.nativeapp.data.TaxInput(c.code, g2))
                    Triple(c, g2, r2)
                }
            }.sortedBy { it.third.effectivePct }
            if (compare.size > 1) NeoCard {
                CardLabel("Same pay, other countries")
                val maxRate = compare.maxOf { it.third.effectivePct }.coerceAtLeast(1.0)
                compare.forEach { (c, g2, r2) ->
                    val me = c.code == country
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${c.flag} ${c.code}", Modifier.width(62.dp), fontWeight = if (me) FontWeight.Black else FontWeight.Medium)
                        Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(50)).background(scheme.surfaceVariant)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((r2.effectivePct / maxRate).toFloat().coerceIn(0.03f, 1f)).background(if (me) Pop.Coral else Pop.Teal))
                        }
                        Text(" ${pct(r2.effectivePct)}", Modifier.width(62.dp), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    Text("${compactMoney(r2.takeHome, c.currency)} take-home from ${compactMoney(g2, c.currency)}", Modifier.padding(start = 62.dp), style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .6f))
                }
                Text("Your income converted at your saved reference rates, run through each country's default settings (single, no extra deductions, standard contributions). Rules and living costs differ, so this is a rough guide, not advice to move.", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(top = 8.dp))
            }
            // ---- Notes
            NeoCard(fill = Pop.Sun.copy(alpha = .3f)) {
                CardLabel("Good to know")
                (res.notes + info.rulesNote).distinct().forEach { Text("•  $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
                Text("Rules were checked against official or published sources in November 2026 and change every year. This ignores capital gains, business income, treaties and many credits. Verify with your tax authority or an adviser before filing.", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
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
    val recurringExpenses = rules.filter { com.fincip.nativeapp.data.isSpend(it) }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", base)) }
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
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val ym = runCatching { java.time.YearMonth.parse(month) }.getOrDefault(java.time.YearMonth.now())
    fun spends(m: String) = monthlyTransactions(workspace, m).filter { com.fincip.nativeapp.data.isSpend(it) }.mapNotNull { t ->
        val d = runCatching { java.time.LocalDate.parse(t.string("date").take(10)) }.getOrNull() ?: return@mapNotNull null
        com.fincip.nativeapp.data.Spend(d, toBase(workspace, t.double("amount"), t.string("currency", "INR")), t.string("category", "Other"), t.string("title"))
    }
    val ins = remember(workspace, month) { com.fincip.nativeapp.data.InsightsMath.analyze(spends(month), spends(ym.minusMonths(1).toString()), ym, java.time.LocalDate.now()) }
    val groups = ins.categories.filter { it.current > 0 }.map { it.category to it.current }.sortedByDescending { it.second }
    fun m(v: Double) = if (privacy) "••••" else money(v, base)
    PageColumn {
        PageHeading("PATTERNS, NOT JUDGMENT", "Understand your spending.", "What changed, where it goes and when it leaks, for the selected month.")
        if (ins.total <= 0) {
            NeoCard { Text("No spending data for $month yet", fontWeight = FontWeight.Black); Text("Add transactions and this page turns them into patterns.", style = MaterialTheme.typography.bodySmall) }
        } else {
            val pct = ins.changePct
            NeoCard(fill = Pop.Lavender) {
                CardLabel("Spent in $month")
                Text(m(ins.total), fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (pct != null) {
                    val up = pct > 0
                    Text(
                        "${if (up) "▲" else "▼"} ${"%.0f".format(kotlin.math.abs(pct))}% ${if (up) "more" else "less"} than last month", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Pop.Ink,
                        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).background(if (up) Pop.Peach else Pop.Lime).padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                } else Text("No last-month data to compare with yet.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) { Text("DAILY AVERAGE", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp); Text(m(ins.dailyAverage), fontWeight = FontWeight.Black) }
                    Column(Modifier.weight(1f)) {
                        Text(if (ins.projected != null) "ON PACE FOR" else "LAST MONTH", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                        Text(m(ins.projected ?: ins.previousTotal), fontWeight = FontWeight.Black)
                    }
                }
                if (ins.projected != null) Text("Pace is a straight-line guess from the days so far.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }

            val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val findings = buildList {
                ins.biggestRise?.let { add("📈" to "${it.category} is up ${m(it.change)} vs last month (${m(it.previous)} → ${m(it.current)}).") }
                ins.biggestDrop?.let { add("📉" to "${it.category} fell by ${m(-it.change)} vs last month. Good move.") }
                ins.biggest?.let { add("💥" to "Biggest single spend: ${it.title.ifBlank { it.category }} · ${m(it.amount)} on ${it.date.dayOfMonth} ${it.date.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }}.") }
                ins.busiestWeekday?.let { add("📅" to "${dayNames[it]} is your heaviest day, ${m(ins.weekdayTotals[it])} in total.") }
                if (ins.weekendShare >= 0.45) add("🎉" to "${"%.0f".format(ins.weekendShare * 100)}% of spending lands on weekends.")
                groups.firstOrNull()?.let { (c, v) -> add("🥇" to "$c takes ${"%.0f".format(v / ins.total * 100)}% of everything you spent.") }
            }
            if (findings.isNotEmpty()) NeoCard {
                CardLabel("What stands out")
                findings.forEach { (icon, text) ->
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Text(icon, fontSize = 18.sp); Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)) }
                }
            }

            BreakdownCard("Category split · $month", groups, privacy, base, "spent", "No spending data for $month yet.")

            NeoCard {
                CardLabel("This month vs last month")
                val top = ins.categories.take(6)
                val maxV = top.maxOfOrNull { maxOf(it.current, it.previous) }?.takeIf { it > 0 } ?: 1.0
                top.forEach { c ->
                    Column(Modifier.padding(top = 12.dp)) {
                        Row { Text(c.category, Modifier.weight(1f), fontWeight = FontWeight.Black); Text(if (c.previous <= 0) "new" else "${if (c.change >= 0) "+" else "−"}${if (privacy) "••••" else compactMoney(kotlin.math.abs(c.change), base)}", fontWeight = FontWeight.Bold, color = if (c.change > 0) Pop.Coral else Pop.Forest, fontSize = 13.sp) }
                        ProgressBar((c.current / maxV).toFloat(), Pop.Forest, Modifier.padding(top = 4.dp))
                        ProgressBar((c.previous / maxV).toFloat(), Pop.Ink.copy(alpha = .3f), Modifier.padding(top = 3.dp))
                    }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendRow(Pop.Forest, "This month", "", Modifier.weight(1f)); LegendRow(Pop.Ink.copy(alpha = .3f), "Last month", "", Modifier.weight(1f))
                }
            }

            NeoCard {
                CardLabel("Which days cost you")
                val maxD = ins.weekdayTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                Row(Modifier.fillMaxWidth().height(110.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    ins.weekdayTotals.forEachIndexed { i, v ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((v / maxD).toFloat().coerceIn(0.04f, 1f) * .8f).clip(RoundedCornerShape(8.dp)).background(if (i == ins.busiestWeekday) Pop.Coral else Pop.Lavender))
                            Text(dayNames[i].take(1), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            if (ins.topTitles.isNotEmpty()) NeoCard {
                CardLabel("Where the money actually went")
                ins.topTitles.forEachIndexed { i, (title, v) ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", Modifier.width(22.dp), fontWeight = FontWeight.Black, color = Pop.Forest)
                        Text(title, Modifier.weight(1f), maxLines = 1)
                        Text(m(v), fontWeight = FontWeight.Bold)
                    }
                }
            }
            WeeklySpendCard(workspace, month, privacy)
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
private fun SyncTile(icon: String, title: String, body: String, fill: Color, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(fill).clickable(enabled = enabled, onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .7f)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 22.sp) }
        Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp, color = Pop.Ink)
        Text(body, style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .75f))
    }
}

@Composable
fun HouseholdScreen(
    url: String, onUrl: (String) -> Unit, anon: String, onAnon: (String) -> Unit, email: String, onEmail: (String) -> Unit, password: String, onPassword: (String) -> Unit,
    session: JsonObject, sharedBudgets: String, category: String, onCategory: (String) -> Unit, limit: String, onLimit: (String) -> Unit, currency: String, onCurrency: (String) -> Unit,
    householdName: String, onHouseholdName: (String) -> Unit, inviteCode: String, onInviteCode: (String) -> Unit, busy: Boolean,
    onSignIn: () -> Unit, onPush: () -> Unit, onPull: () -> Unit, onCreateHousehold: () -> Unit, onJoinHousehold: () -> Unit, onRefreshShared: () -> Unit, onAddSharedBudget: () -> Unit, onSignOut: () -> Unit,
    workspace: JsonObject = JsonObject()
) {
    val scheme = MaterialTheme.colorScheme
    val connected = session.string("accessToken").isNotBlank()
    val hid = session.string("householdId")
    var tab by remember { mutableStateOf("household") }
    var showConfig by remember { mutableStateOf(false) }
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val shared = remember(sharedBudgets) { runCatching { com.google.gson.JsonParser.parseString(sharedBudgets).asJsonArray.objects() }.getOrDefault(emptyList()) }
    val monthSpend = remember(workspace) { monthlyTransactions(workspace, currentMonth()).filter { com.fincip.nativeapp.data.isSpend(it) } }
    fun spentIn(cat: String) = monthSpend.filter { it.string("category").equals(cat, true) }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }

    PageColumn {
        PageHeading("OPTIONAL PRIVATE CLOUD", "Your household, together.", "Connect your own Supabase project to sync this device and share budget limits.")
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(scheme.surfaceVariant).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("household" to "🏡  Household", "sync" to "☁  Cloud sync").forEach { (k, label) ->
                val on = tab == k
                Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) scheme.primary else Color.Transparent).clickable { tab = k }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(label, fontWeight = FontWeight.Bold, color = if (on) scheme.onPrimary else scheme.onSurface, maxLines = 1)
                }
            }
        }

        if (tab == "household") {
            // ---- Hero
            NeoCard(fill = Pop.Mint) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .7f)), contentAlignment = Alignment.Center) { Text("🏡", fontSize = 36.sp) }
                    Column(Modifier.weight(1f)) {
                        Text("MY HOUSEHOLD", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .6f))
                        Text(if (hid.isNotBlank()) "Household joined" else if (connected) "No household yet" else "Not connected", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Pop.Ink)
                        Text(if (hid.isNotBlank()) "ID ${hid.take(8)}…" else if (connected) "Create one or join with an invite code." else "Connect on the Cloud sync tab first.", style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .7f))
                    }
                }
            }
            if (!connected) {
                NeoButton("Go to Cloud sync", modifier = Modifier.fillMaxWidth(), onClick = { tab = "sync" })
                NeoCard {
                    Text("How it works", fontWeight = FontWeight.Black)
                    listOf("1" to "Connect your own Supabase project on the Cloud sync tab.", "2" to "Create a household and share the invite code.", "3" to "Everyone in it sees the same monthly category limits.").forEach { (n, t) ->
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) { Text(n, color = scheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                            Text(t, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else if (hid.isBlank()) {
                NeoCard {
                    CardLabel("Start a household")
                    Field("Household name", householdName, onHouseholdName)
                    Spacer(Modifier.height(10.dp))
                    NeoButton(if (busy) "Working…" else "Create household", modifier = Modifier.fillMaxWidth(), onClick = onCreateHousehold, enabled = !busy)
                }
                NeoCard {
                    CardLabel("Or join with a code")
                    Field("Invite code", inviteCode, onInviteCode)
                    Spacer(Modifier.height(10.dp))
                    NeoOutlineButton("Join household", onJoinHousehold, Modifier.fillMaxWidth())
                }
            } else {
                // ---- Shared budgets, visual
                val totalLimit = shared.sumOf { toBase(workspace, it.double("monthly_limit"), it.string("currency", "INR")) }
                val totalUsed = shared.sumOf { spentIn(it.string("category")) }
                NeoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            CardLabel("Shared budgets · this month")
                            Text(money(totalUsed, base), fontSize = 30.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text(if (shared.isEmpty()) "No shared limits yet." else "of ${money(totalLimit, base)} shared limits · your spending only", style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .65f))
                        }
                        Text(if (busy) "…" else "↻ Refresh", Modifier.clip(RoundedCornerShape(50)).background(scheme.surfaceVariant).clickable(enabled = !busy, onClick = onRefreshShared).padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    shared.forEach { b ->
                        val cat = b.string("category"); val lim = toBase(workspace, b.double("monthly_limit"), b.string("currency", "INR")); val used = spentIn(cat)
                        val frac = if (lim > 0) (used / lim).toFloat() else 0f
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            com.fincip.nativeapp.ui.CategoryBadge(cat, 40.dp)
                            Column(Modifier.weight(1f)) {
                                Row { Text(cat, Modifier.weight(1f), fontWeight = FontWeight.Black, maxLines = 1); Text("${compactMoney(used, base)} / ${compactMoney(lim, base)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.height(5.dp))
                                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(scheme.surfaceVariant)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0.02f, 1f)).background(if (frac > 1f) Pop.Coral else if (frac > .8f) Pop.Sun else Pop.Teal))
                                }
                            }
                        }
                    }
                }
                NeoCard {
                    CardLabel("Add a shared limit")
                    com.fincip.nativeapp.ui.IconCategoryPicker(com.fincip.nativeapp.ui.EXPENSE_CATEGORIES, category, onCategory)
                    Spacer(Modifier.height(12.dp))
                    Field("Monthly limit", limit, onLimit)
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("1000", "2000", "5000", "10000", "20000").forEach { PopChip(it, limit == it, { onLimit(it) }) }
                    }
                    Selector("Currency", currency, CURRENCY_INFO.map { it.code }, onCurrency)
                    Spacer(Modifier.height(10.dp))
                    NeoButton(if (busy) "Working…" else "Add shared limit", modifier = Modifier.fillMaxWidth(), onClick = onAddSharedBudget, enabled = !busy)
                }
            }
        } else {
            // ---- Cloud sync
            NeoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(if (connected) Pop.Teal else scheme.onSurface.copy(alpha = .3f)))
                    Column(Modifier.weight(1f)) {
                        Text(if (connected) "Connected" else "Not connected", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(if (connected) "Account ${session.string("userId").take(8)}…" else "Add your Supabase details to begin.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .65f))
                    }
                    if (connected) Text("Sign out", Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onSignOut).padding(horizontal = 12.dp, vertical = 8.dp), color = scheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (connected) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SyncTile("⬆", "Push", "Send this device's data to your cloud.", Pop.Lavender, !busy, onPush, Modifier.weight(1f))
                    SyncTile("⬇", "Pull", "Replace this device with the cloud copy.", Pop.Peach, !busy, onPull, Modifier.weight(1f))
                }
                NeoCard(fill = Pop.Sun.copy(alpha = .35f)) {
                    Text("Before you sync", fontWeight = FontWeight.Black)
                    Text("Sync sends the full workspace to your Supabase account and is not end-to-end encrypted. Push replaces the cloud snapshot. Pull replaces this device's data, so push first if unsure.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
            NeoCard {
                Row(Modifier.clickable { showConfig = !showConfig }, verticalAlignment = Alignment.CenterVertically) {
                    Text("Connection settings", Modifier.weight(1f), fontWeight = FontWeight.Black)
                    Text(if (showConfig || !connected) "▴" else "▾", fontWeight = FontWeight.Black)
                }
                if (showConfig || !connected) {
                    Spacer(Modifier.height(10.dp))
                    Field("Project URL", url, onUrl, hint = "https://your-project.supabase.co")
                    Spacer(Modifier.height(8.dp))
                    Field("Public anon key", anon, onAnon)
                    Spacer(Modifier.height(8.dp))
                    Field("Email", email, onEmail)
                    Spacer(Modifier.height(8.dp))
                    Field("Password", password, onPassword)
                    Spacer(Modifier.height(12.dp))
                    NeoButton(if (busy) "Working…" else if (connected) "Reconnect" else "Sign in", modifier = Modifier.fillMaxWidth(), onClick = onSignIn, enabled = !busy)
                }
            }
            if (!connected) Text("Run the Supabase SQL file included in this Android project before connecting. This client uses Supabase Auth and row-level-security policies from that schema.", style = MaterialTheme.typography.bodySmall, color = scheme.onBackground.copy(alpha = .65f))
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

// ---------------------------------------------------------------- India finance: extra visual cards

private fun bucketOf(kind: String): String = when (kind) {
    "SIP", "Mutual fund", "ELSS", "Stocks" -> "Market-linked"
    "EPF", "PPF", "FD", "RD", "NSC", "NPS" -> "Fixed / retirement"
    "Gold", "SGB" -> "Gold"
    else -> "Other"
}

private fun valueBase(workspace: JsonObject, p: JsonObject): Double = toBase(workspace, p.double("currentValue", p.double("value")), p.string("currency", "INR"))

@Composable
private fun MixBarCard(workspace: JsonObject, products: List<JsonObject>, privacy: Boolean) {
    val buckets = products.groupBy { bucketOf(it.string("kind", it.string("type", "Other"))) }.mapValues { (_, v) -> v.sumOf { valueBase(workspace, it) } }.filter { it.value > 0 }
    val total = buckets.values.sum()
    if (total <= 0) return
    val colors = mapOf("Market-linked" to Pop.Lavender, "Fixed / retirement" to Color(0xFFB8E3F2), "Gold" to Pop.Sun, "Other" to Color(0xFFE4DFF5))
    NeoCard {
        CardLabel("Asset mix")
        Row(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(50))) {
            buckets.entries.sortedByDescending { it.value }.forEach { (k, v) ->
                Box(Modifier.fillMaxHeight().weight((v / total).toFloat().coerceAtLeast(0.02f)).background(colors[k] ?: Color.Gray))
            }
        }
        Spacer(Modifier.height(10.dp))
        buckets.entries.sortedByDescending { it.value }.forEach { (k, v) ->
            LegendRow(colors[k] ?: Color.Gray, k, if (privacy) "••" else "${(v / total * 100).toInt()}%")
            Spacer(Modifier.height(4.dp))
        }
        Text("Market-linked values move daily; fixed and retirement products grow on a schedule.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
    }
}

@Composable
private fun InvestedGrowthCard(outflows: List<Pair<LocalDate, Double>>, base: String, privacy: Boolean) {
    if (outflows.size < 2) return
    val months = com.fincip.nativeapp.data.IndiaMath.cumulativeByMonth(outflows, LocalDate.now(), 12)
    if (months.last().second <= 0.0) return
    NeoCard {
        CardLabel("Money invested · last 12 months")
        LineChart(
            labels = months.map { it.first.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) },
            series = listOf(ChartSeries("Invested", Pop.Teal, months.map { it.second })),
            currency = base, hideValues = privacy, height = 150.dp
        )
        Text("Cumulative contributions you have recorded. It does not include market gains.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
    }
}

@Composable
private fun IndiaInsightsCard(workspace: JsonObject, products: List<JsonObject>, privacy: Boolean, base: String) {
    val insights = mutableListOf<Pair<String, String>>()
    val total = products.sumOf { valueBase(workspace, it) }
    val top = products.maxByOrNull { valueBase(workspace, it) }
    if (top != null && total > 0 && products.size > 1) {
        val share = valueBase(workspace, top) / total * 100
        if (share >= 50) insights += "⚖" to "${top.string("name")} is ${share.toInt()}% of your portfolio. Heavy concentration in one holding raises risk."
    }
    val monthly = products.sumOf { toBase(workspace, it.double("monthlyContribution"), it.string("currency", "INR")) }
    if (monthly > 0) insights += "🔁" to "You commit ${if (privacy) "••••" else money(monthly, base)} every month across ${products.count { it.double("monthlyContribution") > 0 }} product(s)."
    val ranked = products.filter { it.double("invested", it.double("amount")) > 0 }.map { it to (it.double("currentValue", it.double("value")) - it.double("invested", it.double("amount"))) / it.double("invested", it.double("amount")) * 100 }
    ranked.maxByOrNull { it.second }?.let { (p, g) -> if (ranked.size > 1) insights += "🏆" to "Best so far: ${p.string("name")} at ${if (privacy) "••" else "%+.1f".format(g)}% on invested amount." }
    ranked.minByOrNull { it.second }?.let { (p, g) -> if (ranked.size > 1 && g < 0) insights += "⚠" to "${p.string("name")} is below what you put in (${if (privacy) "••" else "%.1f".format(g)}%). Check if it still fits your plan." }
    products.mapNotNull { p -> runCatching { LocalDate.parse(p.string("maturityDate")) }.getOrNull()?.let { p to ChronoUnit.DAYS.between(LocalDate.now(), it) } }
        .filter { it.second in 0..120 }.minByOrNull { it.second }?.let { (p, d) -> insights += "📅" to "${p.string("name")} matures in $d days. Decide early where the money goes next." }
    val bucketTotals = products.groupBy { bucketOf(it.string("kind", it.string("type", "Other"))) }.mapValues { (_, v) -> v.sumOf { valueBase(workspace, it) } }
    if (total > 0 && (bucketTotals["Market-linked"] ?: 0.0) == 0.0 && products.size >= 2) insights += "📈" to "Nothing here is market-linked. That is safe, but may not beat inflation over long periods."
    if (insights.isEmpty()) return
    NeoCard(fill = Pop.Sun.copy(alpha = .35f)) {
        CardLabel("Smart insights")
        insights.take(5).forEach { (icon, text) ->
            Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Text(icon, fontSize = 18.sp)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("Observations from the numbers you entered. They are not investment advice.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FyTrackerCard(workspace: JsonObject, products: List<JsonObject>, privacy: Boolean, base: String) {
    val today = LocalDate.now()
    fun fyAmount(kinds: Set<String>) = products.filter { it.string("kind") in kinds }.sumOf { p ->
        com.fincip.nativeapp.data.IndiaMath.contributedInFy(productFlows(p, p.double("invested")), today).let { toBase(workspace, it, p.string("currency", "INR")) }
    }
    val c80 = fyAmount(setOf("PPF", "ELSS", "EPF", "NSC"))
    val nps = fyAmount(setOf("NPS"))
    if (c80 <= 0 && nps <= 0 && products.none { it.string("kind") in setOf("PPF", "ELSS", "EPF", "NSC", "NPS") }) return
    NeoCard {
        CardLabel("Tax-saving tracker · ${com.fincip.nativeapp.data.IndiaMath.fiscalYearLabel(today)}")
        FyBar("Section 80C products (PPF, ELSS, EPF, NSC)", c80, 150000.0, base, privacy)
        Spacer(Modifier.height(10.dp))
        FyBar("NPS (extra 80CCD(1B) limit)", nps, 50000.0, base, privacy)
        Text("Counts contributions you recorded this April–March. Limits shown are the commonly quoted old-regime ones and tax rules change, so verify with the current Income Tax rules or a CA. This is not tax advice.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun FyBar(label: String, used: Double, limit: Double, base: String, privacy: Boolean) {
    val frac = (used / limit).toFloat().coerceIn(0f, 1f)
    Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (frac > 0.01f) Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(if (frac >= 1f) Pop.Teal else Pop.Lavender))
    }
    Text(
        if (privacy) "•••• of ${compactMoney(limit, base)}" else "${compactMoney(used, base)} of ${compactMoney(limit, base)}  ·  ${compactMoney((limit - used).coerceAtLeast(0.0), base)} room left",
        style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp)
    )
}

@Composable
private fun SipCalculatorCard(base: String) {
    var lump by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf(5000f) }
    var years by remember { mutableStateOf(10f) }
    var rate by remember { mutableStateOf(12f) }
    val months = (years * 12).toInt()
    val invested = if (lump) amount.toDouble() else amount.toDouble() * months
    val fv = if (lump) com.fincip.nativeapp.data.IndiaMath.lumpFutureValue(amount.toDouble(), rate.toDouble(), years.toDouble(), 1)
    else com.fincip.nativeapp.data.IndiaMath.sipFutureValue(amount.toDouble(), rate.toDouble(), months)
    NeoCard(fill = Pop.Mint) {
        CardLabel("What-if calculator")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PopChip("Monthly SIP", !lump, { lump = false; amount = 5000f })
            PopChip("One-time", lump, { lump = true; amount = 100000f })
        }
        Spacer(Modifier.height(8.dp))
        Text(money(fv, base), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Pop.Ink, maxLines = 1)
        Text("Put in ${money(invested, base)}  ·  growth ${money((fv - invested).coerceAtLeast(0.0), base)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Pop.Ink)
        Spacer(Modifier.height(6.dp))
        Text(if (lump) "Amount ${money(amount.toDouble(), base)}" else "Per month ${money(amount.toDouble(), base)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Pop.Ink)
        Slider(amount, { amount = (Math.round(it / 500f) * 500f).coerceAtLeast(500f) }, valueRange = 500f..(if (lump) 2000000f else 100000f))
        Text("Years ${years.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Pop.Ink)
        Slider(years, { years = Math.round(it).toFloat() }, valueRange = 1f..40f)
        Text("Expected return ${"%.1f".format(rate)}% a year", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Pop.Ink)
        Slider(rate, { rate = Math.round(it * 2f) / 2f }, valueRange = 1f..20f)
        Text("Illustration with a constant return. Real returns vary and can be negative. Not a promise or advice.", style = MaterialTheme.typography.labelSmall, color = Pop.Ink.copy(alpha = .65f))
    }
}
