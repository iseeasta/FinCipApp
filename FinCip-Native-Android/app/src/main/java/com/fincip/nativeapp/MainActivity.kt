package com.fincip.nativeapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import com.fincip.nativeapp.ui.Feedback
import com.fincip.nativeapp.ui.LockScreen
import com.fincip.nativeapp.data.NavDests
import com.fincip.nativeapp.data.AppIcons
import com.fincip.nativeapp.data.AppLock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import com.fincip.nativeapp.ui.Pop
import com.fincip.nativeapp.ui.FinCipInk
import com.fincip.nativeapp.ui.FinCipMark
import com.fincip.nativeapp.ui.FinCipSidebar
import com.fincip.nativeapp.ui.SidebarWidth
import com.fincip.nativeapp.ui.ProfileData
import com.fincip.nativeapp.ui.ProfileForm
import com.fincip.nativeapp.ui.OnboardingScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalConfiguration
import com.fincip.nativeapp.ui.PopField
import com.fincip.nativeapp.ui.PopChip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fincip.nativeapp.data.CURRENCY_INFO
import com.fincip.nativeapp.data.CryptoBackup
import com.fincip.nativeapp.data.FinanceStore
import com.fincip.nativeapp.data.NAV_PAGES
import com.fincip.nativeapp.data.SecurePrefs
import com.fincip.nativeapp.data.SupabaseApi
import com.fincip.nativeapp.data.arrayOrEmpty
import com.fincip.nativeapp.data.asDoubleOr
import com.fincip.nativeapp.data.asObjectOrNull
import com.fincip.nativeapp.data.asStringOr
import com.fincip.nativeapp.data.bool
import com.fincip.nativeapp.data.currentMonth
import com.fincip.nativeapp.data.deepCopyObject
import com.fincip.nativeapp.data.defaultWorkspace
import com.fincip.nativeapp.data.double
import com.fincip.nativeapp.data.expenseTotal
import com.fincip.nativeapp.data.fxRate
import com.fincip.nativeapp.data.incomeTotal
import com.fincip.nativeapp.data.isoToday
import com.fincip.nativeapp.data.mergeDefaults
import com.fincip.nativeapp.data.money
import com.fincip.nativeapp.data.normalizeTaxCountry
import com.fincip.nativeapp.data.monthlyTransactions
import com.fincip.nativeapp.data.nextDueLabel
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.objects
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.toBase
import com.fincip.nativeapp.data.transactions
import com.fincip.nativeapp.notifications.ReminderWorker
import com.fincip.nativeapp.ui.Field
import com.fincip.nativeapp.ui.FinCipTheme
import com.fincip.nativeapp.ui.MetricTile
import com.fincip.nativeapp.ui.NeoButton
import com.fincip.nativeapp.ui.NeoCard
import com.fincip.nativeapp.ui.NeoOutlineButton
import com.fincip.nativeapp.ui.NeoTitle
import com.fincip.nativeapp.ui.ProgressBar
import com.fincip.nativeapp.ui.Selector
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyPrivacyScreen(this, AppLock.privacyScreen(this))
        scheduleReminders()
        setContent { FinCipApp(FinanceStore(applicationContext), SecurePrefs(applicationContext)) }
    }

    private fun scheduleReminders() {
        val work = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("fincip-bill-reminders", ExistingPeriodicWorkPolicy.KEEP, work)
    }
}

@Composable
private fun FinCipApp(store: FinanceStore, securePrefs: SecurePrefs) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var workspace by remember { mutableStateOf(defaultWorkspace()) }
    var loading by remember { mutableStateOf(true) }
    var vibe by remember {
        val sp = context.getSharedPreferences("fincip_ui", 0)
        // One-time switch so existing installs land on the new look; the user can still change it in Settings.
        if (!sp.getBoolean("leaf_v1", false)) { sp.edit().putString("vibe", "Leaf").putBoolean("leaf_v1", true).apply() }
        if (sp.getString("vibe", "Leaf") == "Brutalist Lime") sp.edit().putString("vibe", "Brutalist").apply()
        mutableStateOf(sp.getString("vibe", "Leaf") ?: "Leaf")
    }
    var dark by remember { mutableStateOf(context.getSharedPreferences("fincip_ui", 0).getBoolean("dark", false)) }
    var page by remember { mutableStateOf("overview") }
    var group by remember { mutableStateOf("Home") }
    var sidebarExpanded by remember { mutableStateOf(context.resources.configuration.screenWidthDp >= 600 && context.getSharedPreferences("fincip_ui", 0).getBoolean("sidebar_expanded", true)) }
    var editingProfile by remember { mutableStateOf(false) }
    var accent by remember { mutableStateOf(context.getSharedPreferences("fincip_ui", 0).getString("accent_v2", "Default") ?: "Default") }
    var appIcon by remember { mutableStateOf(AppIcons.current(context)) }
    var soundOn by remember { mutableStateOf(context.getSharedPreferences("fincip_ui", 0).getBoolean("sfx", true)) }
    var hapticOn by remember { mutableStateOf(context.getSharedPreferences("fincip_ui", 0).getBoolean("haptic", true)) }
    Feedback.sound = soundOn; Feedback.haptic = hapticOn
    var locked by remember { mutableStateOf(AppLock.lockEnabled(context)) }
    fun applySecure(on: Boolean) { (context as? Activity)?.let { applyPrivacyScreen(it, on) } }
    androidx.compose.runtime.DisposableEffect(Unit) {
        applySecure(AppLock.privacyScreen(context))
        var stoppedAt = 0L
        val owner = (context as? androidx.activity.ComponentActivity)
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) stoppedAt = android.os.SystemClock.elapsedRealtime()
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START && stoppedAt > 0 && AppLock.lockEnabled(context) &&
                android.os.SystemClock.elapsedRealtime() - stoppedAt >= AppLock.timeoutSeconds(context) * 1000L) locked = true
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    var month by remember { mutableStateOf(currentMonth()) }
    var showEntry by remember { mutableStateOf(false) }
    var showBackupPassword by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var pendingBackup by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var statementImport by remember { mutableStateOf(false) }
    var transactionQuery by remember { mutableStateOf("") }
    var session by remember { mutableStateOf(securePrefs.readSupabase()) }
    var dialogType by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf("") }
    var formName by remember { mutableStateOf("") }
    var formAmount by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("Food") }
    var formDate by remember { mutableStateOf(isoToday()) }
    var formCurrency by remember { mutableStateOf("INR") }
    var formType by remember { mutableStateOf("expense") }
    var formTags by remember { mutableStateOf("") }
    var formPerson by remember { mutableStateOf("") }
    var formProject by remember { mutableStateOf("") }
    var formAccount by remember { mutableStateOf("UPI") }
    var formTarget by remember { mutableStateOf("") }
    var formSaved by remember { mutableStateOf("0") }
    var formContribution by remember { mutableStateOf("0") }
    var formDeadline by remember { mutableStateOf("") }
    var formFrequency by remember { mutableStateOf("monthly") }
    var formRate by remember { mutableStateOf("0") }
    var formExtra by remember { mutableStateOf("") }
    var formShared by remember { mutableStateOf(false) }
    var activeGoalId by remember { mutableStateOf("") }
    var activeProductId by remember { mutableStateOf("") }
    var taxCountry by remember { mutableStateOf("IN") }
    var taxYear by remember { mutableStateOf("2026–27") }
    var taxIncome by remember { mutableStateOf("") }
    var taxDeduction by remember { mutableStateOf("0") }
    var taxStandardDeduction by remember { mutableStateOf("75000") }
    var taxAllowance by remember { mutableStateOf("0") }
    var taxRate by remember { mutableStateOf("20") }
    var taxRegime by remember { mutableStateOf("new") }
    var taxAgeGroup by remember { mutableStateOf("below60") }
    var taxResident by remember { mutableStateOf(true) }
    var petName by remember { mutableStateOf("Mochi") }
    var petKind by remember { mutableStateOf("🐱") }
    var petAccent by remember { mutableStateOf("#D8FF3E") }
    var backupPasswordDialog by remember { mutableStateOf(false) }
    var importPasswordDialog by remember { mutableStateOf(false) }
    var cloudUrl by remember { mutableStateOf("") }
    var cloudAnon by remember { mutableStateOf("") }
    var cloudEmail by remember { mutableStateOf("") }
    var cloudPassword by remember { mutableStateOf("") }
    var householdName by remember { mutableStateOf("Our household") }
    var inviteCode by remember { mutableStateOf("") }
    var sharedBudgetCategory by remember { mutableStateOf("Groceries") }
    var sharedBudgetLimit by remember { mutableStateOf("") }
    var sharedBudgetCurrency by remember { mutableStateOf("INR") }
    var sharedBudgetList by remember { mutableStateOf("[]") }
    var sharedBusy by remember { mutableStateOf(false) }
    var quickEntryText by remember { mutableStateOf("") }
    var quickVoiceDialog by remember { mutableStateOf(false) }
    var fxRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val raw = withContext(Dispatchers.IO) { runCatching { store.loadWorkspace() }.getOrNull() }
        if (!raw.isNullOrBlank()) runCatching { workspace = mergeDefaults(JsonParser.parseString(raw).asJsonObject) }
        val savedCloud = securePrefs.readSupabase()
        cloudUrl = savedCloud.string("url")
        cloudAnon = savedCloud.string("anonKey")
        petName = workspace.objectOrEmpty("mascot").string("name", "Mochi")
        petKind = workspace.objectOrEmpty("mascot").string("kind", "🐱")
        petAccent = workspace.objectOrEmpty("mascot").string("accent", "#D8FF3E")
        val loadedTax = workspace.objectOrEmpty("taxProfile")
        taxCountry = normalizeTaxCountry(loadedTax.string("country", "IN"))
        taxYear = loadedTax.string("taxYear", if (taxCountry == "IN") "2026–27" else "2026")
        taxIncome = loadedTax.double("income").takeIf { it > 0 }?.toString().orEmpty()
        taxDeduction = loadedTax.double("deductions").toString()
        taxStandardDeduction = loadedTax.double("standardDeduction", 75000.0).toString()
        taxAllowance = loadedTax.double("allowance").toString()
        taxRate = loadedTax.double("regionalRate", 0.0).toString()
        taxRegime = loadedTax.string("regime", "new")
        taxAgeGroup = loadedTax.string("ageGroup", "below60")
        taxResident = loadedTax.bool("resident", true)
        loading = false
    }

    fun updateWorkspace(change: (JsonObject) -> Unit) {
        val next = workspace.deepCopyObject()
        change(next)
        workspace = next
        scope.launch(Dispatchers.IO) { runCatching { store.saveWorkspace(next.toString()) } }
    }
    fun updateProfile(block: JsonObject.() -> Unit) = updateWorkspace { root -> val p = root.objectOrEmpty("profile"); p.block(); root.add("profile", p) }
    fun updateMascot(block: JsonObject.() -> Unit) = updateWorkspace { root -> val p = root.objectOrEmpty("mascot"); p.block(); root.add("mascot", p) }

    fun replaceWorkspace(imported: JsonObject) {
        val next = mergeDefaults(imported)
        workspace = next
        scope.launch(Dispatchers.IO) { runCatching { store.saveWorkspace(next.toString()) } }
        petName = next.objectOrEmpty("mascot").string("name", "Mochi")
        petKind = next.objectOrEmpty("mascot").string("kind", "🐱")
        petAccent = next.objectOrEmpty("mascot").string("accent", "#D8FF3E")
        val importedTax = next.objectOrEmpty("taxProfile")
        taxCountry = normalizeTaxCountry(importedTax.string("country", "IN"))
        taxYear = importedTax.string("taxYear", if (taxCountry == "IN") "2026–27" else "2026")
        taxIncome = importedTax.double("income").takeIf { it > 0 }?.toString().orEmpty()
        taxDeduction = importedTax.double("deductions").toString()
        taxStandardDeduction = importedTax.double("standardDeduction", 75000.0).toString()
        taxAllowance = importedTax.double("allowance").toString()
        taxRate = importedTax.double("regionalRate", 0.0).toString()
        taxRegime = importedTax.string("regime", "new")
        taxAgeGroup = importedTax.string("ageGroup", "below60")
        taxResident = importedTax.bool("resident", true)
        val importedVibe = next.objectOrEmpty("settings").string("vibeTheme", "Material You")
        if (importedVibe in com.fincip.nativeapp.ui.Vibes.map { it.name } + listOf("Brutalist Lime", "Pastel Pop", "System")) {
            vibe = if (importedVibe == "System") "Material You" else importedVibe
            context.getSharedPreferences("fincip_ui", 0).edit().putString("vibe", vibe).apply()
        }
        val importedDark = next.objectOrEmpty("settings").string("theme", "light") == "dark"
        dark = importedDark
        context.getSharedPreferences("fincip_ui", 0).edit().putBoolean("dark", importedDark).apply()
    }

    fun notify(message: String) { statusMessage = message }
    fun refreshFxRates() {
        if (fxRefreshing) return
        fxRefreshing = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { com.fincip.nativeapp.data.FxRateApi.fetchInrRates(CURRENCY_INFO.map { it.code }.filter { it != "INR" }) } }
                .onSuccess { result ->
                    updateWorkspace { root ->
                        val settings = root.objectOrEmpty("settings")
                        val rates = settings.objectOrEmpty("fxRates")
                        rates.addProperty("INR", 1.0)
                        result.rates.forEach { (code, value) -> if (value > 0.0) rates.addProperty(code, value) }
                        settings.add("fxRates", rates)
                        settings.addProperty("fxUpdatedAt", result.date)
                        settings.addProperty("fxRateSource", "Frankfurter reference rates")
                        root.add("settings", settings)
                    }
                    notify("Reference rates updated · ${result.date}")
                }
                .onFailure { notify("Could not refresh FX rates: ${it.message ?: "check your internet connection"}") }
            fxRefreshing = false
        }
    }
    fun openForm(type: String, existing: JsonObject? = null) {
        dialogType = type
        editId = existing?.string("id").orEmpty()
        formName = existing?.string("title", existing.string("name", existing.string("asset"))).orEmpty()
        formAmount = existing?.double("amount", existing.double("limit", existing.double("value")))?.takeIf { it > 0 }?.toString().orEmpty()
        formCategory = existing?.string("category", when (type) { "investment" -> "Stock"; "asset" -> "Asset"; "product" -> "SIP"; "rule" -> "Food"; else -> "Food" }) ?: when (type) { "investment" -> "Stock"; "asset" -> "Asset"; "product" -> "SIP"; else -> "Food" }
        if (type == "goal" && existing == null) formCategory = "Other"
        formDate = existing?.string("date", existing.string("nextDate", isoToday())) ?: isoToday()
        formCurrency = existing?.string("currency", workspace.objectOrEmpty("settings").string("baseCurrency", "INR")) ?: workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
        formType = existing?.string("type", if (type == "transaction") "expense" else "savings") ?: if (type == "transaction") "expense" else "savings"
        formTags = existing?.get("tags")?.let { if (it.isJsonArray) it.asJsonArray.joinToString(",") { tag -> tag.asStringOr() } else it.asString } ?: ""
        formPerson = existing?.string("person").orEmpty()
        formProject = existing?.string("project").orEmpty()
        formAccount = existing?.string("account", "UPI") ?: "UPI"
        formTarget = existing?.double("target")?.takeIf { it > 0 }?.toString().orEmpty()
        formSaved = existing?.double("saved")?.toString() ?: "0"
        formContribution = existing?.double("monthlyContribution")?.toString() ?: "0"
        formDeadline = existing?.string("deadline").orEmpty()
        formFrequency = existing?.string("frequency", "monthly") ?: "monthly"
        formRate = existing?.double("interestRate")?.toString() ?: "0"
        formExtra = existing?.arrayOrEmpty("splits")?.objects()?.joinToString(", ") { split -> "${split.string("category")}:${split.double("amount")}" }.orEmpty()
        formShared = existing?.bool("shared") ?: false
        if (type == "product") {
            formCategory = existing?.string("kind", "SIP") ?: "SIP"
            formAmount = existing?.double("invested")?.toString().orEmpty()
            formSaved = existing?.double("currentValue")?.toString() ?: ""
            formPerson = existing?.string("provider").orEmpty()
            formDate = existing?.string("startDate", isoToday()) ?: isoToday()
            formDeadline = existing?.string("maturityDate").orEmpty()
            formContribution = existing?.double("monthlyContribution")?.toString() ?: ""
            formRate = existing?.double("rate")?.toString() ?: (com.fincip.nativeapp.data.IndiaKinds.of("SIP").defaultRate?.toString() ?: "")
        }
        if (type == "asset") {
            formCategory = existing?.string("type", "Asset") ?: "Asset"
            formPerson = existing?.string("group").orEmpty()
        }
        if (type == "emergency") {
            formName = "Emergency fund"
            formAmount = existing?.double("balance")?.toString() ?: "0"
            formTarget = existing?.double("targetMonths", 6.0)?.toString() ?: "6"
            formCurrency = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
        }
        if (type == "productContribution") {
            formName = existing?.string("name", "Investment contribution") ?: "Investment contribution"
            formCurrency = existing?.string("currency", "INR") ?: "INR"
        }
    }

    fun existingArrayForProduct(id: String): JsonArray {
        if (id.isBlank()) return JsonArray()
        return workspace.arrayOrEmpty("financeProducts").objects().firstOrNull { it.string("id") == id }?.arrayOrEmpty("contributions") ?: JsonArray()
    }

    fun addToArray(key: String, item: JsonObject, replaceId: String = "") {
        updateWorkspace { root ->
            val arr = root.arrayOrEmpty(key)
            if (replaceId.isNotBlank()) {
                val next = JsonArray()
                arr.forEach { element ->
                    val obj = element.asObjectOrNull()
                    if (obj == null || obj.string("id") != replaceId) next.add(element.deepCopy()) else next.add(item)
                }
                root.add(key, next)
            } else arr.add(item)
        }
    }

    fun saveForm() {
        val id = editId.ifBlank { java.util.UUID.randomUUID().toString() }
        val amount = formAmount.toDoubleOrNull() ?: 0.0
        val item = JsonObject().apply {
            addProperty("id", id); addProperty("title", formName.trim()); addProperty("name", formName.trim())
            addProperty("amount", amount); addProperty("currency", formCurrency); addProperty("category", formCategory)
            addProperty("date", formDate); addProperty("type", formType); addProperty("account", formAccount)
            addProperty("person", formPerson); addProperty("project", formProject)
            add("tags", JsonArray().apply { formTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(::add) })
            addProperty("target", formTarget.toDoubleOrNull() ?: 0.0); addProperty("saved", formSaved.toDoubleOrNull() ?: 0.0)
            addProperty("monthlyContribution", formContribution.toDoubleOrNull() ?: 0.0); addProperty("deadline", formDeadline)
            addProperty("nextDate", formDate); addProperty("frequency", formFrequency); addProperty("interestRate", formRate.toDoubleOrNull() ?: 0.0)
            addProperty("asset", formName.trim()); addProperty("units", 1.0); addProperty("buyPrice", amount); addProperty("currentPrice", amount)
            addProperty("source", formPerson); addProperty("shared", formShared)
        }
        when (dialogType) {
            "transaction" -> {
                if (amount <= 0 || formName.isBlank()) return
                val splits = JsonArray()
                val splitParts = formExtra.split(",").map { it.trim() }.filter { it.isNotBlank() }
                splitParts.forEach { part ->
                    val bits = part.split(":", limit = 2)
                    val splitAmount = bits.getOrNull(1)?.trim()?.toDoubleOrNull()
                    if (bits.size != 2 || bits[0].isBlank() || splitAmount == null || splitAmount <= 0) {
                        notify("Use split format Category:amount, and enter positive amounts")
                        return
                    }
                    splits.add(JsonObject().apply { addProperty("category", bits[0].trim()); addProperty("amount", splitAmount) })
                }
                if (splits.size() > 0) {
                    val allocated = splits.objects().sumOf { it.double("amount") }
                    if (kotlin.math.abs(allocated - amount) > 0.005) {
                        notify("Split allocations must add up to the full transaction amount")
                        return
                    }
                    item.add("splits", splits)
                }
                addToArray("transactions", item, editId)
                notify(if (editId.isBlank()) "Transaction added" else "Transaction updated")
            }
            "budget" -> {
                if (amount <= 0) return
                item.addProperty("limit", amount); addToArray("budgets", item, editId); notify("Budget saved")
            }
            "goal" -> {
                if (formName.isBlank() || formTarget.toDoubleOrNull().orZero() <= 0) return
                item.addProperty("target", formTarget.toDoubleOrNull() ?: 0.0); item.addProperty("saved", formSaved.toDoubleOrNull() ?: 0.0)
                item.addProperty("monthlyContribution", formContribution.toDoubleOrNull() ?: 0.0); addToArray("goals", item, editId); notify("Goal saved")
            }
            "contribution" -> {
                val addition = amount
                updateWorkspace { root ->
                    val next = JsonArray(); root.arrayOrEmpty("goals").forEach { el ->
                        val goal = el.asObjectOrNull()
                        if (goal?.string("id") == activeGoalId) { val copy = goal.deepCopyObject(); copy.addProperty("saved", copy.double("saved") + addition); next.add(copy) } else next.add(el.deepCopy())
                    }; root.add("goals", next)
                }; notify("Progress updated")
            }
            "bill" -> {
                if (formName.isBlank() || amount <= 0) return
                item.addProperty("nextDate", formDate); item.addProperty("frequency", formFrequency); item.addProperty("type", "expense")
                addToArray("recurringRules", item, editId); notify("Reminder saved")
            }
            "investment" -> {
                if (formName.isBlank() || amount <= 0) return
                item.addProperty("asset", formName); item.addProperty("type", formCategory); item.addProperty("units", formTarget.toDoubleOrNull() ?: 1.0)
                item.addProperty("buyPrice", amount); item.addProperty("currentPrice", formSaved.toDoubleOrNull() ?: amount)
                addToArray("investments", item, editId); notify("Holding saved")
            }
            "asset" -> {
                if (formName.isBlank() || amount <= 0) return
                item.addProperty("name", formName); item.addProperty("value", amount); item.addProperty("type", formCategory); item.addProperty("group", formPerson)
                addToArray("netWorthItems", item, editId); notify("Net worth item saved")
            }
            "product" -> {
                val invested = amount
                var currentValue = formSaved.toDoubleOrNull() ?: 0.0
                if (editId.isBlank() && currentValue == 0.0) currentValue = invested
                if (formName.isBlank() || invested < 0 || currentValue < 0 || invested + currentValue <= 0) { notify("Add a name and an invested amount or current value (numbers only, no negatives)"); return }
                val contributions = existingArrayForProduct(editId)
                item.addProperty("name", formName.trim())
                item.addProperty("kind", formCategory)
                item.addProperty("provider", formPerson.trim())
                item.addProperty("invested", invested)
                item.addProperty("currentValue", currentValue)
                item.addProperty("currency", formCurrency)
                item.addProperty("startDate", formDate.ifBlank { isoToday() })
                item.addProperty("rate", formRate.toDoubleOrNull() ?: 0.0)
                item.addProperty("monthlyContribution", formContribution.toDoubleOrNull() ?: 0.0)
                item.addProperty("maturityDate", formDeadline)
                item.add("contributions", if (contributions.size() > 0) contributions else JsonArray().apply { if (invested > 0) add(JsonObject().apply { addProperty("date", formDate.ifBlank { isoToday() }); addProperty("amount", invested) }) })
                addToArray("financeProducts", item, editId); notify("Finance product saved")
            }
            "productContribution" -> {
                if (amount <= 0 || activeProductId.isBlank()) return
                updateWorkspace { root ->
                    val next = JsonArray()
                    root.arrayOrEmpty("financeProducts").forEach { el ->
                        val product = el.asObjectOrNull()
                        if (product?.string("id") == activeProductId) {
                            val copy = product.deepCopyObject()
                            copy.addProperty("invested", copy.double("invested") + amount)
                            copy.addProperty("currentValue", copy.double("currentValue") + amount)
                            val flows = copy.arrayOrEmpty("contributions")
                            flows.add(JsonObject().apply { addProperty("date", isoToday()); addProperty("amount", amount) })
                            copy.add("contributions", flows)
                            next.add(copy)
                        } else next.add(el.deepCopy())
                    }
                    root.add("financeProducts", next)
                }
                notify("Contribution recorded. Update current value after your next statement.")
            }
            "emergency" -> {
                val fund = JsonObject().apply { addProperty("balance", amount.coerceAtLeast(0.0)); addProperty("targetMonths", (formTarget.toDoubleOrNull() ?: 6.0).coerceIn(1.0, 36.0)) }
                updateWorkspace { root -> root.add("emergencyFund", fund) }
                notify("Emergency fund updated")
            }
            "rule" -> {
                if (formName.isBlank()) return
                item.addProperty("keyword", formName); item.addProperty("category", formCategory); addToArray("smartRules", item, editId); notify("Rule saved")
            }
        }
        dialogType = ""
    }

    fun deleteFromArray(key: String, id: String) {
        updateWorkspace { root -> root.add(key, JsonArray().apply { root.arrayOrEmpty(key).forEach { el -> if (el.asObjectOrNull()?.string("id") != id) add(el.deepCopy()) } }) }
        notify("Item deleted")
    }

    fun markRecurringPaid(rule: JsonObject) {
        val dueDate = runCatching { LocalDate.parse(rule.string("nextDate")) }.getOrElse { LocalDate.now() }
        val tx = JsonObject().apply {
            addProperty("id", java.util.UUID.randomUUID().toString())
            addProperty("title", rule.string("title", "Recurring payment"))
            addProperty("amount", rule.double("amount")); addProperty("currency", rule.string("currency", "INR"))
            addProperty("type", rule.string("type", "expense")); addProperty("category", rule.string("category", "Bills"))
            addProperty("date", dueDate.toString()); addProperty("account", "Recurring payment")
            addProperty("note", "Marked paid from recurring schedule"); add("tags", JsonArray().apply { add("recurring") })
        }
        val fingerprint = transactionFingerprint(tx)
        val alreadyExists = transactions(workspace).any { transactionFingerprint(it) == fingerprint }
        updateWorkspace { root ->
            if (!alreadyExists) root.add("transactions", root.arrayOrEmpty("transactions").apply { add(tx) })
            val updated = JsonArray()
            root.arrayOrEmpty("recurringRules").forEach { el ->
                val item = el.asObjectOrNull()
                if (item?.string("id") == rule.string("id")) {
                    val copy = item.deepCopyObject()
                    val frequency = copy.string("frequency", "monthly")
                    val anchor = copy.double("anchorDay", dueDate.dayOfMonth.toDouble()).toInt().coerceIn(1, 31)
                    val next = when (frequency) {
                        "weekly" -> dueDate.plusWeeks(1)
                        "yearly" -> dueDate.plusYears(1)
                        else -> {
                            val ym = YearMonth.from(dueDate).plusMonths(1)
                            ym.atDay(minOf(anchor, ym.lengthOfMonth()))
                        }
                    }
                    copy.addProperty("anchorDay", anchor); copy.addProperty("nextDate", next.toString()); copy.addProperty("lastPaidDate", dueDate.toString())
                    updated.add(copy)
                } else updated.add(el.deepCopy())
            }
            root.add("recurringRules", updated)
        }
        notify(if (alreadyExists) "Payment already exists; next due date advanced" else "Payment recorded and next date advanced")
    }

    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
            if (text != null) {
                runCatching {
                    val parsed = JsonParser.parseString(text).asJsonObject
                    if (parsed.string("format") == "fincip-encrypted-backup") { pendingBackup = text; importPasswordDialog = true }
                    else if (parsed.string("type") == "fincip-shared-budget") {
                        require(parsed.has("budgets") && parsed.get("budgets").isJsonArray) { "This shared budget pack is incomplete." }
                        val next = workspace.deepCopyObject()
                        val incomingBudgets = parsed.arrayOrEmpty("budgets").objects()
                        val mergedBudgets = JsonArray()
                        val incomingCategories = incomingBudgets.map { it.string("category").lowercase(Locale.ROOT) }.toSet()
                        next.arrayOrEmpty("budgets").forEach { row ->
                            val budget = row.asObjectOrNull()
                            if (budget == null || budget.string("category").lowercase(Locale.ROOT) !in incomingCategories) mergedBudgets.add(row.deepCopy())
                        }
                        incomingBudgets.forEach { mergedBudgets.add(it.deepCopy()) }
                        next.add("budgets", mergedBudgets)
                        val txRows = next.arrayOrEmpty("transactions")
                        val seenIds = txRows.mapNotNull { it.asObjectOrNull()?.string("id")?.takeIf(String::isNotBlank) }.toMutableSet()
                        val seenFingerprints = txRows.mapNotNull { it.asObjectOrNull()?.let(::transactionFingerprint) }.toMutableSet()
                        parsed.arrayOrEmpty("transactions").forEach { incoming ->
                            val row = incoming.asObjectOrNull()?.deepCopyObject() ?: return@forEach
                            val fingerprint = transactionFingerprint(row)
                            if ((row.string("id").isBlank() || seenIds.add(row.string("id"))) && seenFingerprints.add(fingerprint)) {
                                row.addProperty("shared", true); txRows.add(row)
                            }
                        }
                        next.add("transactions", txRows)
                        replaceWorkspace(next)
                        notify("Shared budget pack merged into this device")
                    } else {
                        val root = if (parsed.has("workspace")) parsed.getAsJsonObject("workspace") else if (parsed.has("data") && parsed.get("data").isJsonObject) parsed.getAsJsonObject("data") else parsed
                        replaceWorkspace(root)
                        notify("FinCip backup restored")
                    }
                }.onFailure { notify("Could not read backup: ${it.message}") }
            }
        }
    }
    fun importLegacyText(content: String): Boolean {
        val imported = parseStatement(content)
        if (imported.isEmpty()) return false
        val existingKeys = transactions(workspace).map(::transactionFingerprint).toSet()
        val unique = imported.filter { transactionFingerprint(it) !in existingKeys }.distinctBy(::transactionFingerprint)
        updateWorkspace { root -> val arr = root.arrayOrEmpty("transactions"); unique.forEach { arr.add(it) }; root.add("transactions", arr) }
        notify("Imported ${unique.size} entries · skipped ${imported.size - unique.size} duplicates")
        return true
    }
    fun importBatch(rows: List<JsonObject>, record: JsonObject): Boolean = try {
        // All-or-nothing: updateWorkspace builds a copy, applies the change, and only then swaps it in and saves.
        updateWorkspace { root ->
            val arr = root.arrayOrEmpty("transactions"); rows.forEach { arr.add(it) }; root.add("transactions", arr)
            val log = root.arrayOrEmpty("importBatches"); log.add(record); root.add("importBatches", log)
        }
        true
    } catch (_: Throwable) { false }
    fun undoBatch(id: String) = updateWorkspace { root ->
        val kept = JsonArray(); root.arrayOrEmpty("transactions").forEach { el -> if (el.asObjectOrNull()?.string("importId") != id) kept.add(el) }
        root.add("transactions", kept)
    }
    val statementLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val content = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
            if (content != null && !importLegacyText(content)) notify("No transactions found. Check the CSV or OFX file format.")
        }
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray()) }; withContext(Dispatchers.Main) { notify("Backup exported") } }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(exportCsv(workspace).toByteArray()) }; withContext(Dispatchers.Main) { notify("CSV exported") } }
    }
    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) { runCatching { writePdfReport(context, uri, workspace, month) }.onFailure { withContext(Dispatchers.Main) { notify("PDF export failed: ${it.message}") } }; withContext(Dispatchers.Main) { notify("PDF report saved") } }
    }
    val receiptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
                    val out = File(dir, "receipt_${System.currentTimeMillis()}")
                    context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { output -> input.copyTo(output) } }
                        ?: throw IllegalArgumentException("The selected file could not be read.")
                    if (!out.isFile || out.length() > 15L * 1024L * 1024L) { out.delete(); throw IllegalArgumentException("Receipt files must be smaller than 15 MB.") }
                    out
                }
                val mime = context.contentResolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
                val displayName = runCatching { context.contentResolver.query(uri, null, null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)) else null } }.getOrNull().orEmpty().ifBlank { file.name }
                val receipt = JsonObject().apply { addProperty("id", file.name); addProperty("title", displayName); addProperty("fileName", displayName); addProperty("mime", mime); addProperty("size", file.length()); addProperty("note", "Saved locally"); addProperty("filePath", file.absolutePath); addProperty("createdAt", isoToday()) }
                updateWorkspace { root -> val arr = root.arrayOrEmpty("receipts"); arr.add(receipt); root.add("receipts", arr) }
                notify("Receipt saved privately on this device")
            }.onFailure { notify(it.message ?: "Could not save this receipt") }
        }
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val words = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            quickEntryText = words; quickVoiceDialog = true
        }
    }
    fun saveProfile(d: ProfileData) {
        updateWorkspace { root -> root.add("profile", JsonObject().apply { addProperty("name", d.name); addProperty("handle", d.handle); addProperty("tagline", d.tagline); addProperty("avatarSeed", d.seed); addProperty("joined", java.time.LocalDate.now().toString()) }) }
    }
    fun startVoice() { runCatching { voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your money move")) }.onFailure { notify("Voice input is not available on this device") } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }

    FinCipTheme(vibe, dark, accent) {
        val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
        val bgArgb = MaterialTheme.colorScheme.background.toArgb()
        SideEffect {
            (context as? Activity)?.window?.let { w ->
                val controller = androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
                controller.isAppearanceLightStatusBars = lightBars
                controller.isAppearanceLightNavigationBars = lightBars
            }
            (context as? Activity)?.let { act ->
                val bg = bgArgb
                @Suppress("DEPRECATION") act.setTaskDescription(android.app.ActivityManager.TaskDescription("FinCip", null, bg))
            }
        }
        if (locked) {
            LockScreen(onUnlocked = { locked = false })
        } else if (loading) {
            Box(Modifier.fillMaxSize().background(Color(0xFFF6F5EF)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FinCipMark(size = 96.dp, glyph = FinCipInk)
                    Text("FinCip", color = FinCipInk, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-1).sp)
                    Text("OPENING YOUR MONEY SPACE", color = FinCipInk.copy(alpha = .55f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.6.sp)
                }
            }
        } else if (workspace.objectOrEmpty("profile").string("name").isBlank()) {
            OnboardingScreen { saveProfile(it) }
        } else {
            val privacy = workspace.objectOrEmpty("settings").bool("privacyMode")
            val monthIncome = incomeTotal(workspace, month)
            val monthExpenses = expenseTotal(workspace, month)
            val balance = com.fincip.nativeapp.data.accountBalance(workspace)
            val savingsRate = if (monthIncome > 0) (monthIncome - monthExpenses) / monthIncome * 100 else 0.0
            val wide = LocalConfiguration.current.screenWidthDp >= 600
            val navPrefs = remember(workspace) { com.fincip.nativeapp.data.NavPrefs.fromJson(workspace.objectOrEmpty("settings").get("navPrefs")?.takeIf { it.isJsonObject }?.asJsonObject) }
            val navState = com.fincip.nativeapp.ui.rememberNavState()
            androidx.compose.runtime.LaunchedEffect(navPrefs.isDefaultDrawer) {
                sidebarExpanded = if (navPrefs.isDefaultDrawer) (wide && context.getSharedPreferences("fincip_ui", 0).getBoolean("sidebar_expanded", true)) else false
                navState.closeAll()
            }
            fun navGo(id: String) {
                navState.closeAll()
                NavDests.all.firstOrNull { it.id == id }?.let { group = it.group }
                page = id
            }
            val sbW = if (wide) SidebarWidth else minOf(SidebarWidth, (LocalConfiguration.current.screenWidthDp * 0.78f).dp)
            val pushW by animateDpAsState(if (sidebarExpanded) sbW else 0.dp, tween(300, easing = FastOutSlowInEasing), label = "pushW")
            val sidebarX by animateDpAsState(if (sidebarExpanded) 0.dp else -sbW - 24.dp, tween(300, easing = FastOutSlowInEasing), label = "sidebarX")
            val corner by animateDpAsState(if (sidebarExpanded && !wide) 28.dp else 0.dp, tween(300), label = "corner")
            val isPersonal = page.substringBefore(":") in setOf("profile", "settings", "security", "customize")
            fun toggleSidebar() { sidebarExpanded = !sidebarExpanded; if (wide && navPrefs.isDefaultDrawer) context.getSharedPreferences("fincip_ui", 0).edit().putBoolean("sidebar_expanded", sidebarExpanded).apply() }
            // ---- Back navigation: history stack built from page changes (screens set `page` directly in many places)
            val pageStack = remember { androidx.compose.runtime.mutableStateListOf<String>() }
            var lastPage by remember { mutableStateOf(page) }
            var poppingBack by remember { mutableStateOf(false) }
            var lastBackAt by remember { mutableStateOf(0L) }
            androidx.compose.runtime.LaunchedEffect(page) {
                if (poppingBack) poppingBack = false
                else if (page != lastPage) {
                    pageStack.removeAll { it == lastPage || it == page }
                    pageStack.add(lastPage)
                    while (pageStack.size > 30) pageStack.removeAt(0)
                }
                lastPage = page
            }
            // Root exit: second Back within 2 seconds closes the app
            androidx.activity.compose.BackHandler(enabled = page == "overview" && pageStack.isEmpty()) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastBackAt < 2000) (context as? Activity)?.finish() else { lastBackAt = now; android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show() }
            }
            androidx.activity.compose.BackHandler(enabled = page != "overview" || pageStack.isNotEmpty()) {
                val prev = pageStack.removeLastOrNull() ?: "overview"
                poppingBack = true
                NAV_PAGES.firstOrNull { it.id == prev.substringBefore(":") }?.let { group = it.group }
                page = prev
            }
            androidx.activity.compose.BackHandler(enabled = !wide && sidebarExpanded) { sidebarExpanded = false }
            // any open menu (sheet, flyout, fan, search, full-screen menu) closes before the page changes
            androidx.activity.compose.BackHandler(enabled = navState.anyOpen) { navState.closeAll() }
            val glowA = MaterialTheme.colorScheme.primary
            val glowB = MaterialTheme.colorScheme.tertiary
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(glowA.copy(alpha = .38f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(120f, 300f), radius = 900f)).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(glowB.copy(alpha = .30f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(200f, 2200f), radius = 1000f))) {
              Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.width(if (wide) pushW else 0.dp))
                com.fincip.nativeapp.ui.NavSide(navPrefs, page, ::navGo, navState, Modifier.statusBarsPadding().navigationBarsPadding())
                Box(Modifier.weight(1f).then(if (!wide) Modifier.offset(x = pushW).clip(RoundedCornerShape(topStart = corner, bottomStart = corner)) else Modifier)) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                  Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (navPrefs.layout == "drawer" || navPrefs.layout == "overlay") IconButton(modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape), onClick = { if (navPrefs.isDefaultDrawer) toggleSidebar() else { val o = navState.menuOpen; navState.closeAll(); navState.menuOpen = !o } }) { Icon(androidx.compose.material.icons.Icons.Default.Menu, contentDescription = "Open menu") }
                        if (navPrefs.isDefaultDrawer) FinCipMark(Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = "Show sidebar") { toggleSidebar() }, size = 34.dp)
                        else FinCipMark(Modifier.clip(RoundedCornerShape(10.dp)), size = 34.dp)
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(when (page.substringBefore(":")) { "profile" -> "Profile"; "settings" -> "Settings"; "security" -> "Privacy & security"; "customize" -> "Customization"; else -> null } ?: (NAV_PAGES.firstOrNull { it.id == page }?.title ?: "FinCip"), fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.5).sp, maxLines = 1)
                            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)).uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f), maxLines = 1)
                        }
                        if (!navPrefs.isDefaultDrawer) IconButton(modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape), onClick = { startVoice() }) { Icon(androidx.compose.material.icons.Icons.Default.Mic, contentDescription = "Voice entry", modifier = Modifier.size(20.dp)) }
                        IconButton(modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape), onClick = { updateWorkspace { root -> val settings = root.objectOrEmpty("settings"); settings.addProperty("privacyMode", !settings.bool("privacyMode")); root.add("settings", settings) } }) { Icon(if (privacy) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle privacy", modifier = Modifier.size(20.dp)) }
                    }
                    com.fincip.nativeapp.ui.NavTop(navPrefs, page, ::navGo, navState)
                    if (!isPersonal) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MonthMenu(month, transactions(workspace).map { it.string("date").take(7) }.toSet()) { month = it }
                    }
                  }
                }
                ,
                bottomBar = {
                    com.fincip.nativeapp.ui.NavBottom(navPrefs, page, ::navGo, navState, Modifier.navigationBarsPadding())
                }
            ) { innerPadding ->
                androidx.compose.runtime.CompositionLocalProvider(com.fincip.nativeapp.ui.LocalNavInset provides (if (navPrefs.layout == "dock" || navPrefs.layout == "pillMenu") (navPrefs.height + 40).dp else 0.dp)) {
                Column(Modifier.fillMaxSize().padding(innerPadding).then(if (navPrefs.region == "bottom") Modifier else Modifier.navigationBarsPadding())) {
                    AnimatedContent(targetState = page, transitionSpec = { (fadeIn(tween(240, delayMillis = 70)) + slideInVertically(tween(280)) { it / 18 }) togetherWith fadeOut(tween(120)) }, label = "page") { currentPage ->
                    when (currentPage.substringBefore(":")) {
                        "overview" -> OverviewScreen(workspace, month, privacy, savingsRate, balance, onAdd = { openForm("transaction") }, onMascot = { page = "customize" }, onOpen = { id -> NAV_PAGES.firstOrNull { it.id == id }?.let { group = it.group; page = it.id } }, onNewGoal = { openForm("goal") }, onEditTx = { openForm("transaction", it) }, onWidgets = { ids -> updateWorkspace { root -> val st = root.objectOrEmpty("settings"); st.add("homeWidgets", com.google.gson.JsonArray().also { a -> ids.forEach { a.add(it) } }); root.add("settings", st) } })
                        "dataio" -> com.fincip.nativeapp.ui.DataIOScreen(workspace, ::importBatch, ::undoBatch, { m -> month = m; page = "transactions" }, { pendingExport = workspace.toString(); exportJsonLauncher.launch("FinCip-backup.json") }, ::importLegacyText, ::notify, { v -> updateWorkspace { root -> val st = root.objectOrEmpty("settings"); st.addProperty("openingBalance", v); root.add("settings", st) }; android.widget.Toast.makeText(this@MainActivity, "Starting balance saved", android.widget.Toast.LENGTH_SHORT).show() })
                        "transactions" -> TransactionsScreen(workspace, month, privacy, transactionQuery, { transactionQuery = it }, { openForm("transaction") }, { openForm("transaction", it) }, { deleteFromArray("transactions", it.string("id")) }, { page = "dataio" }, { page = "dataio" })
                        "recurring" -> RecurringScreen(workspace, privacy, { openForm("bill") }, { deleteFromArray("recurringRules", it.string("id")) }, { markRecurringPaid(it) })
                        "calendar" -> CalendarScreen(workspace, month, { month = it })
                        "subscriptions" -> SubscriptionsScreen(workspace, privacy, { openForm("bill") })
                        "currency" -> CurrencyScreen(workspace, { change -> updateWorkspace(change) }, { refreshFxRates() }, fxRefreshing)
                        "budgets" -> BudgetsScreen(workspace, month, privacy, { openForm("budget") }, { deleteFromArray("budgets", it.string("id")) }, {
                            val pack = JsonObject().apply {
                                addProperty("app", "FinCip")
                                addProperty("type", "fincip-shared-budget")
                                addProperty("createdAt", java.time.Instant.now().toString())
                                add("budgets", workspace.arrayOrEmpty("budgets").deepCopy())
                                add("transactions", JsonArray().apply { workspace.arrayOrEmpty("transactions").forEach { row -> if (row.asObjectOrNull()?.bool("shared") == true) add(row.deepCopy()) } })
                            }
                            pendingExport = pack.toString(); exportJsonLauncher.launch("fincip-shared-budget-${isoToday()}.json")
                        }, { backupImportLauncher.launch(arrayOf("application/json", "text/*")) })
                        "goals" -> GoalsScreen(workspace, privacy, { openForm("goal") }, { goal -> activeGoalId = goal.string("id"); openForm("contribution"); formName = goal.string("name") }, { deleteFromArray("goals", it.string("id")) }, { openForm("emergency", workspace.objectOrEmpty("emergencyFund")) }, { openForm("goal", it) })
                        "networth" -> NetWorthScreen(workspace, privacy, { openForm("asset") }, { deleteFromArray("netWorthItems", it.string("id")) }, { openForm("asset", it) })
                        "simulator" -> SimulatorScreen(workspace)
                        "india" -> IndiaFinanceScreen(workspace, privacy, { openForm("product") }, { deleteFromArray("financeProducts", it.string("id")) }, { product -> activeProductId = product.string("id"); openForm("productContribution", product) }, { product -> openForm("product", product) })
                        "tax" -> TaxScreen(
                            country = taxCountry, onCountry = { code -> taxCountry = code; if (code == "IN") taxYear = "2026–27" else taxYear = "2026" },
                            taxYear = taxYear, onTaxYear = { taxYear = it }, income = taxIncome, onIncome = { taxIncome = it },
                            deductions = taxDeduction, onDeductions = { taxDeduction = it }, standardDeduction = taxStandardDeduction,
                            onStandardDeduction = { taxStandardDeduction = it }, allowance = taxAllowance, onAllowance = { taxAllowance = it },
                            rate = taxRate, onRate = { taxRate = it }, regime = taxRegime, onRegime = { taxRegime = it },
                            ageGroup = taxAgeGroup, onAgeGroup = { taxAgeGroup = it }, resident = taxResident, onResident = { taxResident = it },
                            onSave = {
                                updateWorkspace { root ->
                                    val tax = root.objectOrEmpty("taxProfile")
                                    tax.addProperty("country", taxCountry); tax.addProperty("currency", com.fincip.nativeapp.data.taxCurrency(taxCountry)); tax.addProperty("taxYear", taxYear)
                                    tax.addProperty("mode", "country"); tax.addProperty("income", taxIncome.toDoubleOrNull() ?: 0.0)
                                    tax.addProperty("deductions", taxDeduction.toDoubleOrNull() ?: 0.0); tax.addProperty("standardDeduction", taxStandardDeduction.toDoubleOrNull() ?: 0.0)
                                    tax.addProperty("allowance", taxAllowance.toDoubleOrNull() ?: 0.0); tax.addProperty("regionalRate", taxRate.toDoubleOrNull() ?: 0.0)
                                    tax.addProperty("regime", taxRegime); tax.addProperty("ageGroup", taxAgeGroup); tax.addProperty("resident", taxResident)
                                    root.add("taxProfile", tax)
                                }
                                notify("Tax assumptions saved")
                            },
                            workspace = workspace
                        )
                        "portfolio" -> PortfolioScreen(workspace, privacy, { openForm("investment") }, { deleteFromArray("investments", it.string("id")) })
                        "forecast" -> ForecastScreen(workspace, privacy)
                        "insights" -> InsightsScreen(workspace, month, privacy)
                        "world" -> com.fincip.nativeapp.world.WorldScreen(workspace) { block -> updateWorkspace { root -> val w = root.objectOrEmpty("world"); w.block(); root.add("world", w) } }
                        "rules" -> RulesScreen(workspace, { openForm("rule") }, {
                            updateWorkspace { root ->
                                val rules = root.arrayOrEmpty("smartRules").objects()
                                val next = JsonArray()
                                root.arrayOrEmpty("transactions").forEach { el ->
                                    val tr = el.asObjectOrNull()?.deepCopyObject()
                                    if (tr != null) {
                                        val hit = rules.firstOrNull { r -> tr.string("title").contains(r.string("keyword"), true) }
                                        if (hit != null) tr.addProperty("category", hit.string("category"))
                                        next.add(tr)
                                    } else next.add(el.deepCopy())
                                }; root.add("transactions", next)
                            }; notify("Smart rules applied")
                        }, { deleteFromArray("smartRules", it.string("id")) })
                        "receipts" -> ReceiptScreen(workspace, { receiptLauncher.launch("*/*") }, { receipt ->
                            val file = File(receipt.string("filePath"))
                            runCatching {
                                if (file.isFile) {
                                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val openIntent = Intent(Intent.ACTION_VIEW).setDataAndType(contentUri, receipt.string("mime", "application/octet-stream")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(openIntent)
                                } else notify("Receipt file is not on this device. Restore it from an encrypted backup.")
                            }.onFailure { notify("No app can open this receipt type: ${it.message}") }
                        }, { receipt ->
                            val file = File(receipt.string("filePath")); if (file.exists()) file.delete(); deleteFromArray("receipts", receipt.string("id"))
                        })
                        "household" -> HouseholdScreen(
                            cloudUrl, { cloudUrl = it }, cloudAnon, { cloudAnon = it }, cloudEmail, { cloudEmail = it }, cloudPassword, { cloudPassword = it },
                            session, sharedBudgetList, sharedBudgetCategory, { sharedBudgetCategory = it }, sharedBudgetLimit, { sharedBudgetLimit = it }, sharedBudgetCurrency, { sharedBudgetCurrency = it }, householdName, { householdName = it }, inviteCode, { inviteCode = it }, sharedBusy,
                            onSignIn = {
                                sharedBusy = true
                                scope.launch {
                                    runCatching {
                                        val s = withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).signIn(cloudEmail, cloudPassword) }
                                        val old = securePrefs.readSupabase().string("householdId")
                                        securePrefs.saveSupabase(cloudUrl, cloudAnon, s.accessToken, s.userId, old)
                                        session = securePrefs.readSupabase(); notify("Supabase account connected")
                                    }.onFailure { notify(it.message ?: "Sign in failed") }
                                    sharedBusy = false
                                }
                            },
                            onPush = {
                                sharedBusy = true; scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).pushWorkspace(session.toSession(), workspace) } }.onSuccess { notify("Workspace synced to your Supabase project") }.onFailure { notify(it.message ?: "Cloud sync failed") }; sharedBusy = false
                                }
                            },
                            onPull = {
                                sharedBusy = true; scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).pullWorkspace(session.toSession()) } }.onSuccess { remote -> if (remote != null) { replaceWorkspace(remote); notify("Cloud workspace restored") } else notify("No cloud backup found") }.onFailure { notify(it.message ?: "Cloud pull failed") }; sharedBusy = false
                                }
                            },
                            onCreateHousehold = {
                                sharedBusy = true; scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).createHousehold(session.toSession(), householdName) } }.onSuccess { result ->
                                        val id = result.string("id"); val old = securePrefs.readSupabase(); securePrefs.saveSupabase(cloudUrl, cloudAnon, old.string("accessToken"), old.string("userId"), id); session = securePrefs.readSupabase(); notify("Household created · invite ${result.string("invite_code")}")
                                    }.onFailure { notify(it.message ?: "Could not create household") }; sharedBusy = false
                                }
                            },
                            onJoinHousehold = {
                                sharedBusy = true; scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).joinHousehold(session.toSession(), inviteCode) } }.onSuccess { id -> val old = securePrefs.readSupabase(); securePrefs.saveSupabase(cloudUrl, cloudAnon, old.string("accessToken"), old.string("userId"), id); session = securePrefs.readSupabase(); notify("Joined household") }.onFailure { notify(it.message ?: "Could not join") }; sharedBusy = false
                                }
                            },
                            onRefreshShared = {
                                sharedBusy = true; scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).sharedBudgets(session.toSession(), session.string("householdId")) } }.onSuccess { sharedBudgetList = it; notify("Shared budgets refreshed") }.onFailure { notify(it.message ?: "Refresh failed") }; sharedBusy = false
                                }
                            },
                            onAddSharedBudget = {
                                sharedBusy = true; scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).addSharedBudget(session.toSession(), session.string("householdId"), sharedBudgetCategory, sharedBudgetLimit.toDouble(), sharedBudgetCurrency) } }.onSuccess { notify("Shared budget saved"); sharedBudgetList = withContext(Dispatchers.IO) { SupabaseApi(cloudUrl, cloudAnon).sharedBudgets(session.toSession(), session.string("householdId")) } }.onFailure { notify(it.message ?: "Shared budget failed") }; sharedBusy = false
                                }
                            }, onSignOut = { securePrefs.clearSession(); session = securePrefs.readSupabase(); notify("Signed out") },
                            workspace = workspace
                        )
"profile", "settings", "security", "customize" -> ProfileHub(
                            workspace, initialTab = when (currentPage.substringBefore(":")) { "settings" -> 1; "security" -> 2; "customize" -> 3; else -> 0 },
                            onNavigate = { target -> page = target }, settingsStart = currentPage.substringAfter(":", ""),
                            onProfileChange = { block -> updateProfile(block) }, onPetChange = { block -> updateMascot(block) },
                            accent = accent, onAccent = { accent = it; context.getSharedPreferences("fincip_ui", 0).edit().putString("accent_v2", it).apply() },
                            appIcon = appIcon, onAppIcon = { appIcon = it; AppIcons.apply(context, it) },
                            onBlockScreens = { applySecure(it) },
                            vibe = vibe, onVibe = { vibe = it; context.getSharedPreferences("fincip_ui", 0).edit().putString("vibe", it).apply() },
                            dark = dark, onDark = { dark = it; context.getSharedPreferences("fincip_ui", 0).edit().putBoolean("dark", it).apply() },
                            soundOn = soundOn,
                            navContent = {
                                com.fincip.nativeapp.ui.NavStudioContent(navPrefs, { p ->
                                    updateWorkspace { root -> val st = root.objectOrEmpty("settings"); st.add("navPrefs", p.toJson()); root.add("settings", st) }
                                    android.widget.Toast.makeText(context, "Navigation saved", android.widget.Toast.LENGTH_SHORT).show()
                                }, {
                                    updateWorkspace { root -> val st = root.objectOrEmpty("settings"); st.remove("navPrefs"); root.add("settings", st) }
                                    android.widget.Toast.makeText(context, "Navigation reset to default", android.widget.Toast.LENGTH_SHORT).show()
                                })
                            },
                            settingsContent = { section ->
                                SettingsScreen(
                            workspace, vibe, { vibe = it; context.getSharedPreferences("fincip_ui", 0).edit().putString("vibe", it).apply() },
                            dark, { dark = it; context.getSharedPreferences("fincip_ui", 0).edit().putBoolean("dark", it).apply() },
                            { code -> updateWorkspace { root -> val settings = root.objectOrEmpty("settings"); settings.addProperty("baseCurrency", code); root.add("settings", settings) } },
                            { exportCsvLauncher.launch("FinCip-transactions.csv") }, { exportPdfLauncher.launch("FinCip-report.pdf") },
                            dashboardOrder = workspace.objectOrEmpty("settings").arrayOrEmpty("dashboardOrder").map { it.asStringOr() }.ifEmpty { listOf("balance", "income", "expenses", "savings") },
                            onDashboardOrder = { order -> updateWorkspace { root -> val settings = root.objectOrEmpty("settings"); settings.add("dashboardOrder", JsonArray().apply { order.forEach(::add) }); root.add("settings", settings) } },
                            onPrivacyDefault = { on -> updateWorkspace { root -> val settings = root.objectOrEmpty("settings"); settings.addProperty("privacyMode", on); root.add("settings", settings) } },
                            sound = soundOn, onSound = { soundOn = it; Feedback.sound = it; context.getSharedPreferences("fincip_ui", 0).edit().putBoolean("sfx", it).apply() },
                            haptic = hapticOn, onHaptic = { hapticOn = it; Feedback.haptic = it; context.getSharedPreferences("fincip_ui", 0).edit().putBoolean("haptic", it).apply() },
                                    section = section
                                )
                            },
                            securityContent = {
                                SecurityScreen(workspace, { updateWorkspace { root -> val settings = root.objectOrEmpty("settings"); settings.addProperty("privacyMode", it); root.add("settings", settings) } }, { backupImportLauncher.launch(arrayOf("application/json", "text/*")) }, { backupPasswordDialog = true }, {
                            val pack = JsonObject().apply { addProperty("app", "FinCip"); addProperty("version", 5); addProperty("exportedAt", isoToday()); add("data", workspace.deepCopyObject()) }
                            pendingExport = pack.toString(); exportJsonLauncher.launch("FinCip-backup.json")
                        })
                            }
                        )
                        else -> OverviewScreen(workspace, month, privacy, savingsRate, balance, onAdd = { openForm("transaction") }, onMascot = { page = "customize" })
                    }
                    }
                }
                }
            }
                if (!wide && sidebarExpanded) Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { sidebarExpanded = false })
                }
              }
              if (navPrefs.isDefaultDrawer) FinCipSidebar(
                  pages = NAV_PAGES, selectedPage = page, profile = profileOf(workspace), dark = dark,
                  onToggle = { toggleSidebar() },
                  onProfile = { page = "profile"; if (!wide || !navPrefs.isDefaultDrawer) sidebarExpanded = false },
                  onSettings = { page = "settings"; if (!wide || !navPrefs.isDefaultDrawer) sidebarExpanded = false },
                  width = sbW,
                  onSelect = { target -> group = target.group; page = target.id; if (!wide || !navPrefs.isDefaultDrawer) sidebarExpanded = false },
                  onVoice = { if (!wide || !navPrefs.isDefaultDrawer) sidebarExpanded = false; startVoice() },
                  onTheme = { dark = !dark; context.getSharedPreferences("fincip_ui", 0).edit().putBoolean("dark", dark).apply() },
                  modifier = Modifier.align(Alignment.TopStart).offset(x = sidebarX)
              )
              com.fincip.nativeapp.ui.NavOverlay(navPrefs, page, ::navGo, navState, Modifier.fillMaxSize())
            }
        }
    }

    if (editingProfile) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { editingProfile = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            FinCipTheme(vibe, dark, accent) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                    ProfileForm(profileOf(workspace), "Save profile", { saveProfile(it); editingProfile = false }, onCancel = { editingProfile = false })
                }
            }
        }
    }

    if (dialogType.isNotBlank()) {
        FinanceEntryDialog(
            type = dialogType, name = formName, onName = { formName = it }, amount = formAmount, onAmount = { formAmount = it },
            category = formCategory, onCategory = { formCategory = it }, date = formDate, onDate = { formDate = it },
            currency = formCurrency, onCurrency = { formCurrency = it }, transactionType = formType, onTransactionType = { formType = it },
            tags = formTags, onTags = { formTags = it }, person = formPerson, onPerson = { formPerson = it }, project = formProject, onProject = { formProject = it },
            account = formAccount, onAccount = { formAccount = it }, target = formTarget, onTarget = { formTarget = it }, saved = formSaved, onSaved = { formSaved = it },
            contribution = formContribution, onContribution = { formContribution = it }, deadline = formDeadline, onDeadline = { formDeadline = it },
            frequency = formFrequency, onFrequency = { formFrequency = it }, rate = formRate, onRate = { formRate = it },
            extra = formExtra, onExtra = { formExtra = it }, shared = formShared, onShared = { formShared = it },
            onSave = { saveForm() }, onDismiss = { dialogType = "" },
            suggestions = com.fincip.nativeapp.ui.FormSuggestions(
                projects = transactions(workspace).map { it.string("project") }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key },
                tags = transactions(workspace).flatMap { t -> t.arrayOrEmpty("tags").map { it.asStringOr() } }.filter { it.isNotBlank() && it != "imported" }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key },
                people = transactions(workspace).map { it.string("person") }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key },
                accounts = transactions(workspace).map { it.string("account") }.filter { it.isNotBlank() }.distinct(),
                providers = workspace.arrayOrEmpty("financeProducts").objects().map { it.string("provider") }.filter { it.isNotBlank() }.distinct()
            )
        )
    }
    if (backupPasswordDialog) {
        PasswordDialog(title = "Create encrypted backup", value = backupPassword, onChange = { backupPassword = it }, onDismiss = { backupPasswordDialog = false }, onConfirm = {
            runCatching {
                val payload = JsonObject().apply {
                    add("data", workspace.deepCopyObject())
                    val blobs = JsonArray()
                    workspace.arrayOrEmpty("receipts").objects().forEach { receipt ->
                        val file = File(receipt.string("filePath"))
                        if (file.isFile && file.length() <= 20L * 1024L * 1024L) {
                            val bytes = file.readBytes()
                            blobs.add(JsonObject().apply {
                                addProperty("id", receipt.string("id"))
                                addProperty("type", receipt.string("mime", "application/octet-stream"))
                                addProperty("data", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                            })
                        }
                    }
                    add("receiptFiles", blobs)
                }
                pendingExport = CryptoBackup.encrypt(payload.toString(), backupPassword)
                exportJsonLauncher.launch("FinCip-encrypted-backup.json")
                backupPassword = ""; backupPasswordDialog = false
            }.onFailure { notify(it.message ?: "Backup failed") }
        })
    }
    if (importPasswordDialog) {
        PasswordDialog(title = "Unlock FinCip backup", value = backupPassword, onChange = { backupPassword = it }, onDismiss = { importPasswordDialog = false; backupPassword = "" }, onConfirm = {
            runCatching {
                val decrypted = JsonParser.parseString(CryptoBackup.decrypt(pendingBackup, backupPassword)).asJsonObject
                val data = if (decrypted.has("data") && decrypted.get("data").isJsonObject) decrypted.getAsJsonObject("data") else decrypted
                val imported = mergeDefaults(data)
                val receiptFiles = decrypted.arrayOrEmpty("receiptFiles")
                val receiptDir = File(context.filesDir, "receipts").apply { mkdirs() }
                receiptFiles.forEach { entry ->
                    val blob = entry.asObjectOrNull() ?: return@forEach
                    val id = blob.string("id")
                    val bytes = android.util.Base64.decode(blob.string("data"), android.util.Base64.DEFAULT)
                    val safeName = id.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "receipt_${System.currentTimeMillis()}" }
                    val file = File(receiptDir, safeName)
                    file.writeBytes(bytes)
                    val rows = JsonArray()
                    imported.arrayOrEmpty("receipts").forEach { receiptElement ->
                        val receipt = receiptElement.asObjectOrNull()?.deepCopyObject()
                        if (receipt != null) {
                            if (receipt.string("id") == id) {
                                receipt.addProperty("filePath", file.absolutePath)
                                if (!receipt.has("mime")) receipt.addProperty("mime", blob.string("type", "application/octet-stream"))
                            }
                            rows.add(receipt)
                        }
                    }
                    imported.add("receipts", rows)
                }
                replaceWorkspace(imported)
                importPasswordDialog = false; backupPassword = ""; notify("Encrypted backup and available receipts restored")
            }.onFailure { notify("Could not unlock backup. Check the password.") }
        })
    }
    if (quickVoiceDialog) {
        AlertDialog(onDismissRequest = { quickVoiceDialog = false }, title = { Text("Quick voice entry") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PopField(value = quickEntryText, onValueChange = { quickEntryText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("What happened?") })
                Text("Example: spent 250 on lunch · received 12000 salary", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = {
            val record = parseQuickEntry(quickEntryText, workspace.objectOrEmpty("settings").string("baseCurrency", "INR"))
            if (record != null) { updateWorkspace { root -> val arr = root.arrayOrEmpty("transactions"); arr.add(record); root.add("transactions", arr) }; notify("Voice entry saved") } else notify("Could not understand that entry. Try ‘spent 250 on lunch’.")
            quickVoiceDialog = false
        }) { Text("Save entry") } }, dismissButton = { TextButton(onClick = { quickVoiceDialog = false }) { Text("Cancel") } })
    }
    if (statusMessage.isNotBlank()) {
        androidx.compose.material3.SnackbarHostState()
        AlertDialog(onDismissRequest = { statusMessage = "" }, title = { Text("FinCip") }, text = { Text(statusMessage) }, confirmButton = { TextButton(onClick = { statusMessage = "" }) { Text("OK") } })
    }
}

private fun JsonObject.toSession(): SupabaseApi.Session = SupabaseApi.Session(string("userId"), string("accessToken"), string("refreshToken").takeIf { it.isNotBlank() })
private fun Double?.orZero(): Double = this ?: 0.0
private fun transactionFingerprint(t: JsonObject): String = listOf(
    t.string("date"), t.string("type"), "%.2f".format(Locale.US, t.double("amount")),
    t.string("currency", "INR").uppercase(Locale.ROOT), t.string("title").trim().lowercase(Locale.ROOT)
).joinToString("|")

/**
 * Privacy screen. Android snapshots the app for Recents before app code can draw anything, so a live blur is not possible.
 * What the system does support: FLAG_SECURE (no screenshots/recording, preview hidden), and on Android 13+ an opt-out of the recents screenshot.
 */
fun applyPrivacyScreen(activity: Activity, on: Boolean) {
    val w = activity.window
    if (on) w.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    if (Build.VERSION.SDK_INT >= 33) runCatching { activity.setRecentsScreenshotEnabled(!on) }
}

@Composable
private fun MonthMenu(month: String, activeMonths: Set<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = runCatching { YearMonth.parse(month) }.getOrElse { YearMonth.now() }
    OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp))
        Text(selected.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)), fontWeight = FontWeight.Bold)
    }
    if (open) com.fincip.nativeapp.ui.MonthPickerDialog(selected, activeMonths, { onSelect(it.toString()); open = false }, { open = false })
}

@Composable
private fun PasswordDialog(title: String, value: String, onChange: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontWeight = FontWeight.Black) }, text = {
        PopField(value = value, onValueChange = onChange, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
    }, confirmButton = { TextButton(onClick = onConfirm, enabled = value.length >= 8) { Text("Continue") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun FinanceEntryDialog(
    type: String, name: String, onName: (String) -> Unit, amount: String, onAmount: (String) -> Unit,
    category: String, onCategory: (String) -> Unit, date: String, onDate: (String) -> Unit,
    currency: String, onCurrency: (String) -> Unit, transactionType: String, onTransactionType: (String) -> Unit,
    tags: String, onTags: (String) -> Unit, person: String, onPerson: (String) -> Unit, project: String, onProject: (String) -> Unit,
    account: String, onAccount: (String) -> Unit, target: String, onTarget: (String) -> Unit, saved: String, onSaved: (String) -> Unit,
    contribution: String, onContribution: (String) -> Unit, deadline: String, onDeadline: (String) -> Unit,
    frequency: String, onFrequency: (String) -> Unit, rate: String, onRate: (String) -> Unit,
    extra: String, onExtra: (String) -> Unit, shared: Boolean, onShared: (Boolean) -> Unit,
    onSave: () -> Unit, onDismiss: () -> Unit, suggestions: com.fincip.nativeapp.ui.FormSuggestions = com.fincip.nativeapp.ui.FormSuggestions()
) {
    val title = when (type) { "transaction" -> "Add transaction"; "budget" -> "Set budget"; "goal" -> "Goal or debt"; "contribution" -> "Add contribution"; "bill" -> "Add recurring bill"; "investment" -> "Add portfolio holding"; "asset" -> "Add net-worth item"; "product" -> "Add finance product"; "productContribution" -> "Record investment contribution"; "emergency" -> "Emergency fund settings"; "rule" -> "Add smart rule"; else -> "FinCip entry" }
    val hasAmount = type in listOf("contribution", "productContribution", "emergency", "transaction", "budget", "goal", "bill", "investment", "asset", "product")
    val amountLabel = when (type) { "contribution", "productContribution" -> "Contribution amount"; "emergency" -> "Emergency fund balance"; "goal" -> if (transactionType == "debt") "Total owed" else "Target amount"; "product" -> "Invested to date"; else -> "Amount / value" }
    val headerFill = when (type) {
        "transaction" -> if (transactionType == "income") Pop.Mint else Pop.Peach
        "budget" -> Pop.Lavender
        "goal", "contribution", "emergency" -> Pop.Mint
        "bill" -> Pop.Peach
        "investment", "product", "productContribution" -> Pop.Lavender
        else -> Pop.Sun
    }
    val categoryOptions: List<String>? = when (type) {
        "transaction" -> listOf("Food", "Transport", "Shopping", "Bills", "Health", "Education", "Entertainment", "Travel", "Home", "Subscriptions", "Personal care", "EMI", "Salary", "Freelance", "Business", "Investment", "Gift", "Refund", "Other")
        "investment" -> listOf("Stock", "Mutual fund", "ETF", "Crypto", "Other")
        "asset" -> listOf("Asset", "Liability")
        "product" -> com.fincip.nativeapp.data.IndiaKinds.all.map { it.id }
        else -> null
    }
    val categoryLabel = when (type) { "investment" -> "Asset type"; "asset" -> "Item type"; "product" -> "Product type"; else -> "Category" }
    val fieldGap = Arrangement.spacedBy(12.dp)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            Modifier.fillMaxSize().statusBarsPadding().padding(top = 20.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize()) {
                // ---- Coloured header with the amount as the hero
                Column(Modifier.fillMaxWidth().background(headerFill).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("FINCIP / NEW ENTRY", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .65f))
                            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Pop.Ink)
                        }
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = .6f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) { Text("×", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Pop.Ink) }
                    }
                    if (hasAmount) {
                        Spacer(Modifier.height(16.dp))
                        Text("${amountLabel.uppercase()} · $currency", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .65f))
                        androidx.compose.foundation.text.BasicTextField(
                            value = if (type == "goal") target else amount, onValueChange = if (type == "goal") onTarget else onAmount, singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Black, color = Pop.Ink, fontFamily = com.fincip.nativeapp.ui.PopFont),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Pop.Ink),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner -> Box { if ((if (type == "goal") target else amount).isEmpty()) Text("0", fontSize = 46.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .3f)); inner() } }
                        )
                    }
                }
                // ---- Scrollable form body
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (type == "transaction") PopSegmented(listOf("expense" to "↗  Expense", "income" to "↙  Income"), transactionType) { t -> onTransactionType(t); val pool = if (t == "income") com.fincip.nativeapp.ui.INCOME_CATEGORIES else com.fincip.nativeapp.ui.EXPENSE_CATEGORIES; if (category !in pool) onCategory(if (t == "income") "Salary" else "Food") }
                    if (type == "goal") PopSegmented(listOf("savings" to "Savings goal", "debt" to "Debt payoff"), transactionType, onTransactionType)
                    if (type == "bill") PopSegmented(listOf("weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly"), frequency, onFrequency)
                    if (categoryOptions != null && type != "transaction" && type != "product") PopChipPicker(categoryLabel, categoryOptions, category, onCategory)
                    if (hasAmount) PopChipPicker("Currency", CURRENCY_INFO.map { it.code }, currency, onCurrency)

                    if (type == "transaction") com.fincip.nativeapp.ui.TransactionFormBody(transactionType == "income", name, onName, category, onCategory, date, onDate, account, onAccount, person, onPerson, project, onProject, tags, onTags, extra, onExtra, suggestions)
                    if (type == "asset") {
                        val pool = if (category.equals("Liability", true)) com.fincip.nativeapp.data.NetWorthGroups.liabilities else com.fincip.nativeapp.data.NetWorthGroups.assets
                        PopChipPicker("Group", pool.map { "${com.fincip.nativeapp.data.NetWorthGroups.icon(it)} $it" }, pool.firstOrNull { it == person }?.let { "${com.fincip.nativeapp.data.NetWorthGroups.icon(it)} $it" } ?: "") { picked -> onPerson(picked.substringAfter(' ')) }
                        Text("Skip it and FinCip guesses the group from the name.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (type == "product") com.fincip.nativeapp.ui.ProductFormBody(
                        kind = category, onKind = { k ->
                            val prev = com.fincip.nativeapp.data.IndiaKinds.of(category)
                            val next = com.fincip.nativeapp.data.IndiaKinds.of(k)
                            val rateNow = rate.toDoubleOrNull()
                            if (rate.isBlank() || rateNow == null || rateNow == 0.0 || rateNow == prev.defaultRate) onRate(next.defaultRate?.toString() ?: "")
                            onCategory(k)
                        },
                        name = name, onName = onName, provider = person, onProvider = onPerson, invested = amount, current = saved, onCurrent = onSaved,
                        monthly = contribution, onMonthly = onContribution, rate = rate, onRate = onRate, start = date, onStart = onDate,
                        maturity = deadline, onMaturity = onDeadline, currency = currency, existingProviders = suggestions.providers
                    )
                    if (type == "goal") com.fincip.nativeapp.ui.GoalFormBody(
                        debt = transactionType == "debt", purpose = category, onPurpose = onCategory, name = name, onName = onName, target = target,
                        saved = saved, onSaved = onSaved, monthly = contribution, onMonthly = onContribution, rate = rate, onRate = onRate,
                        deadline = deadline, onDeadline = onDeadline, currency = currency
                    )
                    if (type != "transaction" && type != "product" && type != "goal") NeoCard {
                        Text("DETAILS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = fieldGap) {
                            if (type != "contribution" && type != "productContribution" && type != "emergency") PopField(value = name, onValueChange = onName, singleLine = true, label = { Text(if (type == "rule") "Keyword" else "Name / description") }, modifier = Modifier.fillMaxWidth())
                            if (type == "transaction" || type == "bill" || type == "product") PopField(value = date, onValueChange = onDate, singleLine = true, label = { Text(when (type) { "bill" -> "Next due date"; "product" -> "Start date"; else -> "Date" }) }, placeholder = { Text("YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                            if (type == "product") {
                                PopField(value = person, onValueChange = onPerson, singleLine = true, label = { Text("Provider / institution") }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PopField(value = saved, onValueChange = onSaved, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Current value") }, modifier = Modifier.weight(1f))
                                    PopField(value = contribution, onValueChange = onContribution, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Monthly add") }, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PopField(value = rate, onValueChange = onRate, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Rate % p.a.") }, modifier = Modifier.weight(1f))
                                    PopField(value = deadline, onValueChange = onDeadline, singleLine = true, label = { Text("Maturity") }, placeholder = { Text("optional") }, modifier = Modifier.weight(1f))
                                }
                            }
                            if (type == "emergency") PopField(value = target, onValueChange = onTarget, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Target months of expenses") }, modifier = Modifier.fillMaxWidth())
                            if (type == "transaction") {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PopField(value = account, onValueChange = onAccount, singleLine = true, label = { Text("Payment account") }, modifier = Modifier.weight(1f))
                                    PopField(value = person, onValueChange = onPerson, singleLine = true, label = { Text("Person / payee") }, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PopField(value = project, onValueChange = onProject, singleLine = true, label = { Text("Project") }, modifier = Modifier.weight(1f))
                                    PopField(value = tags, onValueChange = onTags, singleLine = true, label = { Text("Tags") }, placeholder = { Text("a, b") }, modifier = Modifier.weight(1f))
                                }
                                PopField(value = extra, onValueChange = onExtra, label = { Text("Split allocations · optional") }, placeholder = { Text("Food:250, Travel:100") }, modifier = Modifier.fillMaxWidth())
                            }
                            if (type == "goal") {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PopField(value = target, onValueChange = onTarget, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Target") }, modifier = Modifier.weight(1f))
                                    PopField(value = saved, onValueChange = onSaved, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Saved / repaid") }, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PopField(value = contribution, onValueChange = onContribution, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Monthly add") }, modifier = Modifier.weight(1f))
                                    PopField(value = rate, onValueChange = onRate, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Interest % p.a.") }, modifier = Modifier.weight(1f))
                                }
                                PopField(value = deadline, onValueChange = onDeadline, singleLine = true, label = { Text("Target date") }, placeholder = { Text("YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                            }
                            if (type == "investment") {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PopField(value = target, onValueChange = onTarget, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Units") }, modifier = Modifier.weight(1f))
                                    PopField(value = saved, onValueChange = onSaved, singleLine = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, label = { Text("Current unit price") }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (type == "transaction") PopSegmented(listOf("false" to "Personal", "true" to "Shared budget pack"), shared.toString()) { onShared(it == "true") }
                    if (type == "contribution") Text("This amount is added to the selected goal or debt balance.", style = MaterialTheme.typography.bodySmall)
                    if (type == "productContribution") Text("This adds to invested amount and current value. Update the current value after your next statement.", style = MaterialTheme.typography.bodySmall)
                    if (type == "emergency") Text("Keep this reserve separate from your ordinary savings goals.", style = MaterialTheme.typography.bodySmall)
                }
                // ---- Sticky action bar
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().imePadding().padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    com.fincip.nativeapp.ui.NeoOutlineButton("Cancel", onDismiss, Modifier.weight(1f))
                    NeoButton("Save", Modifier.weight(1.6f), onSave)
                }
            }
        }
    }
}

/** Full-width pill toggle for 2-3 mutually exclusive choices. */
@Composable
private fun PopSegmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (key, label) ->
            val active = key == selected
            val bg by animateColorAsState(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, tween(160), label = "seg")
            Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).background(bg).clickable { onSelect(key) }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, maxLines = 1)
            }
        }
    }
}

/** Labelled horizontally-scrolling chip row used instead of dropdowns in the entry form. */
@Composable
private fun PopChipPicker(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option -> PopChip(option, option == selected, { onSelect(option) }) }
        }
    }
}

private fun parseQuickEntry(text: String, currency: String): JsonObject? {
    val amountMatch = Regex("([0-9]+(?:[,.][0-9]{1,2})?)").find(text) ?: return null
    val amount = amountMatch.value.replace(",", "").toDoubleOrNull() ?: return null
    val lower = text.lowercase(Locale.ROOT)
    val type = if (listOf("spent", "paid", "bought", "expense", "purchased").any(lower::contains)) "expense" else "income"
    val title = text.replace(Regex("(?i)(spent|paid|received|earned|bought|salary|income)"), "").trim().ifBlank { "Quick entry" }
    return JsonObject().apply { addProperty("id", java.util.UUID.randomUUID().toString()); addProperty("title", title); addProperty("amount", amount); addProperty("currency", currency); addProperty("type", type); addProperty("category", if (type == "income") "Other" else "Food"); addProperty("date", isoToday()); addProperty("account", "UPI"); add("tags", JsonArray()) }
}

private fun parseStatement(text: String): List<JsonObject> {
    fun parseDate(raw: String): String {
        val value = raw.trim().substringBefore('T').substringBefore(' ')
        val patterns = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT),
            DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US),
            DateTimeFormatter.ofPattern("uuuuMMdd", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH)
        )
        val candidates = listOf(raw.trim(), value).distinct()
        return candidates.firstNotNullOfOrNull { candidate ->
            patterns.firstNotNullOfOrNull { formatter -> runCatching { LocalDate.parse(candidate, formatter).toString() }.getOrNull() }
        } ?: isoToday()
    }
    fun currencyCode(raw: String): String {
        val c = raw.trim().uppercase(Locale.ROOT)
        return when {
            c.contains("INR") || c.contains("₹") -> "INR"
            c.contains("USD") || c == "$" -> "USD"
            c.contains("EUR") || c == "€" -> "EUR"
            c.contains("GBP") || c == "£" -> "GBP"
            c.contains("JPY") -> "JPY"
            c.contains("CNY") || c.contains("RMB") -> "CNY"
            c.contains("CHF") -> "CHF"
            c.contains("CAD") -> "CAD"
            c.contains("AUD") -> "AUD"
            c.contains("SGD") -> "SGD"
            else -> "INR"
        }
    }
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (quoted && i + 1 < line.length && line[i + 1] == '"') { cell.append('"'); i++ }
                else quoted = !quoted
            } else if (ch == ',' && !quoted) { result += cell.toString().trim(); cell.clear() }
            else cell.append(ch)
            i++
        }
        result += cell.toString().trim()
        return result
    }

    if (text.contains("<STMTTRN>", true)) {
        val statementCurrency = Regex("(?i)<CURDEF>\\s*([A-Z]{3})").find(text)?.groupValues?.get(1).orEmpty()
        return Regex("(?is)<STMTTRN>(.*?)</STMTTRN>").findAll(text).mapNotNull { match ->
            val block = match.groupValues[1]
            val amount = Regex("(?i)<TRNAMT>\\s*([-0-9.]+)").find(block)?.groupValues?.get(1)?.toDoubleOrNull() ?: return@mapNotNull null
            val dateRaw = Regex("(?i)<DTPOSTED>\\s*([0-9]{8})").find(block)?.groupValues?.get(1).orEmpty()
            val name = Regex("(?i)<NAME>\\s*([^<\\r\\n]+)").find(block)?.groupValues?.get(1)?.trim().orEmpty()
                .ifBlank { Regex("(?i)<MEMO>\\s*([^<\\r\\n]+)").find(block)?.groupValues?.get(1)?.trim().orEmpty() }
                .ifBlank { "Imported OFX entry" }
            JsonObject().apply {
                addProperty("id", java.util.UUID.randomUUID().toString()); addProperty("title", name)
                addProperty("amount", kotlin.math.abs(amount)); addProperty("currency", currencyCode(statementCurrency))
                addProperty("type", if (amount >= 0) "income" else "expense"); addProperty("category", "Other")
                addProperty("date", parseDate(dateRaw)); addProperty("account", "Bank statement"); add("tags", JsonArray())
            }
        }.toList()
    }

    val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.size < 2) return emptyList()
    val header = parseCsvLine(lines.first()).map { it.trim().trim('"').lowercase(Locale.ROOT) }
    fun index(vararg names: String): Int = names.firstNotNullOfOrNull { n -> header.indexOfFirst { it.contains(n) }.takeIf { it >= 0 } } ?: -1
    val dateI = index("date", "transaction date", "value date", "posting date")
    val descI = index("description", "narration", "details", "payee", "merchant", "name", "particulars")
    val amountI = index("amount", "transaction amount", "withdrawal", "deposit")
    val debitI = index("debit", "withdrawal")
    val creditI = index("credit", "deposit")
    val currencyI = index("currency", "ccy", "curr")
    return lines.drop(1).mapNotNull { line ->
        val cols = parseCsvLine(line)
        fun col(i: Int) = cols.getOrNull(i).orEmpty()
        fun numeric(value: String): Double? = value.trim().replace(",", "").replace("₹", "").replace("$", "").replace("€", "").replace("£", "").replace(" ", "").toDoubleOrNull()
        val raw = if (debitI >= 0 || creditI >= 0) {
            val debit = numeric(col(debitI)) ?: 0.0
            val credit = numeric(col(creditI)) ?: 0.0
            if (credit > 0) credit else -debit
        } else numeric(col(amountI)) ?: return@mapNotNull null
        if (raw == 0.0) return@mapNotNull null
        val currency = currencyCode(col(currencyI))
        JsonObject().apply {
            addProperty("id", java.util.UUID.randomUUID().toString())
            addProperty("title", col(descI).ifBlank { "Imported bank entry" })
            addProperty("amount", kotlin.math.abs(raw)); addProperty("currency", currency)
            addProperty("type", if (raw >= 0) "income" else "expense"); addProperty("category", "Other")
            addProperty("date", parseDate(col(dateI))); addProperty("account", "Bank statement"); add("tags", JsonArray())
        }
    }
}

private fun exportCsv(workspace: JsonObject): String {
    val rows = transactions(workspace)
    return buildString {
        appendLine("date,type,title,category,amount,currency,account,person,project,tags,source,note,shared")
        rows.forEach { t ->
            val cells = listOf(t.string("date"), t.string("type"), t.string("title"), t.string("category"), t.double("amount").toString(), t.string("currency", "INR"), t.string("account"), t.string("person"), t.string("project"), t.get("tags")?.toString().orEmpty(), t.string("source"), t.string("note"), t.bool("shared").toString())
            appendLine(cells.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        }
    }
}

private fun writePdfReport(context: android.content.Context, uri: Uri, workspace: JsonObject, month: String) {
    val doc = android.graphics.pdf.PdfDocument()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = android.graphics.Color.BLACK }
    val page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    paint.textSize = 24f; paint.typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD); canvas.drawText("FinCip · Money report", 38f, 54f, paint)
    paint.textSize = 12f; paint.typeface = android.graphics.Typeface.DEFAULT; canvas.drawText("Month: $month · Generated ${isoToday()}", 38f, 80f, paint)
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    canvas.drawText("Income ${money(incomeTotal(workspace, month), base)}", 38f, 116f, paint)
    canvas.drawText("Spending ${money(expenseTotal(workspace, month), base)}", 250f, 116f, paint)
    var y = 158f
    paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    canvas.drawText("DATE      TYPE      DESCRIPTION                                  AMOUNT", 38f, y, paint); y += 23f
    paint.typeface = android.graphics.Typeface.DEFAULT
    monthlyTransactions(workspace, month).take(36).forEach { t ->
        val line = "${t.string("date")}   ${t.string("type").take(7)}   ${t.string("title").take(28)}   ${money(t.double("amount"), t.string("currency", "INR"))}"
        canvas.drawText(line, 38f, y, paint); y += 18f
    }
    doc.finishPage(page)
    context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
    doc.close()
}
