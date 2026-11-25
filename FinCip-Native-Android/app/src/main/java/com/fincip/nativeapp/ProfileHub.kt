package com.fincip.nativeapp

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.AppIconOption
import com.fincip.nativeapp.data.AppIcons
import com.fincip.nativeapp.data.AppLock
import com.fincip.nativeapp.data.arrayOrEmpty
import com.fincip.nativeapp.data.objects
import com.fincip.nativeapp.data.double
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.transactions
import com.fincip.nativeapp.ui.AccentPresets
import com.fincip.nativeapp.ui.FinCipMark
import com.fincip.nativeapp.ui.NeoButton
import com.fincip.nativeapp.ui.NeoCard
import com.fincip.nativeapp.ui.NeoOutlineButton
import com.fincip.nativeapp.ui.PasswordField
import com.fincip.nativeapp.ui.PetOptions
import com.fincip.nativeapp.ui.Pop
import com.fincip.nativeapp.ui.PopChip
import com.fincip.nativeapp.ui.PopField
import com.fincip.nativeapp.ui.ProfileAvatar
import com.fincip.nativeapp.ui.rememberTap
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import com.google.gson.JsonObject
import com.fincip.nativeapp.ui.LockInput
import com.fincip.nativeapp.ui.LockTypes
import com.fincip.nativeapp.ui.lockTypeBlurb
import com.fincip.nativeapp.ui.lockTypeLabel

private val Banners = listOf(
    listOf(Color(0xFFD9D0F5), Color(0xFFF0A8B0), Color(0xFFF7D774)),
    listOf(Color(0xFF1F5C3A), Color(0xFF3C9460), Color(0xFFB9F227)),
    listOf(Color(0xFF5BB8D9), Color(0xFF9B8AE0), Color(0xFFF0A8D0)),
    listOf(Color(0xFFF8873B), Color(0xFFF26D85), Color(0xFFF7B733)),
    listOf(Color(0xFF26283A), Color(0xFF4B4F78), Color(0xFF9B8AE0)),
    listOf(Color(0xFF16231B), Color(0xFF2E8C7F), Color(0xFFB9F227))
)
private val Roles = listOf("Student", "Professional", "Freelancer", "Business owner", "Investor", "Saver")
private val TabNames = listOf("Profile", "Settings", "Privacy & security", "Customization")

/**
 * The one place for everything personal: profile card on top, four tabs below.
 * Settings are reachable only from here.
 */
@Composable
fun ProfileHub(
    workspace: JsonObject,
    initialTab: Int,
    onProfileChange: (JsonObject.() -> Unit) -> Unit,
    onPetChange: (JsonObject.() -> Unit) -> Unit,
    accent: String, onAccent: (String) -> Unit,
    appIcon: AppIconOption, onAppIcon: (AppIconOption) -> Unit,
    onBlockScreens: (Boolean) -> Unit,
    vibe: String, onVibe: (String) -> Unit, dark: Boolean, onDark: (Boolean) -> Unit, soundOn: Boolean,
    onNavigate: (String) -> Unit = {}, settingsStart: String = "",
    navContent: @Composable () -> Unit = {},
    settingsContent: @Composable (String) -> Unit,
    securityContent: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CompositionLocalProvider(LocalEmbedded provides true) {
            when (initialTab) {
                0 -> ProfilePage(workspace, onProfileChange, onNavigate)
                1 -> SettingsTab(workspace, vibe, onVibe, dark, onDark, accent, soundOn, { onNavigate("profile") }, { onNavigate("security") }, { onNavigate("customize") }, settingsStart.ifBlank { null }, settingsContent)
                2 -> { BackRow("Privacy & security") { onNavigate("settings") }; LockCard(onBlockScreens); securityContent() }
                else -> { BackRow("Customization") { onNavigate("settings") }; navContent(); CustomizationTab(workspace, onPetChange, accent, onAccent, appIcon, onAppIcon) }
            }
        }
    }
}

@Composable
private fun BackRow(title: String, onBack: () -> Unit) {
    val tap = rememberTap()
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { tap(); onBack() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 30.sp, modifier = Modifier.padding(end = 10.dp))
        Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun HubHeader(workspace: JsonObject) {
    val p = profileOf(workspace)
    val raw = workspace.objectOrEmpty("profile")
    val banner = Banners[raw.double("banner").toInt().coerceIn(0, Banners.lastIndex)]
    val txs = transactions(workspace)
    val prog = petProgress(workspace)
    NeoCard {
        Box(Modifier.fillMaxWidth()) {
            val bannerImg = if (raw.string("bannerMode") == "photo") com.fincip.nativeapp.ui.rememberUserImage(androidx.compose.ui.platform.LocalContext.current, true, raw.double("bannerStamp").toLong()) else null
            if (bannerImg != null) androidx.compose.foundation.Image(bannerImg, null, Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            else Box(Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(banner)))
            Box(Modifier.padding(top = 62.dp).align(Alignment.TopCenter).background(MaterialTheme.colorScheme.surface, CircleShape).padding(5.dp)) { com.fincip.nativeapp.ui.UserAvatar(p, size = 84.dp) }
        }
        Spacer(Modifier.height(48.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(p.name, fontWeight = FontWeight.Black, fontSize = 24.sp)
            if (p.handle.isNotBlank()) Text("@" + p.handle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            Text(listOf(p.tagline, raw.string("city")).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "FinCip member" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (raw.string("bio").isNotBlank()) Text(raw.string("bio"), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Entries" to txs.size.toString(), "Pet level" to prog.level.toString(), "Member since" to raw.string("joined").take(4).ifBlank { "—" }).forEach { (k, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(v, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(k.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)

@Composable
private fun ProfileEditor(profile: JsonObject, onChange: (JsonObject.() -> Unit) -> Unit) {
    val tap = rememberTap()
    var name by remember { mutableStateOf(profile.string("name")) }
    var handle by remember { mutableStateOf(profile.string("handle")) }
    var bio by remember { mutableStateOf(profile.string("bio")) }
    var city by remember { mutableStateOf(profile.string("city")) }
    var role by remember { mutableStateOf(profile.string("tagline")) }
    var seed by remember { mutableStateOf(profile.double("avatarSeed").toLong()) }
    var banner by remember { mutableIntStateOf(profile.double("banner").toInt()) }
    var saved by remember { mutableStateOf(false) }
    val valid = name.trim().length >= 2
    NeoCard {
        SectionTitle("Your avatar")
        Spacer(Modifier.height(10.dp))
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val mode = profile.string("avatarMode", "seed")
        val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && com.fincip.nativeapp.ui.saveUserImage(ctx, uri, false)) onChange { addProperty("avatarMode", "photo"); addProperty("photoStamp", System.currentTimeMillis()) }
        }
        val bannerPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && com.fincip.nativeapp.ui.saveUserImage(ctx, uri, true)) onChange { addProperty("bannerMode", "photo"); addProperty("bannerStamp", System.currentTimeMillis()) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            com.fincip.nativeapp.ui.UserAvatar(profileOf(JsonObject().apply { add("profile", profile) }), size = 84.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoOutlineButton("Upload photo", { photoPicker.launch("image/*") })
                NeoOutlineButton("Shuffle face", { seed = java.util.Random().nextLong() and 0x7fffffffL; saved = false; onChange { addProperty("avatarSeed", seed); addProperty("avatarMode", "seed") } })
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Characters", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            com.fincip.nativeapp.ui.AvatarPresets.forEachIndexed { i, look ->
                val on = mode == "preset" && profile.double("preset").toInt() == i
                Box(Modifier.size(60.dp).clip(CircleShape).border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape).clickable { tap(); onChange { addProperty("avatarMode", "preset"); addProperty("preset", i) } }) {
                    com.fincip.nativeapp.ui.PetCharacter(look, com.fincip.nativeapp.ui.PetMood.HAPPY, 6, diameter = 60.dp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Banner", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            NeoOutlineButton("Upload banner", { bannerPicker.launch("image/*") })
        }
        Spacer(Modifier.height(8.dp))
        Text("Banner colours", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Banners.forEachIndexed { i, colors ->
                Box(Modifier.size(width = 64.dp, height = 36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(colors))
                    .border(if (banner == i) 3.dp else 1.dp, if (banner == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable { tap(); banner = i; saved = false; onChange { addProperty("banner", i); addProperty("bannerMode", "color") } })
            }
        }
    }
    NeoCard {
        SectionTitle("About you")
        Spacer(Modifier.height(8.dp))
        PopField(name, { name = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
        if (!valid) Text("Name needs at least 2 characters.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        PopField(handle, { handle = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20); saved = false }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        Text("I am a…", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Roles.forEach { r -> PopChip(r, role == r, { role = r; saved = false }) } }
        Spacer(Modifier.height(10.dp))
        PopField(city, { city = it.take(40); saved = false }, Modifier.fillMaxWidth(), label = { Text("City") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        PopField(bio, { bio = it.take(140); saved = false }, Modifier.fillMaxWidth(), label = { Text("Short bio (${bio.length}/140)") })
        Spacer(Modifier.height(14.dp))
        NeoButton(if (saved) "Saved ✓" else "Save profile", Modifier.fillMaxWidth(), onClick = {
            onChange {
                addProperty("name", name.trim()); addProperty("handle", handle); addProperty("tagline", role); addProperty("avatarSeed", seed)
                addProperty("bio", bio.trim()); addProperty("city", city.trim()); addProperty("banner", banner)
                if (!has("joined")) addProperty("joined", java.time.LocalDate.now().toString())
            }
            saved = true
        }, enabled = valid && !saved)
    }
}

@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f)) }
        Switch(checked, onChecked, enabled = enabled)
    }
}

/** App password, fingerprint and auto-lock. The password never leaves the device and is stored only as a salted hash. */
@Composable
private fun LockCard(onBlockScreens: (Boolean) -> Unit) {
    val context = LocalContext.current
    var has by remember { mutableStateOf(AppLock.hasPassword(context)) }
    var enabled by remember { mutableStateOf(AppLock.lockEnabled(context)) }
    var bio by remember { mutableStateOf(AppLock.biometricEnabled(context)) }
    var timeout by remember { mutableIntStateOf(AppLock.timeoutSeconds(context)) }
    var mode by remember { mutableStateOf("") } // "", "setup", "verify-change", "verify-remove"
    var type by remember { mutableStateOf("password") }
    var first by remember { mutableStateOf<String?>(null) }
    var nonce by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    val bioOk = remember { AppLock.biometricAvailable(context) }
    fun reset() { mode = ""; first = null; message = ""; nonce += 1 }
    NeoCard {
        SectionTitle("App lock")
        Text(
            if (has) "FinCip asks for your ${lockTypeLabel(AppLock.lockType(context)).lowercase()} when you open it." else "Lock FinCip with a password, PIN, pattern, drawing, rhythm, emoji code or colour orbs. There is no recovery: if you forget it you will have to reinstall, so keep a backup.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)
        )
        Spacer(Modifier.height(12.dp))
        if (mode == "setup") {
            if (first == null) {
                Text("Lock style", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockTypes.forEach { (id, label) -> PopChip(label, type == id, { type = id; nonce += 1; message = "" }) }
                }
                Text(lockTypeBlurb(type), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(if (first == null) "Create your ${lockTypeLabel(type).lowercase()}" else "Repeat it to confirm", fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            LockInput(type, nonce, { input ->
                val f = first
                when {
                    type == "password" && input.length < 4 -> { message = "Use at least 4 characters."; nonce += 1 }
                    f == null -> { first = input; message = ""; nonce += 1 }
                    AppLock.same(type, f, input) -> { AppLock.setSecret(context, type, input); has = true; enabled = true; reset() }
                    else -> { message = "That did not match. Start again."; first = null; nonce += 1 }
                }
            })
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            NeoOutlineButton("Cancel", { reset() }, Modifier.fillMaxWidth())
        } else if (mode == "verify-change" || mode == "verify-remove") {
            val cur = AppLock.lockType(context)
            Text("Confirm your current ${lockTypeLabel(cur).lowercase()}", fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            LockInput(cur, nonce, { input ->
                if (AppLock.check(context, input)) {
                    if (mode == "verify-change") { mode = "setup"; first = null; message = ""; nonce += 1 }
                    else { AppLock.removePassword(context); has = false; enabled = false; bio = false; reset() }
                } else { message = "That is not right."; nonce += 1 }
            })
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            NeoOutlineButton("Cancel", { reset() }, Modifier.fillMaxWidth())
        } else if (!has) {
            NeoButton("Set up app lock", Modifier.fillMaxWidth(), onClick = { mode = "setup"; type = "password"; first = null; nonce += 1 })
        } else {
            SwitchRow("Lock FinCip", "Require it to open the app.", enabled) { enabled = it; AppLock.setLockEnabled(context, it) }
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                "Fingerprint / face unlock", if (bioOk) "Use your phone's biometrics instead." else "No fingerprint or face is enrolled on this phone.", bio && bioOk, enabled = bioOk && enabled
            ) { on ->
                if (!on) { bio = false; AppLock.setBiometric(context, false) }
                else AppLock.authenticate(context, "Confirm to enable") { ok -> if (ok) { bio = true; AppLock.setBiometric(context, true) } }
            }
            Spacer(Modifier.height(10.dp))
            Text("Lock after leaving the app", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Immediately" to 0, "30 seconds" to 30, "1 minute" to 60, "5 minutes" to 300).forEach { (label, s) ->
                    PopChip(label, timeout == s, { timeout = s; AppLock.setTimeoutSeconds(context, s) })
                }
            }
            Text("Choosing a file (backup, import) leaves the app briefly, so a short delay avoids re-entering it.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoOutlineButton("Change lock", { mode = "verify-change"; message = ""; nonce += 1 }, Modifier.weight(1f))
                NeoOutlineButton("Remove", { mode = "verify-remove"; message = ""; nonce += 1 }, Modifier.weight(1f))
            }
        }
    }
    NeoCard {
        var privacy by remember { mutableStateOf(AppLock.privacyScreen(context)) }
        SwitchRow("Privacy screen", "Hides FinCip in the recent-apps switcher and blocks screenshots and screen recording. While on, you cannot screenshot FinCip yourself.", privacy) { privacy = it; AppLock.setPrivacyScreen(context, it); onBlockScreens(it) }
    }
}

@Composable
private fun CustomizationTab(
    workspace: JsonObject, onPetChange: (JsonObject.() -> Unit) -> Unit,
    accent: String, onAccent: (String) -> Unit, appIcon: AppIconOption, onAppIcon: (AppIconOption) -> Unit
) {
    val tap = rememberTap()
    NeoCard {
        SectionTitle("App colour")
        Text("17 colours. Each has a light and a dark version and works with every vibe. \"Vibe\" keeps the theme's own colours.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            run {
                val on = accent == "Default"
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { tap(); onAccent("Default") }) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
                        .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, CircleShape))
                    Text("Vibe", style = MaterialTheme.typography.labelSmall, fontWeight = if (on) FontWeight.Black else FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            AccentPresets.forEach { a ->
                val on = a.name == accent
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { tap(); onAccent(a.name) }) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(if (a.name == "Wallpaper") Brush.sweepGradient(listOf(Color(0xFFF26D85), Color(0xFFF7B733), Color(0xFF9BDE3A), Color(0xFF5BB8D9), Color(0xFF9B8AE0), Color(0xFFF26D85))) else Brush.linearGradient(listOf(a.swatch, a.swatch)))
                            .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(a.name, style = MaterialTheme.typography.labelSmall, fontWeight = if (on) FontWeight.Black else FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (accent == "Wallpaper") Text("Uses your Android wallpaper colours (Android 12 or newer).", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    }
    NeoCard {
        SectionTitle("App icon")
        Text("Pick the colour of FinCip on your home screen. It can take a few seconds to update.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
        Spacer(Modifier.height(12.dp))
        AppIcons.options.chunked(5).forEach { rowOpts -> Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            rowOpts.forEach { o ->
                val on = o.id == appIcon.id
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { tap(); onAppIcon(o) }) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(o.bg))
                            .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) { FinCipMark(size = 38.dp, glyph = Color(o.leaf), accent = Color(o.shoot)) }
                    Text(o.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (on) FontWeight.Black else FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            repeat(5 - rowOpts.size) { Spacer(Modifier.size(62.dp)) }
        } }
    }
    Text("Your companion", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
    PetCustomizer(workspace, onPetChange)
}


// ───────────────────────────── Premium profile page ─────────────────────────────

private data class Badge(val emoji: String, val title: String, val desc: String, val cur: Int, val target: Int) { val done get() = cur >= target }

private fun longestStreak(days: Set<java.time.LocalDate>): Int {
    var best = 0; var run = 0; var prev: java.time.LocalDate? = null
    for (d in days.sorted()) { run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1; best = maxOf(best, run); prev = d }
    return best
}

private fun rankOf(level: Int) = when { level >= 15 -> "Mogul"; level >= 10 -> "Strategist"; level >= 6 -> "Planner"; level >= 3 -> "Saver"; else -> "Rookie" }

/** Every badge is computed from the user's real data; nothing is stored or faked. */
private fun badgesOf(ws: JsonObject, hasLock: Boolean): List<Badge> {
    val txs = transactions(ws)
    val days = txs.mapNotNull { runCatching { java.time.LocalDate.parse(it.string("date")) }.getOrNull() }.toSet()
    val longest = longestStreak(days)
    val goals = ws.arrayOrEmpty("goals").objects()
    val reached = goals.count { it.string("type") != "debt" && it.double("target") > 0 && it.double("saved") >= it.double("target") }
    val base = ws.objectOrEmpty("settings").string("baseCurrency", "INR")
    val currencies = txs.map { it.string("currency", base).ifBlank { base } }.toSet().size
    val inGreen = if (txs.isNotEmpty() && com.fincip.nativeapp.data.incomeTotal(ws) > com.fincip.nativeapp.data.expenseTotal(ws)) 1 else 0
    val level = petProgress(ws).level
    return listOf(
        Badge("🌱", "First step", "Log your first entry", txs.size, 1),
        Badge("✍️", "Getting regular", "Log 25 entries", txs.size, 25),
        Badge("📒", "Ledger pro", "Log 100 entries", txs.size, 100),
        Badge("🔥", "On a roll", "7 days in a row", longest, 7),
        Badge("🗓️", "Habit formed", "30 days in a row", longest, 30),
        Badge("🎯", "Planner", "Create a savings goal", goals.size, 1),
        Badge("🏆", "Goal crusher", "Fully fund a goal", reached, 1),
        Badge("💼", "Budget keeper", "Set a budget", ws.arrayOrEmpty("budgets").size(), 1),
        Badge("🛡️", "Budget boss", "Set 3 budgets", ws.arrayOrEmpty("budgets").size(), 3),
        Badge("🔔", "Bill tamer", "Track a recurring bill", ws.arrayOrEmpty("recurringRules").size(), 1),
        Badge("💚", "In the green", "Earn more than you spend", inGreen, 1),
        Badge("🌍", "Globetrotter", "Log in 2 currencies", currencies, 2),
        Badge("📈", "Investor", "Add a finance product", ws.arrayOrEmpty("financeProducts").size(), 1),
        Badge("🔒", "Locked in", "Turn on app lock", if (hasLock) 1 else 0, 1),
        Badge("🐣", "Growing buddy", "Companion reaches level 5", level, 5),
        Badge("⭐", "Star companion", "Companion reaches level 10", level, 10)
    )
}

@Composable
private fun PCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp), content = content)
}

@Composable
private fun PHeader(title: String, trailing: String = "") {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
        if (trailing.isNotBlank()) Text(trailing, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StatTileP(value: String, label: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(scheme.surfaceVariant.copy(alpha = .55f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = (-0.5).sp)
        Text(label, fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .62f), lineHeight = 15.sp)
    }
}

@Composable
private fun HeroCover(p: com.fincip.nativeapp.ui.ProfileData, raw: JsonObject, centered: Boolean, level: Int, entries: Int, streak: Int, joined: String?, onEdit: () -> Unit) {
    val context = LocalContext.current
    val img = if (raw.string("bannerMode") == "photo") com.fincip.nativeapp.ui.rememberUserImage(context, true, raw.double("bannerStamp").toLong()) else null
    val glass = Color.White.copy(alpha = .16f)
    Box(Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(32.dp))) {
        if (img != null) androidx.compose.foundation.Image(img, null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        else Box(Modifier.matchParentSize().background(Brush.linearGradient(Banners[raw.double("banner").toInt().coerceIn(0, Banners.lastIndex)])))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Transparent, .45f to Color.Black.copy(alpha = .15f), 1f to Color.Black.copy(alpha = .82f))))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Box(Modifier.background(Color.White.copy(alpha = .9f), CircleShape).padding(4.dp)) { com.fincip.nativeapp.ui.UserAvatar(p, size = if (centered) 92.dp else 76.dp) }
            Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
                Text(p.name, fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White, letterSpacing = (-0.6).sp, maxLines = 1)
                if (p.handle.isNotBlank()) Text("@" + p.handle, color = Color.White.copy(alpha = .75f), fontSize = 14.sp)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(p.tagline, raw.string("city"), joined?.let { "Since $it" } ?: "").filter { it.isNotBlank() }.forEach {
                    Text(it, Modifier.clip(RoundedCornerShape(50)).background(glass).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(glass).padding(vertical = 12.dp)) {
                listOf("Lv $level" to "Level", entries.toString() to "Entries", "${streak}d" to "Streak").forEach { (v, l) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White); Text(l, fontSize = 11.sp, color = Color.White.copy(alpha = .75f))
                    }
                }
            }
            if (raw.string("bio").isNotBlank()) Text(raw.string("bio"), color = Color.White.copy(alpha = .85f), fontSize = 13.sp, maxLines = 2)
            Text("Edit profile", Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White).clickable { onEdit() }.padding(vertical = 14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Black, color = Color(0xFF16231B))
        }
    }
}

@Composable
private fun ProfilePage(workspace: JsonObject, onProfileChange: (JsonObject.() -> Unit) -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val tap = rememberTap()
    var editing by remember { mutableStateOf(false) }
    val p = profileOf(workspace)
    val raw = workspace.objectOrEmpty("profile")
    val prog = petProgress(workspace)
    val txs = transactions(workspace)
    val badges = remember(workspace) { badgesOf(workspace, AppLock.hasPassword(context)) }
    val unlocked = badges.filter { it.done }
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val joinedDate = runCatching { java.time.LocalDate.parse(raw.string("joined").take(10)) }.getOrNull()
    val joinedShort = joinedDate?.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH))
    val joinedLong = joinedDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH))

    // ── Identity
    val heroLayout = raw.string("heroLayout", "classic")
    var studio by remember { mutableStateOf(false) }
    if (heroLayout != "classic") HeroCover(p, raw, heroLayout == "centered", prog.level, txs.size, entryStreak(workspace), joinedShort, onEdit = { tap(); editing = true })
    else
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(scheme.surface)) {
        Box(Modifier.fillMaxWidth()) {
            val bannerImg = if (raw.string("bannerMode") == "photo") com.fincip.nativeapp.ui.rememberUserImage(context, true, raw.double("bannerStamp").toLong()) else null
            if (bannerImg != null) androidx.compose.foundation.Image(bannerImg, null, Modifier.fillMaxWidth().height(132.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            else Box(Modifier.fillMaxWidth().height(132.dp).background(Brush.linearGradient(Banners[raw.double("banner").toInt().coerceIn(0, Banners.lastIndex)])))
            Box(Modifier.align(Alignment.BottomStart).offset(x = 20.dp, y = 46.dp).background(scheme.surface, CircleShape).padding(5.dp)) { com.fincip.nativeapp.ui.UserAvatar(p, size = 92.dp) }
        }
        Spacer(Modifier.height(54.dp))
        Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontWeight = FontWeight.Black, fontSize = 27.sp, letterSpacing = (-0.6).sp, maxLines = 1)
                    if (p.handle.isNotBlank()) Text("@" + p.handle, color = scheme.onSurface.copy(alpha = .55f))
                }
                Text("Edit", Modifier.clip(RoundedCornerShape(50)).background(scheme.primary).clickable { tap(); editing = true }.padding(horizontal = 18.dp, vertical = 9.dp), color = scheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(p.tagline, raw.string("city"), joinedShort?.let { "Member since $it" } ?: "").filter { it.isNotBlank() }.forEach {
                    Text(it, Modifier.clip(RoundedCornerShape(50)).background(scheme.surfaceVariant.copy(alpha = .7f)).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (raw.string("bio").isNotBlank()) Text(raw.string("bio"), Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface.copy(alpha = .75f))
            if (joinedLong != null) Text("Account created $joinedLong", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .45f))
        }
        Spacer(Modifier.height(18.dp))
    }

    // ── FinCip identity card (fully customisable)
    val idData = com.fincip.nativeapp.ui.IdData(p.name, p.handle, prog.level, rankOf(prog.level), prog.xpInLevel, prog.xp, unlocked.map { it.emoji }, unlocked.size, badges.size, txs.size, entryStreak(workspace), joinedShort ?: "")
    val idLook = com.fincip.nativeapp.ui.idLookOf(raw)
    com.fincip.nativeapp.ui.IdentityCard(idData, idLook)
    Text("✦  Customize cards", Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(scheme.primary.copy(alpha = .14f)).clickable { tap(); studio = true }.padding(vertical = 13.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold, color = scheme.primary)
    if (studio) com.fincip.nativeapp.ui.CardStudioDialog(
        data = idData, look = idLook, heroLayout = heroLayout,
        onHeroLayout = { v -> onProfileChange { addProperty("heroLayout", v) } },
        onLook = { _, changes -> onProfileChange { changes.forEach { (k, v) -> when (v) { is Boolean -> addProperty(k, v); is Int -> addProperty(k, v); else -> addProperty(k, v.toString()) } } } },
        onChangeBanner = { studio = false; editing = true }, onDismiss = { studio = false }
    )

    // ── Achievements
    PHeader("Achievements", "${unlocked.size} / ${badges.size}")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        badges.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (b.done) scheme.primary.copy(alpha = .15f) else scheme.surface).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(44.dp).background(if (b.done) scheme.primary.copy(alpha = .28f) else scheme.onSurface.copy(alpha = .07f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(b.emoji, fontSize = 22.sp, modifier = Modifier.then(if (b.done) Modifier else Modifier.alpha(.35f)))
                        }
                        Text(b.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(b.desc, fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .6f), lineHeight = 14.sp, maxLines = 2)
                        if (b.done) Text("Unlocked", fontSize = 11.sp, fontWeight = FontWeight.Black, color = scheme.primary)
                        else {
                            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = .1f))) {
                                Box(Modifier.fillMaxWidth((b.cur.toFloat() / b.target).coerceIn(0.02f, 1f)).height(5.dp).clip(CircleShape).background(scheme.primary.copy(alpha = .7f)))
                            }
                            Text("${b.cur} / ${b.target}", fontSize = 11.sp, color = scheme.onSurface.copy(alpha = .55f))
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    // ── Financial journey
    PHeader("Financial journey")
    val ym = java.time.YearMonth.now()
    val budgets = workspace.arrayOrEmpty("budgets").objects()
    val spendNow = com.fincip.nativeapp.data.monthlyTransactions(workspace, ym.toString()).filter { com.fincip.nativeapp.data.isSpend(it) }
    val onTrack = budgets.count { b ->
        val spent = spendNow.filter { it.string("category").equals(b.string("category"), true) }.sumOf { com.fincip.nativeapp.data.toBase(workspace, it.double("amount"), it.string("currency", "INR")) }
        spent <= com.fincip.nativeapp.data.toBase(workspace, b.double("limit"), b.string("currency", base))
    }
    val goals = workspace.arrayOrEmpty("goals").objects().filter { it.string("type") != "debt" }
    val reachedGoals = goals.count { it.double("target") > 0 && it.double("saved") >= it.double("target") }
    val streak = entryStreak(workspace)
    val best = longestStreak(txs.mapNotNull { runCatching { java.time.LocalDate.parse(it.string("date")) }.getOrNull() }.toSet())
    PCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTileP(txs.size.toString(), "Transactions recorded", Modifier.weight(1f))
            StatTileP(if (budgets.isEmpty()) "—" else "$onTrack/${budgets.size}", "Budgets on track this month", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTileP(if (goals.isEmpty()) "—" else "$reachedGoals/${goals.size}", "Savings goals reached", Modifier.weight(1f))
            StatTileP("${streak}d", "Current streak · best ${best}d", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text("Entries per month", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        val months = (5 downTo 0).map { ym.minusMonths(it.toLong()) }
        val counts = months.map { com.fincip.nativeapp.data.monthlyTransactions(workspace, it.toString()).size }
        val peak = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
        if (counts.all { it == 0 }) Text("Nothing logged in the last six months yet.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .6f))
        else Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            months.forEachIndexed { i, m ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(counts[i].toString(), fontSize = 10.sp, color = scheme.onSurface.copy(alpha = .6f))
                    Box(Modifier.fillMaxWidth().height((52.dp * counts[i] / peak).coerceAtLeast(4.dp)).clip(RoundedCornerShape(8.dp)).background(if (i == 5) scheme.primary else scheme.primary.copy(alpha = .35f)))
                    Text(m.month.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.ENGLISH), fontSize = 10.sp, color = scheme.onSurface.copy(alpha = .55f))
                }
            }
        }
    }

    // ── Companion
    PHeader("Your companion")
    val pet = workspace.objectOrEmpty("mascot")
    val unlockedAcc = PetOptions.accessories.filter { it.second <= prog.level && it.first != "None" }
    val nextAcc = PetOptions.accessories.filter { it.second > prog.level }.minByOrNull { it.second }
    PCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            com.fincip.nativeapp.ui.PetCharacter(petLookOf(pet), com.fincip.nativeapp.ui.PetMood.HAPPY, prog.level, Modifier.clip(RoundedCornerShape(26.dp)), diameter = 112.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pet.string("name", "Mochi"), fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Level ${prog.level} · ${com.fincip.nativeapp.ui.PetStageNames[com.fincip.nativeapp.ui.petStage(prog.level)]}", fontSize = 13.sp, color = scheme.onSurface.copy(alpha = .65f))
                Text("Customize ›", Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).background(scheme.primary.copy(alpha = .15f)).clickable { tap(); onNavigate("customize") }.padding(horizontal = 14.dp, vertical = 7.dp), color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Unlocked accessories · ${unlockedAcc.size}/${PetOptions.accessories.size - 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            unlockedAcc.forEach { Text(it.first, Modifier.clip(RoundedCornerShape(50)).background(scheme.surfaceVariant.copy(alpha = .7f)).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp) }
        }
        if (nextAcc != null) Text("Next unlock: ${nextAcc.first} at level ${nextAcc.second}", Modifier.padding(top = 8.dp), fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .55f))
    }

    if (editing) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { editing = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(scheme.background).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Edit profile", fontWeight = FontWeight.Black, fontSize = 24.sp)
                ProfileEditor(raw, onProfileChange)
                NeoButton("Done", Modifier.fillMaxWidth(), onClick = { editing = false })
            }
        }
    }
}
