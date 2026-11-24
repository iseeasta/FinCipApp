package com.fincip.nativeapp.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.BuildConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

/**
 * Voluntary support. The payee details come from BuildConfig (injected at build time, never committed) and are
 * never drawn on screen. UPI gives the app no trustworthy payment result, so nothing here ever claims a payment was verified.
 */
object SupportConfig {
    val upiId: String get() = BuildConfig.SUPPORT_UPI_ID
    val payeeName: String get() = BuildConfig.SUPPORT_PAYEE_NAME
    val configured: Boolean get() = upiId.contains("@") && payeeName.isNotBlank()
    const val NOTE = "Support FinCip"
    const val MIN = 5.0
    const val MAX = 100000.0
    val presets = listOf(5, 10, 20, 50, 100, 200)

    fun amountText(a: Double): String = if (a % 1.0 == 0.0) a.toLong().toString() else String.format(Locale.US, "%.2f", a)

    /** Standard UPI deep link. Values are URL-encoded by Uri.Builder. */
    fun link(amount: Double): Uri = Uri.Builder().scheme("upi").authority("pay")
        .appendQueryParameter("pa", upiId).appendQueryParameter("pn", payeeName)
        .appendQueryParameter("am", amountText(amount)).appendQueryParameter("cu", "INR").appendQueryParameter("tn", NOTE).build()

    /** Returns an error message, or null when the amount is acceptable. */
    fun validate(text: String): String? {
        val v = text.trim().toDoubleOrNull() ?: return "Enter an amount"
        if (!Regex("^\\d{1,6}(\\.\\d{1,2})?$").matches(text.trim())) return "Use up to 2 decimals"
        if (v < MIN) return "Minimum is ₹${MIN.toInt()}"
        if (v > MAX) return "Maximum is ₹${MAX.toInt()}"
        return null
    }
}

private fun qrBitmap(content: String, px: Int = 640): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
    val m = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px, hints)
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    for (x in 0 until px) for (y in 0 until px) bmp.setPixel(x, y, if (m[x, y]) AColor.BLACK else AColor.WHITE)
    return bmp
}

@Composable
fun SupportCard() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var picked by rememberSaveable { mutableStateOf<Int?>(50) }
    var custom by rememberSaveable { mutableStateOf("") }
    var showQr by rememberSaveable { mutableStateOf(false) }
    var initiated by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    val customMode = picked == null
    val error = if (customMode) SupportConfig.validate(custom) else null
    val amount: Double? = if (customMode) (if (error == null) custom.trim().toDoubleOrNull() else null) else picked?.toDouble()
    val ready = SupportConfig.configured && amount != null

    NeoCard {
        Text("SUPPORT THE DEVELOPER", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black, color = scheme.onSurface.copy(alpha = .55f))
        Spacer(Modifier.height(4.dp))
        Text("A little support goes a long way", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("FinCip is built with passion. If you enjoy using it, consider supporting its continued development. Every contribution helps it grow.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface.copy(alpha = .75f))
        Spacer(Modifier.height(14.dp))
        Text("Choose your contribution", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val options: List<Int?> = SupportConfig.presets + listOf(null)
        options.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o ->
                    val on = picked == o
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = .6f))
                            .clickable { picked = o; initiated = false }.padding(vertical = 14.dp), contentAlignment = Alignment.Center
                    ) { Text(if (o == null) "Custom" else "₹$o", fontWeight = if (on) FontWeight.Black else FontWeight.Bold, color = if (on) scheme.onPrimaryContainer else scheme.onSurface) }
                }
            }
        }
        if (customMode) {
            OutlinedTextField(custom, { v -> custom = v.filter { it.isDigit() || it == '.' }.take(9); initiated = false }, singleLine = true, label = { Text("Amount in ₹ (min 5)") },
                isError = custom.isNotEmpty() && error != null, supportingText = { if (custom.isNotEmpty() && error != null) Text(error) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
            Spacer(Modifier.height(8.dp))
        }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(if (ready) scheme.primary else scheme.onSurface.copy(alpha = .12f))
                .clickable(enabled = ready) {
                    val a = amount ?: return@clickable
                    try {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, SupportConfig.link(a)), "Pay with"))
                        initiated = true; notice = ""
                    } catch (_: ActivityNotFoundException) { showQr = true; notice = "No UPI app was found on this phone. Scan the QR from another phone instead." }
                }.padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (amount != null) "Contribute ₹${SupportConfig.amountText(amount)} with UPI" else "Contribute with UPI", fontWeight = FontWeight.Black, color = if (ready) scheme.onPrimary else scheme.onSurface.copy(alpha = .4f))
                FinCipMark(size = 22.dp, glyph = if (ready) scheme.onPrimary else scheme.onSurface.copy(alpha = .4f))
            }
        }
        if (!SupportConfig.configured) Text("Support payments are not set up in this build yet.", fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(6.dp))
        Text(if (showQr) "Hide QR code" else "Show QR code instead", Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50)).clickable(enabled = ready) { showQr = !showQr }.padding(10.dp),
            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (ready) scheme.primary else scheme.onSurface.copy(alpha = .35f))
        if (notice.isNotBlank()) Text(notice, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (showQr && ready) {
            val bmp = remember(amount) { qrBitmap(SupportConfig.link(amount!!).toString()).asImageBitmap() }
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Image(bmp, "UPI QR code", Modifier.size(210.dp).clip(RoundedCornerShape(16.dp)).background(androidx.compose.ui.graphics.Color.White).padding(8.dp))
            }
            Text("Scan with any UPI app from another phone.", fontSize = 12.sp, textAlign = TextAlign.Center, color = scheme.onSurface.copy(alpha = .65f), modifier = Modifier.fillMaxWidth())
        }
        Text("One-time contribution · No subscription · Entirely voluntary", fontSize = 11.sp, textAlign = TextAlign.Center, color = scheme.onSurface.copy(alpha = .55f), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
    if (initiated) {
        NeoCard(fill = Pop.Mint) {
            Text("Thank you for supporting FinCip!", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Pop.Ink)
            Spacer(Modifier.height(4.dp))
            Text("Thank you! If you've completed your UPI payment, we appreciate your support. Your contribution helps us continue building a better financial experience.", color = Pop.Ink, fontSize = 13.sp)
            Text("FinCip cannot confirm UPI payments, so nothing is recorded as received.", color = Pop.Ink.copy(alpha = .6f), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = scheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Every contribution helps", fontWeight = FontWeight.Bold)
            listOf("Build better features" to "Continuous development", "Keep FinCip running" to "Testing and maintenance", "Bring new ideas to life" to "Future improvements").forEach { (a, b) ->
                Column { Text(a, fontWeight = FontWeight.Medium); Text(b, fontSize = 12.sp, color = scheme.onSurface.copy(alpha = .6f)) }
            }
        }
    }
}
