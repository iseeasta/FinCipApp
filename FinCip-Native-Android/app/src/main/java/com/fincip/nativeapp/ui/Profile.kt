package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ProfileData(val name: String, val handle: String, val tagline: String, val seed: Long)

private val Roles = listOf("Student", "Professional", "Freelancer", "Business owner", "Investor", "Saver")

private fun cleanHandle(raw: String, name: String): String {
    val source = raw.ifBlank { name }
    return source.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20)
}

/** Shared by first-run onboarding and the edit-profile dialog. */
@Composable
fun ProfileForm(initial: ProfileData, submitLabel: String, onSubmit: (ProfileData) -> Unit, modifier: Modifier = Modifier, onCancel: (() -> Unit)? = null) {
    var name by remember { mutableStateOf(initial.name) }
    var handle by remember { mutableStateOf(initial.handle) }
    var tagline by remember { mutableStateOf(initial.tagline) }
    var seed by remember { mutableStateOf(initial.seed) }
    var showError by remember { mutableStateOf(false) }
    val valid = name.trim().length >= 2
    Column(
        modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            ProfileAvatar(seed, size = 132.dp)
            Box(
                Modifier.align(Alignment.BottomEnd).size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { seed = java.util.Random().nextInt(1_000_000_000).toLong() + 1 },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Refresh, contentDescription = "Shuffle avatar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp)) }
        }
        Text("Tap the button to shuffle your avatar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
        PopField(value = name, onValueChange = { name = it.take(40); showError = false }, singleLine = true, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
        PopField(value = handle, onValueChange = { handle = it.take(20) }, singleLine = true, label = { Text("Username · optional") }, placeholder = { Text(cleanHandle("", name).ifBlank { "yourname" }) }, modifier = Modifier.fillMaxWidth())
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("I AM A…", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Roles.forEach { role -> PopChip(role, tagline == role, { tagline = if (tagline == role) "" else role }) }
            }
        }
        if (showError && !valid) Text("Enter your name (at least 2 characters) to continue.", color = Pop.Loss, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        NeoButton(submitLabel, Modifier.fillMaxWidth(), onClick = {
            if (valid) onSubmit(ProfileData(name.trim(), cleanHandle(handle, name), tagline, seed)) else showError = true
        })
        if (onCancel != null) NeoOutlineButton("Cancel", onCancel, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}

/** Compulsory first-run screen: nothing else in the app is reachable until a profile exists. */
@Composable
fun OnboardingScreen(onDone: (ProfileData) -> Unit) {
    val initial = remember { ProfileData("", "", "", java.util.Random().nextInt(1_000_000_000).toLong() + 1) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinCipMark(size = 36.dp)
                Text("FinCip", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.6).sp)
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WELCOME", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
                Text("Let's set up your profile.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("Your name and avatar stay on this device and are included in your backups.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f))
            }
            ProfileForm(initial, "Get started", onDone, Modifier.weight(1f))
        }
    }
}
