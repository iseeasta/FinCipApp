package com.fincip.nativeapp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.arrayOrEmpty
import com.fincip.nativeapp.data.double
import com.fincip.nativeapp.data.expenseTotal
import com.fincip.nativeapp.data.incomeTotal
import com.fincip.nativeapp.data.money
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.objects
import com.fincip.nativeapp.data.parseMonth
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.toBase
import com.fincip.nativeapp.data.transactions
import com.fincip.nativeapp.ui.ChartSeries
import com.fincip.nativeapp.ui.DonutChart
import com.fincip.nativeapp.ui.NeoButton
import com.fincip.nativeapp.ui.NeoCard
import com.fincip.nativeapp.ui.NeoOutlineButton
import com.fincip.nativeapp.ui.Pop
import com.fincip.nativeapp.ui.PopChip
import com.fincip.nativeapp.ui.ProgressBar
import com.fincip.nativeapp.ui.compactMoney
import com.google.gson.JsonObject
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Periwinkle = Color(0xFFB7B5FF)

/** Money with the decimals rendered small and faint, e.g. ₹3,67,289.00 */
private fun bigMoney(text: String, mainSize: Int, color: Color) = buildAnnotatedString {
    val i = text.lastIndexOf('.')
    if (i > 0 && text.length - i - 1 == 2) {
        withStyle(SpanStyle(fontSize = mainSize.sp, fontWeight = FontWeight.Bold, color = color)) { append(text.substring(0, i)) }
        withStyle(SpanStyle(fontSize = (mainSize * 0.55f).sp, fontWeight = FontWeight.Medium, color = color.copy(alpha = .5f))) { append(text.substring(i)) }
    } else withStyle(SpanStyle(fontSize = mainSize.sp, fontWeight = FontWeight.Bold, color = color)) { append(text) }
}

/**
 * Smooth wave chart. Tap or drag to pick a month: a highlight band and a value bubble follow the selection.
 */
@Composable
private fun FlowChart(
    labels: List<String>, series: List<ChartSeries>, selected: Int, bubble: String,
    onSelect: (Int) -> Unit, modifier: Modifier = Modifier, height: Dp = 190.dp
) {
    val fg = MaterialTheme.colorScheme.onSurface
    val lime = MaterialTheme.colorScheme.primary
    val peak = (series.flatMap { it.values }.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val n = labels.size.coerceAtLeast(1)
    val pos by animateFloatAsState((selected + 0.5f) / n, tween(260), label = "sel")
    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
            val w = maxWidth
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(n) { detectTapGestures { o -> onSelect((o.x / size.width * n).toInt().coerceIn(0, n - 1)) } }
                    .pointerInput(n) { detectHorizontalDragGestures { change, _ -> onSelect((change.position.x / size.width * n).toInt().coerceIn(0, n - 1)) } }
            ) {
                val wp = size.width
                val hp = size.height
                val top = 44.dp.toPx()
                val bottom = 10.dp.toPx()
                fun px(i: Int) = wp * (i + 0.5f) / n
                fun py(v: Double) = (top + (1.0 - v / peak) * (hp - top - bottom)).toFloat()
                val sx = wp * pos
                drawRoundRect(
                    Brush.verticalGradient(listOf(lime.copy(alpha = .22f), Color.Transparent), startY = top),
                    topLeft = Offset(sx - 14.dp.toPx(), top - 8.dp.toPx()), size = androidx.compose.ui.geometry.Size(28.dp.toPx(), hp - top + 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
                )
                series.forEachIndexed { si, s ->
                    if (s.values.size < 2) return@forEachIndexed
                    val path = Path()
                    s.values.forEachIndexed { i, v ->
                        if (i == 0) path.moveTo(0f, py(v)).also { path.lineTo(px(0), py(v)) } else {
                            val cx = (px(i - 1) + px(i)) / 2f
                            path.cubicTo(cx, py(s.values[i - 1]), cx, py(v), px(i), py(v))
                        }
                    }
                    path.lineTo(wp, py(s.values.last()))
                    if (si == 0) {
                        val area = Path().apply { addPath(path); lineTo(wp, hp); lineTo(0f, hp); close() }
                        drawPath(area, Brush.verticalGradient(listOf(s.color.copy(alpha = .28f), Color.Transparent), startY = top))
                    }
                    drawPath(path, s.color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                series.firstOrNull()?.let { s ->
                    val f = pos * n - 0.5f
                    val i0 = f.toInt().coerceIn(0, s.values.lastIndex)
                    val i1 = (i0 + 1).coerceAtMost(s.values.lastIndex)
                    val v = s.values[i0] + (s.values[i1] - s.values[i0]) * (f - i0).coerceIn(0f, 1f)
                    drawCircle(Color.White.copy(alpha = .35f), 9.dp.toPx(), Offset(sx, py(v)))
                    drawCircle(Color.White, 4.dp.toPx(), Offset(sx, py(v)))
                }
            }
            Text(
                bubble, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1,
                modifier = Modifier.offset(x = (w * pos - 34.dp).coerceIn(0.dp, w - 68.dp), y = 0.dp).width(68.dp)
                    .clip(RoundedCornerShape(50)).background(lime).padding(vertical = 6.dp), textAlign = TextAlign.Center
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            labels.forEachIndexed { i, l ->
                Text(
                    l, Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 6.dp), textAlign = TextAlign.Center, maxLines = 1,
                    fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp,
                    color = if (i == selected) lime else fg.copy(alpha = .55f)
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, tint: Color, up: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                Icon(if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, tint = Pop.Ink, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(bigMoney(value, 20, MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Clip)
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            }
        }
    }
}

/** Dark analytics-style Overview. All numbers come from the workspace; empty data shows a start screen instead of empty charts. */
@Composable
fun AnalyticsOverview(
    workspace: JsonObject, month: String, privacy: Boolean, base: String,
    onAdd: () -> Unit, onOpen: (String) -> Unit, onNewGoal: () -> Unit, onEditTx: (JsonObject) -> Unit,
    petSlot: @Composable () -> Unit,
    widgets: List<String> = DefaultWidgets, onWidgets: (List<String>) -> Unit = {}
) {
    var editing by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var picked by remember(month) { mutableIntStateOf(5) }
    val txs = transactions(workspace)
    val scheme = MaterialTheme.colorScheme
    val mask = "••••"
    fun m(v: Double) = if (privacy) mask else money(v, base)
    val monthEnd = parseMonth(month)
    val monthList = (5 downTo 0).map { monthEnd.minusMonths(it.toLong()) }
    val incomeSeries = monthList.map { incomeTotal(workspace, it.toString()) }
    val spendSeries = monthList.map { expenseTotal(workspace, it.toString()) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        widgets.forEach { id ->
            when (id) {
                "pet" -> petSlot()
                "stats" -> {
        if (txs.isNotEmpty()) {
            val si = picked.coerceIn(0, 5)
            val label = monthList[si].format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Outgoing · $label", if (privacy) mask else compactMoney(spendSeries[si], base), scheme.secondary, Modifier.weight(1f)) { filter = "Outcome" }
                MiniStat("Incoming · $label", if (privacy) mask else compactMoney(incomeSeries[si], base), scheme.primary, Modifier.weight(1f)) { filter = "Income" }
                MiniStat("Saved · $label", if (privacy) mask else compactMoney(incomeSeries[si] - spendSeries[si], base), Pop.Sun, Modifier.weight(1f)) { filter = "Savings" }
            }
        }
                }
                "balance" -> {
        val balance = incomeTotal(workspace) - expenseTotal(workspace)
        Column {
            Text("Total balance", fontSize = 14.sp, color = scheme.onSurface.copy(alpha = .65f))
            Text(bigMoney(if (privacy) "••••••" else money(balance, base), 40, scheme.onSurface), maxLines = 1, overflow = TextOverflow.Clip)
        }
                }
                "chart" -> {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Income", "Outcome", "Savings").forEach { PopChip(it, filter == it, { filter = it }) }
        }
        if (txs.isEmpty()) {
            NeoCard {
                Text("Your financial story starts here.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text("Add a first entry and this page fills with your own charts, trends and plans. Nothing here is made up.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface.copy(alpha = .75f))
                Spacer(Modifier.height(14.dp))
                NeoButton("Add your first transaction", Modifier.fillMaxWidth(), onAdd)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeoOutlineButton("Create a goal", onNewGoal, Modifier.weight(1f))
                    NeoOutlineButton("Set a budget", { onOpen("budgets") }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                NeoOutlineButton("Import a statement", { onOpen("transactions") }, Modifier.fillMaxWidth())
            }
        } else {
            val months = monthList
            val income = incomeSeries
            val spend = spendSeries
            val net = income.zip(spend).map { (a, b) -> (a - b).coerceAtLeast(0.0) }
            val series = when (filter) {
                "Income" -> listOf(ChartSeries("Income", scheme.primary, income))
                "Outcome" -> listOf(ChartSeries("Outcome", scheme.secondary, spend))
                "Savings" -> listOf(ChartSeries("Savings", scheme.primary, net))
                else -> listOf(ChartSeries("Income", scheme.primary, income), ChartSeries("Outcome", scheme.secondary, spend))
            }
            val sel = picked.coerceIn(0, 5)
            val bubbleValue = when (filter) { "Income" -> income[sel]; "Outcome" -> spend[sel]; "Savings" -> net[sel]; else -> income[sel] - spend[sel] }
            FlowChart(
                labels = months.map { it.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) },
                series = series, selected = sel,
                bubble = if (privacy) mask else compactMoney(bubbleValue, base), onSelect = { picked = it }
            )
            // Month-over-month banner, only when the previous month has data to compare against.
            val prev = if (sel > 0) spend[sel - 1] else 0.0
            val cur = spend[sel]
            val bannerTitle: String
            val bannerBody: String
            if (prev > 0 && cur > 0) {
                val change = (cur - prev) / prev * 100
                bannerTitle = (if (change >= 0) "+" else "") + "%.1f%%".format(Locale.ENGLISH, change) + (if (change >= 0) " ↑" else " ↓")
                bannerBody = "Expense was ${if (change >= 0) "up" else "down"} compared with ${months[sel - 1].format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))}"
            } else { bannerTitle = "Not enough history yet"; bannerBody = "Add a second month of spending to see how it changes." }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFFBDBBFF), Color(0xFF8F8DF2)))).clickable { onOpen("insights") }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (privacy) mask else bannerTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Pop.Ink)
                    Text(bannerBody, fontSize = 13.sp, color = Pop.Ink.copy(alpha = .75f))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Open insights", tint = Pop.Ink)
            }

        }

                }
                "goals" -> {
        // Saving plans: ring + progress cards from the user's real goals.
        val goals = workspace.arrayOrEmpty("goals").objects().filter { it.string("type") != "debt" }
        if (goals.isNotEmpty()) {
            val colors = listOf(scheme.primary, scheme.secondary, Pop.Coral, Pop.Sun)
            val totalSaved = goals.sumOf { toBase(workspace, it.double("saved"), it.string("currency", base)) }
            Text("Saving plans", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DonutChart(
                    slices = goals.mapIndexed { i, g -> colors[i % colors.size] to toBase(workspace, g.double("saved"), g.string("currency", base)) },
                    centerTop = if (privacy) mask else compactMoney(totalSaved, base), centerBottom = "Total savings", diameter = 200.dp
                )
            }
            goals.take(4).forEachIndexed { i, g ->
                val target = g.double("target")
                val saved = g.double("saved")
                val pct = if (target > 0) (saved / target).coerceIn(0.0, 1.0) else 0.0
                Surface(Modifier.fillMaxWidth().clickable { onOpen("goals") }, shape = RoundedCornerShape(22.dp), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(g.string("name", "Goal"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (g.string("deadline").isBlank()) "No deadline" else "Due ${g.string("deadline")}", fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f))
                            }
                            Text(if (privacy) mask else money(target, g.string("currency", base)), fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        ProgressBar(pct.toFloat(), colors[i % colors.size])
                        Text("${(pct * 100).toInt()}% progress", fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f))
                    }
                }
            }
        }
                }
                "history" -> {
        if (txs.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transactions history", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("View all", Modifier.clip(RoundedCornerShape(50)).clickable { onOpen("transactions") }.padding(8.dp), color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            val shown = txs.filter { filter != "Income" || it.string("type") == "income" }.filter { filter != "Outcome" || it.string("type") != "income" }
                .sortedByDescending { it.string("date") }.take(4)
            shown.forEach { tx ->
                val isIncome = tx.string("type") == "income"
                val title = tx.string("title").ifBlank { tx.string("category", "Entry") }
                Surface(Modifier.fillMaxWidth().clickable { onEditTx(tx) }, shape = RoundedCornerShape(20.dp), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(if (isIncome) scheme.primary.copy(alpha = .9f) else scheme.secondary), contentAlignment = Alignment.Center) {
                            Text(title.take(1).uppercase(), fontWeight = FontWeight.Black, color = Pop.Ink)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${tx.string("category", "Other")} · ${tx.string("date")}", fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f), maxLines = 1)
                        }
                        val amt = toBase(workspace, tx.double("amount"), tx.string("currency", "INR"))
                        Text(if (privacy) mask else (if (isIncome) "+" else "−") + money(amt, base), fontWeight = FontWeight.Bold, color = if (isIncome) Pop.Teal else Pop.Coral, maxLines = 1)
                    }
                }
            }
        }
                }
                "quick" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeoButton("+ Entry", Modifier.weight(1f), onAdd)
                        NeoOutlineButton("+ Plan", onNewGoal, Modifier.weight(1f))
                        NeoOutlineButton("Budgets", { onOpen("budgets") }, Modifier.weight(1f))
                    }
                }
                "budgets" -> BudgetsWidget(workspace, month, base, privacy, onOpen)
                "bills" -> BillsWidget(workspace, base, privacy, onOpen)
                "categories" -> CategoriesWidget(workspace, month, base, privacy)
            }
        }
        Text("Edit home widgets", Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50)).clickable { editing = true }.padding(horizontal = 14.dp, vertical = 8.dp), color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    if (editing) WidgetEditor(widgets, onWidgets) { editing = false }
}


@Composable
private fun MiniStat(label: String, value: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tap = com.fincip.nativeapp.ui.rememberTap()
    Surface(modifier.clickable { tap(); onClick() }, shape = RoundedCornerShape(20.dp), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(tint))
            Text(value, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}


val DefaultWidgets = listOf("pet", "stats", "balance", "chart", "goals", "history", "quick")
private val WidgetNames = mapOf(
    "pet" to "Companion", "stats" to "Incoming / outgoing / saved", "balance" to "Total balance", "chart" to "Flow chart & insights",
    "goals" to "Saving plans", "history" to "Recent transactions", "quick" to "Quick actions", "budgets" to "Budgets", "bills" to "Upcoming bills", "categories" to "Top spending categories"
)

fun homeWidgetsOf(workspace: JsonObject): List<String> {
    val saved = workspace.objectOrEmpty("settings").get("homeWidgets")
    val list = if (saved != null && saved.isJsonArray) saved.asJsonArray.mapNotNull { runCatching { it.asString }.getOrNull() }.filter { it in WidgetNames } else null
    return list ?: DefaultWidgets
}

@Composable
private fun WidgetEditor(current: List<String>, onSave: (List<String>) -> Unit, onClose: () -> Unit) {
    var order by remember { mutableStateOf(current + WidgetNames.keys.filter { it !in current }) }
    var on by remember { mutableStateOf(current.toSet()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Home widgets", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                order.forEachIndexed { i, id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(id in on, { c -> on = if (c) on + id else on - id })
                        Text(WidgetNames[id] ?: id, Modifier.weight(1f), maxLines = 2)
                        Text("▲", Modifier.clickable(enabled = i > 0) { order = order.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) } }.padding(10.dp), color = MaterialTheme.colorScheme.primary)
                        Text("▼", Modifier.clickable(enabled = i < order.lastIndex) { order = order.toMutableList().also { l -> l.add(i + 1, l.removeAt(i)) } }.padding(10.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { Text("Save", Modifier.clickable { onSave(order.filter { it in on }); onClose() }.padding(12.dp), fontWeight = FontWeight.Black) },
        dismissButton = { Text("Reset", Modifier.clickable { onSave(DefaultWidgets); onClose() }.padding(12.dp)) }
    )
}

@Composable
private fun BudgetsWidget(workspace: JsonObject, month: String, base: String, privacy: Boolean, onOpen: (String) -> Unit) {
    val budgets = workspace.arrayOrEmpty("budgets").objects()
    val tx = com.fincip.nativeapp.data.monthlyTransactions(workspace, month).filter { it.string("type") != "income" }
    Text("Budgets", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    if (budgets.isEmpty()) { NeoCard { Text("No budgets yet."); Spacer(Modifier.height(8.dp)); NeoOutlineButton("Set a budget", { onOpen("budgets") }, Modifier.fillMaxWidth()) }; return }
    budgets.take(5).forEach { b ->
        val limit = toBase(workspace, b.double("limit"), b.string("currency", base))
        val spent = tx.filter { it.string("category").equals(b.string("category"), true) }.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
        val pct = if (limit > 0) (spent / limit).toFloat() else 0f
        NeoCard {
            Row { Text(b.string("category", "Budget"), Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(if (privacy) "••••" else "${money(spent, base)} / ${money(limit, base)}", fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
            ProgressBar(pct.coerceIn(0f, 1f), if (pct >= 1f) Pop.Coral else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BillsWidget(workspace: JsonObject, base: String, privacy: Boolean, onOpen: (String) -> Unit) {
    val bills = workspace.arrayOrEmpty("recurringRules").objects().filter { it.string("nextDate").isNotBlank() }.sortedBy { it.string("nextDate") }.take(4)
    Text("Upcoming bills", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    NeoCard {
        if (bills.isEmpty()) Text("No bills scheduled.")
        bills.forEach { b ->
            Row(Modifier.fillMaxWidth().clickable { onOpen("recurring") }.padding(vertical = 6.dp)) {
                Column(Modifier.weight(1f)) { Text(b.string("title", "Bill"), fontWeight = FontWeight.Bold, maxLines = 1); Text(b.string("nextDate"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)) }
                Text(if (privacy) "••••" else money(b.double("amount"), b.string("currency", base)), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoriesWidget(workspace: JsonObject, month: String, base: String, privacy: Boolean) {
    val by = com.fincip.nativeapp.data.monthlyTransactions(workspace, month).filter { it.string("type") != "income" }
        .groupBy { it.string("category", "Other").ifBlank { "Other" } }
        .mapValues { (_, l) -> l.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) } }.entries.sortedByDescending { it.value }.take(5)
    Text("Top spending this month", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    NeoCard {
        if (by.isEmpty()) Text("No spending logged this month.")
        val peak = by.firstOrNull()?.value ?: 1.0
        by.forEach { (cat, v) ->
            Row { Text(cat, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(if (privacy) "••••" else money(v, base), fontSize = 12.sp) }
            Spacer(Modifier.height(4.dp))
            ProgressBar((v / peak).toFloat().coerceIn(0f, 1f), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
    }
}
