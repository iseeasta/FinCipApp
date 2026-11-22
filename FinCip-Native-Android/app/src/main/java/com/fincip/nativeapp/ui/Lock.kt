package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.AppLock
import kotlinx.coroutines.delay

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, onDone: (() -> Unit)? = null) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(), singleLine = true, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (onDone != null) androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Next),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone?.invoke() }),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Pop.RadiusSmall)
    )
}

/** Full-screen lock. Unlocks with the app password or, when enabled, the device fingerprint/face sheet. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var pw by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var fails by remember { mutableIntStateOf(0) }
    var cooldown by remember { mutableIntStateOf(0) }
    val bio = remember { AppLock.biometricEnabled(context) && AppLock.biometricAvailable(context) }
    fun askBiometric() = AppLock.authenticate(context, "Unlock FinCip") { ok -> if (ok) onUnlocked() }
    LaunchedEffect(Unit) { if (bio) askBiometric() }
    LaunchedEffect(cooldown) { if (cooldown > 0) { delay(1000); cooldown -= 1 } }
    fun submit() {
        if (cooldown > 0) return
        if (AppLock.verify(context, pw)) onUnlocked() else {
            fails += 1; pw = ""
            if (fails % 5 == 0) { cooldown = 30; error = "Too many attempts." } else error = "That password is not right."
        }
    }
    val tap = rememberTap()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(40.dp))
        FinCipMark(size = 84.dp)
        Spacer(Modifier.height(16.dp))
        Text("FinCip is locked", fontWeight = FontWeight.Black, fontSize = 26.sp, color = MaterialTheme.colorScheme.onBackground)
        Text("Enter your app password to continue.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        PasswordField(pw, { pw = it; error = "" }, "App password", onDone = { tap(); submit() })
        if (cooldown > 0) Text("Try again in ${cooldown}s", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        else if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        NeoButton("Unlock", Modifier.fillMaxWidth(), onClick = { submit() }, enabled = pw.isNotEmpty() && cooldown == 0)
        if (bio) {
            Spacer(Modifier.height(10.dp))
            NeoOutlineButton("Use fingerprint", { askBiometric() }, Modifier.fillMaxWidth())
        }
    }
}
