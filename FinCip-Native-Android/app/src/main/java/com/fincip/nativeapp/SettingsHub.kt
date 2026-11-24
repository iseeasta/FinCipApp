package com.fincip.nativeapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.ui.FinCipTheme
import com.fincip.nativeapp.ui.NeoCard
import com.fincip.nativeapp.ui.PopChip
import com.fincip.nativeapp.ui.Vibes
import com.fincip.nativeapp.ui.rememberTap
import com.google.gson.JsonObject

@Composable
private fun GroupLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f), modifier = Modifier.padding(start = 6.dp, top = 6.dp))
}

@Composable
private fun SettingRow(icon: ImageVector, label: String, value: String = "", last: Boolean = false, onClick: () -> Unit) {
    val tap = rememberTap()
    Column(Modifier.fillMaxWidth().clickable { tap(); onClick() }) {
        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .75f), modifier = Modifier.size(22.dp))
            Text(label, Modifier.weight(1f).padding(start = 14.dp), fontWeight = FontWeight.Medium, fontSize = 15.sp)
            if (value.isNotBlank()) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f), modifier = Modifier.padding(end = 4.dp))
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f), modifier = Modifier.size(20.dp))
        }
        if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Settings as a grouped list (Account / Preferences / Data / Support). Each row opens its own page, so the
 * long list of switches is split into small, focused screens.
 */
@Composable
fun SettingsTab(
    workspace: JsonObject, vibe: String, onVibe: (String) -> Unit, dark: Boolean, onDark: (Boolean) -> Unit, accent: String,
    soundOn: Boolean, onGoProfile: () -> Unit, onGoSecurity: () -> Unit, onGoCustomize: () -> Unit = {}, initialPage: String? = null, content: @Composable (String) -> Unit
) {
    var page by remember { mutableStateOf(initialPage) }
    val tap = rememberTap()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val settings = workspace.objectOrEmpty("settings")
    val titles = mapOf("theme" to "Theme & vibe", "currency" to "Currency", "dashboard" to "Home layout", "sound" to "Sound & touch", "privacy" to "Privacy defaults", "data" to "Reports & exports", "about" to "About FinCip", "support" to "Support the developer")
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val deeper = targetState != null
            (fadeIn(tween(220, delayMillis = 60)) + slideInHorizontally(tween(260)) { if (deeper) it / 6 else -it / 6 }) togetherWith
                (fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { if (deeper) -it / 8 else it / 8 })
        }, label = "settingsPage"
    ) { current ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (current == null) {
                GroupLabel("Account")
                NeoCard {
                    SettingRow(Icons.Default.Person, "Manage profile", onClick = onGoProfile)
                    SettingRow(Icons.Default.Lock, "Password & app lock", onClick = onGoSecurity)
                    SettingRow(Icons.Default.VisibilityOff, "Privacy defaults", last = true) { page = "privacy" }
                }
                GroupLabel("Preferences")
                NeoCard {
                    SettingRow(Icons.Default.Palette, "Theme & vibe", vibe) { page = "theme" }
                    SettingRow(Icons.Default.AutoAwesome, "Colours, icon & companion", onClick = onGoCustomize)
                    SettingRow(Icons.Default.CurrencyExchange, "Currency", settings.string("baseCurrency", "INR")) { page = "currency" }
                    SettingRow(Icons.Default.Dashboard, "Home layout") { page = "dashboard" }
                    SettingRow(Icons.Default.VolumeUp, "Sound & touch", if (soundOn) "On" else "Off") { page = "sound" }
                    SettingRow(Icons.Default.Notifications, "Notifications", "Bill reminders", last = true) {
                        runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
                GroupLabel("Data")
                NeoCard {
                    SettingRow(Icons.Default.Description, "Reports & exports") { page = "data" }
                    SettingRow(Icons.Default.CloudUpload, "Backup & restore", last = true, onClick = onGoSecurity)
                }
                GroupLabel("Support")
                NeoCard {
                    SettingRow(Icons.Default.Favorite, "Support the developer", "Voluntary · UPI") { page = "support" }
                    SettingRow(Icons.Default.Info, "About FinCip", "v1.0", last = true) { page = "about" }
                }
            } else {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { tap(); page = null }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", fontSize = 30.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(titles[current] ?: "Settings", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                }
                when (current) {
                    "theme" -> ThemePage(vibe, onVibe, dark, onDark, accent)
                    "support" -> com.fincip.nativeapp.ui.SupportCard()
                    "about" -> AboutPage()
                    else -> content(current)
                }
            }
        }
    }
}

@Composable
private fun ThemePage(vibe: String, onVibe: (String) -> Unit, dark: Boolean, onDark: (Boolean) -> Unit, accent: String) {
    NeoCard {
        Text("Appearance", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PopChip("Light", !dark, { onDark(false) })
            PopChip("Dark", dark, { onDark(true) })
        }
        Text("Each vibe below has both a light and a dark version, and your app colour is applied on top.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
    }
    Text("Vibes · ${Vibes.size}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
    Vibes.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { v ->
                val on = v.name == vibe || (vibe == "Brutalist Lime" && v.name == "Brutalist")
                Box(Modifier.weight(1f)) {
                    FinCipTheme(v.name, dark, accent) {
                        val s = MaterialTheme.colorScheme
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(s.background)
                                .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else s.outlineVariant, RoundedCornerShape(18.dp)).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NeoCard {
                                Text(v.name, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                Text(v.blurb, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, color = s.onSurface.copy(alpha = .65f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { PopChip("Tab", true, {}); PopChip("Tab", false, {}) }
                        }
                    }
                    Box(Modifier.matchParentSize().clickable { onVibe(v.name) })
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}


private data class Feature(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val body: String)

@Composable
private fun AboutPage() {
    val scheme = MaterialTheme.colorScheme
    val groups = listOf(
        "Your money" to listOf(
            Feature(Icons.Default.Dashboard, "Overview & widgets", "A home screen of spending, income and savings you can rearrange, with charts and your companion on top."),
            Feature(Icons.Default.Receipt, "Transactions", "Add entries in seconds with a date picker, payment-method dropdown and smart tag, project and payee suggestions. Every category has its own icon."),
            Feature(Icons.Default.CloudUpload, "Import & export", "Bring in CSV statements from banks and UPI apps onto their real dates, with duplicate detection and one-tap undo. Export CSV, a PDF report, or a file shaped for another app."),
            Feature(Icons.Default.CalendarMonth, "Bills, subscriptions & calendar", "Recurring bills with reminders, subscription tracking and a month-by-month money calendar.")
        ),
        "Plan ahead" to listOf(
            Feature(Icons.Default.TrendingUp, "Budgets, goals & debt", "Category budgets, savings goals and debt payoff with progress you can see."),
            Feature(Icons.Default.CurrencyExchange, "Multi-currency", "Record in any currency; totals are converted to your base currency while originals stay untouched."),
            Feature(Icons.Default.AutoAwesome, "Insights & smart rules", "Spot trends, run what-if scenarios, forecast, and set rules that tag entries for you."),
            Feature(Icons.Default.Description, "Net worth, portfolio & tax", "Track assets and liabilities, holdings, India finance tools and a global tax planner.")
        ),
        "Make it yours" to listOf(
            Feature(Icons.Default.Favorite, "Companion & levels", "A pet that grows as you build the habit, with XP, ranks and achievements earned from real activity."),
            Feature(Icons.Default.Palette, "Vibes, colours & icons", "Twelve vibes, seventeen accents, dark and light modes, app-icon colours, and a fully customisable profile and ID card."),
            Feature(Icons.Default.Notifications, "Sound & reminders", "Optional click sounds and haptics, plus local bill reminders.")
        ),
        "Private by design" to listOf(
            Feature(Icons.Default.Lock, "Encrypted & locked", "Your data is sealed on this device with AES-256 and an Android Keystore key. Lock with a password, PIN, pattern, drawing, rhythm, emoji or orbs, or use fingerprint and face."),
            Feature(Icons.Default.VisibilityOff, "Privacy screen", "Hide amounts, and hide FinCip in the app switcher while blocking screenshots."),
            Feature(Icons.Default.Info, "Backups you control", "Encrypted backup and portable JSON export. Sync only if you choose to connect your own project.")
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NeoCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.fincip.nativeapp.ui.FinCipMark(size = 44.dp)
                Column { Text("FinCip", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall); Text("Version 1.0.0", style = MaterialTheme.typography.labelMedium) }
            }
            Spacer(Modifier.height(8.dp))
            Text("A local-first money companion. Your transactions, goals and profile stay on this device, encrypted, unless you choose to sync to your own project.", style = MaterialTheme.typography.bodyMedium)
        }
        groups.forEach { (title, items) ->
            Text(title.uppercase(), Modifier.padding(start = 6.dp), fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black, color = scheme.onBackground.copy(alpha = .55f))
            NeoCard {
                items.forEachIndexed { i, f ->
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(scheme.primary.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Icon(f.icon, null, tint = scheme.primary) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(f.title, fontWeight = FontWeight.Bold)
                            Text(f.body, fontSize = 13.sp, lineHeight = 18.sp, color = scheme.onSurface.copy(alpha = .68f))
                        }
                    }
                    if (i < items.lastIndex) HorizontalDivider(color = scheme.outlineVariant.copy(alpha = .5f))
                }
            }
        }
        Text("Made with care. Voluntary support is available under Settings → Support the developer.", Modifier.padding(horizontal = 6.dp), fontSize = 12.sp, color = scheme.onBackground.copy(alpha = .55f))
    }
}
