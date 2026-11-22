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
    settingsContent: @Composable (String) -> Unit,
    securityContent: @Composable () -> Unit
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var previous by remember { mutableIntStateOf(tab) }
    val profile = workspace.objectOrEmpty("profile")
    val tap = rememberTap()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HubHeader(workspace)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabNames.forEachIndexed { i, name -> PopChip(name, tab == i, { previous = tab; tab = i }) }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState >= initialState) 1 else -1
                (fadeIn(tween(220, delayMillis = 60)) + slideInHorizontally(tween(260)) { dir * it / 6 }) togetherWith (fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { -dir * it / 8 })
            }, label = "hubTab"
        ) { current ->
            CompositionLocalProvider(LocalEmbedded provides true) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (current) {
                        0 -> ProfileEditor(profile, onProfileChange)
                        1 -> SettingsTab(workspace, vibe, onVibe, dark, onDark, accent, soundOn, { previous = tab; tab = 0 }, { previous = tab; tab = 2 }, settingsContent)
                        2 -> { LockCard(onBlockScreens); securityContent() }
                        else -> CustomizationTab(workspace, onPetChange, accent, onAccent, appIcon, onAppIcon)
                    }
                }
            }
        }
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
    var secure by remember { mutableStateOf(AppLock.blockScreenshots(context)) }
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
        SwitchRow("Hide in app switcher", "Blocks screenshots and blanks FinCip in recent apps.", secure) { secure = it; AppLock.setBlockScreenshots(context, it); onBlockScreens(it) }
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
