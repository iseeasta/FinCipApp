package com.fincip.nativeapp.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.csv.*
import com.fincip.nativeapp.data.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val Mono = FontFamily.Monospace
private fun fmtMoney(v: Double) = "%,.2f".format(Locale.US, v)

/** Import & export hub. Imported rows go through the same transaction model and the same save path as manual entries. */
@Composable
fun DataIOScreen(
    workspace: JsonObject,
    onImport: (List<JsonObject>, JsonObject) -> Boolean,
    onUndo: (String) -> Unit,
    onShowMonth: (String) -> Unit,
    onExportJson: () -> Unit,
    onLegacyText: (String) -> Boolean,
    notify: (String) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NeoTitle("Your data, portable", if (tab == 0) "Import transactions" else "Export your financial data", if (tab == 0) "Bring in a CSV from a bank, UPI app, spreadsheet or another tracker. Every row lands on its real date." else "Choose how you want to export your transactions.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PopChip("Import", tab == 0, { tab = 0 }); PopChip("Export", tab == 1, { tab = 1 })
        }
        if (tab == 0) ImportTab(workspace, onImport, onUndo, onShowMonth, onLegacyText, notify) else ExportTab(workspace, onExportJson, notify)
    }
}

// ============================================================ IMPORT

private class ImportDone(val batchId: String, val count: Int, val duplicatesSkipped: Int, val invalid: Int, val months: List<String>)

@Composable
private fun ImportTab(
    workspace: JsonObject, onImport: (List<JsonObject>, JsonObject) -> Boolean, onUndo: (String) -> Unit,
    onShowMonth: (String) -> Unit, onLegacyText: (String) -> Boolean, notify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fileName by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 0) }
    var result by remember { mutableStateOf<ImportDone?>(null) }
    var showMapping by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("Needs review") }
    val edits = remember { mutableStateMapOf<Int, ImportRow>() }
    val excluded = remember { mutableStateMapOf<Int, Boolean>() }
    val forced = remember { mutableStateMapOf<Int, Boolean>() }
    var source by remember { mutableStateOf("") }

    val existing = remember(workspace) {
        val txs = transactions(workspace)
        ExistingIndex(txs.map { ExistingIndex.keyFor(it.string("date").take(10), it.double("amount"), it.string("type", "expense"), it.string("title")) }, txs.map { it.string("reference") })
    }

    fun runParse(content: String, roles: List<Role>? = null, order: DateOrder? = null) {
        busy = true; progress = 0 to 0
        scope.launch {
            val p = withContext(Dispatchers.Default) {
                ImportEngine.parse(content, existing, workspace.objectOrEmpty("settings").string("baseCurrency", "INR"), roles, order) { a, b -> progress = a to b }
            }
            preview = p; edits.clear(); excluded.clear(); forced.clear()
            source = ImportEngine.guessSource(p.headers, content)
            filter = if (p.rows.any { it.state == RowState.NEW && it.issue.isNotBlank() }) "Needs review" else "All"
            busy = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        result = null; preview = null
        scope.launch {
            busy = true
            val read = withContext(Dispatchers.IO) {
                runCatching {
                    val name = context.contentResolver.query(uri, null, null, null, null)?.use { c -> val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && i >= 0) c.getString(i) else null } ?: "statement.csv"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    name to Csv.decode(bytes)
                }
            }
            val pair = read.getOrNull()
            if (pair == null || pair.second.isBlank()) { busy = false; notify("Could not read that file."); return@launch }
            if (pair.second.trimStart().let { it.startsWith("OFXHEADER") || it.contains("<OFX>", true) }) { busy = false; notify(if (onLegacyText(pair.second)) "OFX statement imported" else "No transactions found in the OFX file"); return@launch }
            fileName = pair.first; text = pair.second
            runParse(pair.second)
        }
    }

    // ---- success screen
    result?.let { r ->
        NeoCard(fill = Pop.Mint) {
            Text("Import complete", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Pop.Ink)
            Text("${r.count} transactions were added on their original dates.", color = Pop.Ink)
            if (r.duplicatesSkipped > 0) Text("${r.duplicatesSkipped} duplicates skipped · ${r.invalid} invalid rows left out", color = Pop.Ink.copy(alpha = .7f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text("Open a month", fontWeight = FontWeight.Bold, color = Pop.Ink)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                r.months.take(24).forEach { m -> PopChip(YearMonth.parse(m).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)), false, { onShowMonth(m) }) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoOutlineButton("Undo this import", { onUndo(r.batchId); result = null; notify("Import undone — ${r.count} transactions removed") }, Modifier.weight(1f))
                NeoButton("Import another", Modifier.weight(1f), onClick = { result = null; preview = null })
            }
        }
    }

    val p = preview
    if (result == null && p == null) {
        NeoCard {
            Text("Choose a file", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("CSV or TSV from PhonePe, Google Pay, Paytm, BHIM, your bank, a credit card, Excel/Sheets or another expense tracker. Columns are detected automatically and you review everything before it is saved.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
            Spacer(Modifier.height(10.dp))
            NeoButton(if (busy) "Reading…" else "Select CSV file", Modifier.fillMaxWidth(), onClick = { picker.launch(arrayOf("*/*")) }, enabled = !busy)
            if (busy) { Spacer(Modifier.height(10.dp)); ProgressLine(progress) }
        }
        Text("Tip: download the statement from the other app first (UPI apps and banks offer this under Statements/Reports).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
    }

    if (result == null && p != null) {
        val eff = { r: ImportRow -> edits[r.rowNo] ?: r }
        val included = remember(p, edits.toMap(), excluded.toMap(), forced.toMap()) {
            p.rows.map(eff).filter { (it.state == RowState.NEW && excluded[it.rowNo] != true) || (it.state == RowState.DUPLICATE && forced[it.rowNo] == true) }
        }
        val review = p.rows.count { it.state == RowState.NEW && it.issue.isNotBlank() }
        val inAmt = included.filter { it.type == "income" }.sumOf { it.amount }
        val outAmt = included.filter { it.type == "expense" }.sumOf { it.amount }
        val refunds = included.filter { it.type == "refund" }.sumOf { it.amount }
        val transfers = included.count { it.type == "transfer" }

        NeoCard {
            Text(fileName, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Detected source: $source · delimiter “${if (p.delimiter == '\t') "tab" else p.delimiter.toString()}” · header on row ${p.headerRow + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MetricTile("Found", p.rows.size.toString(), "of ${p.totalDataRows} rows") }
                Box(Modifier.weight(1f)) { MetricTile("New", included.count { it.state == RowState.NEW }.toString(), "to import", fill = Pop.Mint) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MetricTile("Duplicates", p.duplicates.toString(), "already in FinCip", fill = Pop.Peach) }
                Box(Modifier.weight(1f)) { MetricTile("Invalid", (p.errors + p.skipped).toString(), "${p.skipped} skipped, ${p.errors} errors", fill = Pop.Coral) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MetricTile("Incoming", fmtMoney(inAmt), if (refunds > 0) "+ ${fmtMoney(refunds)} refunds" else "income") }
                Box(Modifier.weight(1f)) { MetricTile("Outgoing", fmtMoney(outAmt), if (transfers > 0) "$transfers transfers excluded" else "expenses") }
            }
            Spacer(Modifier.height(8.dp))
            val range = included.minOfOrNull { it.date }?.let { a -> "$a → ${included.maxOf { it.date }}" } ?: "—"
            Text("Date range: $range · needs review: $review", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        p.warnings.forEach { w -> NeoCard(fill = Pop.Sun.copy(alpha = .35f)) { Text(w, fontSize = 13.sp, color = Pop.Ink) } }

        NeoCard {
            Text("Dates & columns", fontWeight = FontWeight.Black)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PopChip("Day / Month / Year", p.dateOrder == DateOrder.DAY_FIRST, { if (p.dateOrder != DateOrder.DAY_FIRST) runParse(text, p.roles, DateOrder.DAY_FIRST) })
                PopChip("Month / Day / Year", p.dateOrder == DateOrder.MONTH_FIRST, { if (p.dateOrder != DateOrder.MONTH_FIRST) runParse(text, p.roles, DateOrder.MONTH_FIRST) })
                PopChip("Map columns", false, { showMapping = true })
            }
            if (busy) ProgressLine(progress)
        }

        // list
        val shown = p.rows.filter {
            when (filter) {
                "Needs review" -> it.state == RowState.NEW && it.issue.isNotBlank()
                "New" -> it.state == RowState.NEW
                "Duplicates" -> it.state == RowState.DUPLICATE
                "Invalid" -> it.state == RowState.ERROR || it.state == RowState.SKIPPED
                else -> true
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Needs review", "New", "Duplicates", "Invalid", "All").forEach { f -> PopChip(f, filter == f, { filter = f }) }
        }
        val limit = 60
        if (shown.isEmpty()) Text("Nothing in this view.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
        shown.take(limit).forEach { raw -> RowCard(eff(raw), excluded, forced, edits) }
        if (shown.size > limit) Text("Showing the first $limit of ${shown.size}. Rows not shown are imported exactly as detected.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoOutlineButton("Cancel", { preview = null; text = "" }, Modifier.weight(1f))
            NeoButton("Import ${included.size}", Modifier.weight(1f), enabled = included.isNotEmpty() && !busy, onClick = {
                busy = true
                scope.launch {
                    val batch = UUID.randomUUID().toString()
                    val built = withContext(Dispatchers.Default) { included.map { toJson(it, batch) } }
                    val rec = JsonObject().apply {
                        addProperty("id", batch); addProperty("file", fileName); addProperty("source", source); addProperty("count", built.size)
                        addProperty("at", java.time.Instant.now().toString())
                        addProperty("from", included.minOfOrNull { it.date } ?: ""); addProperty("to", included.maxOfOrNull { it.date } ?: "")
                    }
                    val ok = onImport(built, rec)
                    busy = false
                    if (ok) {
                        result = ImportDone(batch, built.size, p.duplicates - included.count { it.state == RowState.DUPLICATE }, p.errors + p.skipped, included.map { it.date.take(7) }.distinct().sorted().reversed())
                        preview = null
                    } else notify("Import failed — nothing was changed.")
                }
            })
        }
    }

    if (showMapping && p != null) {
        val roles = remember(p) { mutableStateListOf<Role>().apply { addAll(p.roles) } }
        AlertDialog(
            onDismissRequest = { showMapping = false },
            title = { Text("Map columns", fontWeight = FontWeight.Black) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.headers.forEachIndexed { i, h ->
                        Selector(h.ifBlank { "Column ${i + 1}" }, roles[i].name.lowercase().replaceFirstChar { it.uppercase() }, Role.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }) { pick -> roles[i] = Role.values().first { it.name.equals(pick, true) } }
                    }
                }
            },
            confirmButton = { TextButton({ showMapping = false; runParse(text, roles.toList(), p.dateOrder) }) { Text("Apply") } },
            dismissButton = { TextButton({ showMapping = false }) { Text("Cancel") } }
        )
    }

    // history with undo
    val batches = workspace.arrayOrEmpty("importBatches").objects().reversed()
    if (batches.isNotEmpty() && p == null) {
        Text("Import history", fontWeight = FontWeight.Black, fontSize = 16.sp)
        batches.take(10).forEach { b ->
            NeoCard {
                Text(b.string("file").ifBlank { "Import" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val still = transactions(workspace).count { it.string("importId") == b.string("id") }
                Text("${b.string("source")} · $still of ${b.long("count")} transactions · ${b.string("from")} → ${b.string("to")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                if (still > 0) { Spacer(Modifier.height(6.dp)); NeoOutlineButton("Undo this import", { onUndo(b.string("id")); notify("Import undone") }) }
                else Text("Undone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Pair<Int, Int>) {
    val (a, b) = progress
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (b > 0) { ProgressBar((a.toFloat() / b).coerceIn(0f, 1f)); Text("Processed $a of $b rows", fontSize = 12.sp) } else LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun RowCard(row: ImportRow, excluded: SnapshotStateMap<Int, Boolean>, forced: SnapshotStateMap<Int, Boolean>, edits: SnapshotStateMap<Int, ImportRow>) {
    val scheme = MaterialTheme.colorScheme
    val importable = row.state == RowState.NEW || row.state == RowState.DUPLICATE
    val on = if (row.state == RowState.NEW) excluded[row.rowNo] != true else forced[row.rowNo] == true
    val tone = when (row.state) { RowState.NEW -> if (row.issue.isNotBlank()) Pop.Sun else scheme.primary; RowState.DUPLICATE -> Pop.Peach; else -> Pop.Coral }
    val inflow = row.type == "income" || row.type == "refund"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(scheme.surface).border1(tone).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (importable) Checkbox(on, { v -> if (row.state == RowState.NEW) excluded[row.rowNo] = !v else forced[row.rowNo] = v })
            Column(Modifier.weight(1f)) {
                Text(row.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${row.date.ifBlank { "no date" }} · ${row.category}${if (row.account.isNotBlank()) " · " + row.account else ""} · row ${row.rowNo}", fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text((if (inflow) "+" else if (row.type == "transfer") "↔ " else "−") + fmtMoney(row.amount), fontWeight = FontWeight.Black, color = if (row.type == "transfer") scheme.onSurface else if (inflow) Pop.Teal else Pop.Coral)
        }
        if (row.issue.isNotBlank()) Text(row.issue, fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .75f))
        if (row.state == RowState.NEW) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ChipMenu("Type: ${row.type}", listOf("expense", "income", "transfer", "refund")) { t -> edits[row.rowNo] = row.copy(type = t, category = if (t == "transfer") "Transfer" else if (t == "refund") "Refund" else row.category, classifiedBy = "edited by you") }
                ChipMenu(row.category, listOf("Food", "Transport", "Shopping", "Bills", "Health", "Education", "Entertainment", "Travel", "Home", "Subscriptions", "Personal care", "EMI", "Salary", "Freelance", "Business", "Investment", "Gift", "Refund", "Transfer", "Other")) { c -> edits[row.rowNo] = row.copy(category = c) }
                if (row.classifiedBy.isNotBlank()) Text("via ${row.classifiedBy}", fontSize = 10.sp, color = scheme.onSurface.copy(alpha = .5f))
            }
        }
        if (row.state == RowState.DUPLICATE) Text(if (on) "Will be imported anyway" else "Skipped as duplicate — tick to import anyway", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.border1(c: Color) = this.then(Modifier.border(1.5.dp, c, RoundedCornerShape(16.dp)))

@Composable
private fun ChipMenu(label: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PopChip(label, false, { open = true })
        DropdownMenu(open, { open = false }) { options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onPick(o) }) } }
    }
}

private fun toJson(r: ImportRow, batch: String) = JsonObject().apply {
    addProperty("id", UUID.randomUUID().toString()); addProperty("title", r.title); addProperty("amount", r.amount); addProperty("currency", r.currency)
    addProperty("type", r.type); addProperty("category", r.category); addProperty("date", r.date)
    addProperty("account", r.account.ifBlank { "Imported" }); addProperty("note", r.note)
    if (r.ref.isNotBlank()) addProperty("reference", r.ref)
    add("tags", JsonArray().apply { r.tags.split(';', ',').map { it.trim() }.filter { it.isNotEmpty() }.forEach(::add); add("imported") })
    addProperty("source", "csv-import"); addProperty("importId", batch)
}

// ============================================================ EXPORT

private enum class RangeMode(val label: String) { ALL("All time"), THIS("This month"), LAST("Last month"), MONTH("Specific month"), YEAR("Specific year"), CUSTOM("Custom range") }

private fun toEx(t: JsonObject): ExTxn? {
    val d = runCatching { LocalDate.parse(t.string("date").take(10)) }.getOrNull() ?: return null
    return ExTxn(
        t.string("id"), d, kotlin.math.abs(t.double("amount")), t.string("type", "expense"), t.string("title"), t.string("category"), t.string("account"),
        t.string("currency", "INR"), t.string("note"), t.arrayOrEmpty("tags").map { it.asStringOr() }.filter { it != "imported" }.joinToString(";"), t.string("reference")
    )
}

@Composable
private fun ExportTab(workspace: JsonObject, onExportJson: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val all = remember(workspace) { transactions(workspace).mapNotNull(::toEx) }
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")

    var mode by remember { mutableStateOf("") } // "", "universal", "app"
    var profileId by remember { mutableStateOf("universal") }
    var query by remember { mutableStateOf("") }
    var range by remember { mutableStateOf(RangeMode.ALL) }
    var pickedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showMonth by remember { mutableStateOf(false) }
    val years = remember(all) { all.map { it.date.year }.distinct().sortedDescending().ifEmpty { listOf(LocalDate.now().year) } }
    var year by remember(years) { mutableIntStateOf(years.first()) }
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(Kind.ALL) }
    var cats by remember { mutableStateOf(setOf<String>()) }
    var accs by remember { mutableStateOf(setOf<String>()) }

    val profile = Profiles.byId(profileId) ?: Profiles.universal
    val filter = remember(range, pickedMonth, year, fromText, toText, kind, cats, accs) {
        val now = YearMonth.now()
        val base0 = when (range) {
            RangeMode.ALL -> ExportFilter()
            RangeMode.THIS -> ExportFilter.month(now)
            RangeMode.LAST -> ExportFilter.month(now.minusMonths(1))
            RangeMode.MONTH -> ExportFilter.month(pickedMonth)
            RangeMode.YEAR -> ExportFilter.year(year)
            RangeMode.CUSTOM -> ExportFilter(runCatching { LocalDate.parse(fromText.trim()) }.getOrNull(), runCatching { LocalDate.parse(toText.trim()) }.getOrNull())
        }
        base0.copy(kind = kind, categories = cats, accounts = accs)
    }
    val selected = remember(all, filter) { filter.apply(all) }
    val sum = remember(selected) { summarize(selected) }
    val fileName = profile.fileName(filter, selected)

    var pendingText by remember { mutableStateOf("") }
    fun write(uri: Uri?, bytes: () -> ByteArray) {
        if (uri == null) return
        scope.launch(Dispatchers.IO) {
            val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes()) } != null }.getOrDefault(false)
            withContext(Dispatchers.Main) { notify(if (ok) "Exported ${selected.size} transactions" else "Export failed") }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { write(it) { pendingText.toByteArray(Charsets.UTF_8) } }
    val tsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/tab-separated-values")) { write(it) { pendingText.toByteArray(Charsets.UTF_8) } }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ReportPdf.write(out, selected, periodLabel(filter, selected), filterNote(filter), base) { toBase(workspace, it.amount, it.currency) }
                } != null
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { notify(if (ok) "PDF report saved" else "PDF export failed") }
        }
    }

    NeoCard {
        Text("Choose Export Format", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        FormatTile("Universal CSV", "One clean, application-independent file. Opens in Excel, Sheets and most finance software, and FinCip can re-import it.", mode == "universal") { mode = "universal"; profileId = "universal" }
        Spacer(Modifier.height(8.dp))
        FormatTile("Application-specific export", "Pick the app you are moving to and get its column layout. Compatibility is labelled honestly for each app.", mode == "app") { mode = "app"; if (profileId == "universal") profileId = "" }
        Spacer(Modifier.height(8.dp))
        FormatTile("PDF report", "A designed financial report with summary, charts and the full transaction table.", mode == "pdf") { mode = "pdf" }
    }

    if (mode == "app") {
        NeoCard {
            Field("Search apps", query, { query = it }, hint = "PhonePe, HDFC, Spendee…")
            Spacer(Modifier.height(8.dp))
            AppGroup.values().filter { it != AppGroup.GENERIC }.forEach { g ->
                val list = Profiles.search(query).filter { it.group == g }
                if (list.isNotEmpty()) {
                    Text(g.title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, color = scheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    list.forEach { pr -> AppRow(pr, pr.id == profileId) { profileId = pr.id } }
                }
            }
            if (Profiles.search(query).isEmpty()) Text("No app matches “$query”.", fontSize = 13.sp)
        }
    }

    val ready = mode == "universal" || mode == "pdf" || (mode == "app" && profileId.isNotBlank() && Profiles.byId(profileId) != null)
    if (ready) {
        // ---- filters
        NeoCard {
            Text("What to export", fontWeight = FontWeight.Black)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeMode.values().forEach { r -> PopChip(r.label, range == r, { range = r; if (r == RangeMode.MONTH) showMonth = true }) }
            }
            if (range == RangeMode.MONTH) Text("Month: ${pickedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))} (tap “Specific month” again to change)", fontSize = 12.sp)
            if (range == RangeMode.YEAR) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { years.forEach { y -> PopChip(y.toString(), year == y, { year = y }) } }
            if (range == RangeMode.CUSTOM) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("From (YYYY-MM-DD)", fromText, { fromText = it }, Modifier.weight(1f)); Field("To (YYYY-MM-DD)", toText, { toText = it }, Modifier.weight(1f))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PopChip("Income & expenses", kind == Kind.ALL, { kind = Kind.ALL }); PopChip("Income only", kind == Kind.INCOME, { kind = Kind.INCOME }); PopChip("Expenses only", kind == Kind.EXPENSE, { kind = Kind.EXPENSE })
            }
            val catList = remember(all) { all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
            if (catList.isNotEmpty()) { Text("Categories ${if (cats.isEmpty()) "(all)" else "(${cats.size})"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { catList.forEach { c -> PopChip(c, c in cats, { cats = if (c in cats) cats - c else cats + c }) } } }
            val accList = remember(all) { all.map { it.account }.filter { it.isNotBlank() }.distinct().sorted() }
            if (accList.isNotEmpty()) { Text("Payment methods ${if (accs.isEmpty()) "(all)" else "(${accs.size})"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { accList.forEach { a -> PopChip(a, a in accs, { accs = if (a in accs) accs - a else accs + a }) } } }
        }
        if (showMonth) MonthPickerDialog(pickedMonth, all.map { it.date.toString().take(7) }.toSet(), { pickedMonth = it; showMonth = false }, { showMonth = false })

        // ---- preview
        NeoCard {
            Text("Preview", fontWeight = FontWeight.Black, fontSize = 18.sp)
            val target = if (mode == "pdf") "PDF report" else profile.name
            PreviewLine("Target", target); PreviewLine("Format", if (mode == "pdf") "PDF · A4 · multi-page" else "${profile.extension.uppercase()} · UTF-8 · ${if (profile.delimiter == '\t') "tab" else "comma"}-separated")
            PreviewLine("Transactions", sum.count.toString())
            PreviewLine("Date range", if (sum.from == null) "—" else "${sum.from} → ${sum.to}")
            PreviewLine("Total income", "$base ${fmtMoney(selected.filter { it.type == "income" }.sumOf { toBase(workspace, it.amount, it.currency) })}")
            PreviewLine("Total expenses", "$base ${fmtMoney(selected.filter { it.type !in setOf("income", "transfer", "refund") }.sumOf { toBase(workspace, it.amount, it.currency) } - selected.filter { it.type == "refund" }.sumOf { toBase(workspace, it.amount, it.currency) })}")
            if (sum.transfers > 0 || sum.refunds > 0) PreviewLine("Transfers / refunds", "${sum.transfers} / ${sum.refunds}")
            PreviewLine("File name", if (mode == "pdf") "FinCip_Report_${fileName.substringAfterLast("_").substringBeforeLast(".")}.pdf" else fileName)
            if (mode != "pdf") {
                Spacer(Modifier.height(8.dp))
                CompatBadge(profile.compat)
                Text(profile.compat.note, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                if (profile.source.isNotBlank()) Text("Basis: ${profile.source}", fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(8.dp))
                Text("Fields included", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(profile.columns.joinToString(" · ") { it.header + if (it.required) "*" else "" }, fontSize = 12.sp)
                Text("* required by the target format", fontSize = 10.sp, color = scheme.onSurface.copy(alpha = .55f))
                if (profile.cannotTransfer.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp)); Text("Cannot be transferred / caveats", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    profile.cannotTransfer.forEach { Text("• $it", fontSize = 12.sp) }
                }
                if (sum.transfers > 0 && profile.id != "universal") Text("• Own-account transfers are written like any other debit/credit — this format has no transfer type.", fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("Structure preview", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val sample = remember(profileId, selected) { profile.build(selected.take(3)).lines().filter { it.isNotBlank() }.joinToString("\n") }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(scheme.surfaceVariant).horizontalScroll(rememberScrollState()).padding(10.dp)) { Text(sample, fontFamily = Mono, fontSize = 10.sp, softWrap = false) }
            }
            Spacer(Modifier.height(12.dp))
            val enabled = selected.isNotEmpty() && (mode == "pdf" || profile.enabled)
            NeoButton(if (selected.isEmpty()) "Nothing to export" else if (mode == "pdf") "Export PDF report" else "Export ${profile.extension.uppercase()}", Modifier.fillMaxWidth(), enabled = enabled, onClick = {
                if (mode == "pdf") pdfLauncher.launch("FinCip_Report_${fileName.substringAfterLast("_").substringBeforeLast(".")}.pdf")
                else scope.launch {
                    pendingText = withContext(Dispatchers.Default) { profile.build(selected) }
                    if (profile.extension == "tsv") tsvLauncher.launch(fileName) else csvLauncher.launch(fileName)
                }
            })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NeoOutlineButton("JSON backup", onExportJson, Modifier.weight(1f)) }
}

private fun periodLabel(f: ExportFilter, t: List<ExTxn>): String = when {
    f.from != null && f.to != null -> "${f.from} to ${f.to}"
    t.isNotEmpty() -> "${t.minOf { it.date }} to ${t.maxOf { it.date }}"
    else -> "All time"
}
private fun filterNote(f: ExportFilter): String = listOfNotNull(
    f.kind.takeIf { it != Kind.ALL }?.let { if (it == Kind.INCOME) "income only" else "expenses only" },
    f.categories.takeIf { it.isNotEmpty() }?.let { "categories: " + it.joinToString() }, f.accounts.takeIf { it.isNotEmpty() }?.let { "methods: " + it.joinToString() }
).joinToString(" · ")

@Composable
private fun FormatTile(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) scheme.primaryContainer else scheme.surfaceVariant).clickable { onClick() }.padding(14.dp)
    ) {
        Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (selected) scheme.onPrimaryContainer else scheme.onSurface)
        Text(body, fontSize = 12.sp, color = (if (selected) scheme.onPrimaryContainer else scheme.onSurface).copy(alpha = .75f))
    }
}

@Composable
private fun AppRow(pr: ExportProfile, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) scheme.primaryContainer else Color.Transparent).clickable(enabled = pr.enabled) { onClick() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(pr.brandColor)), contentAlignment = Alignment.Center) { Text(pr.initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
        Column(Modifier.weight(1f)) {
            Text(pr.name, fontWeight = FontWeight.Bold, color = if (pr.enabled) scheme.onSurface else scheme.onSurface.copy(alpha = .45f))
            CompatBadge(pr.compat)
        }
    }
}

@Composable
private fun CompatBadge(c: Compat) {
    val (bg, fg) = when (c) {
        Compat.OFFICIAL -> Color(0xFFDDEFD6) to Color(0xFF1F5C3A)
        Compat.BEST_EFFORT -> Color(0xFFFFE9B0) to Color(0xFF6B4A00)
        Compat.STATEMENT -> Color(0xFFE3E7EE) to Color(0xFF2F3B52)
        Compat.NOT_SUPPORTED -> Color(0xFFF3D3D0) to Color(0xFF7A2A25)
    }
    Text(c.label, Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp), color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun PreviewLine(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)); Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
