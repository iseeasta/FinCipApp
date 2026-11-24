package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.IndiaKind
import com.fincip.nativeapp.data.IndiaKinds
import com.fincip.nativeapp.data.IndiaMath
import com.fincip.nativeapp.data.money
import java.time.LocalDate

fun kindIcon(id: String): ImageVector = when (id) {
    "SIP" -> Icons.Default.Autorenew
    "Mutual fund" -> Icons.Default.PieChart
    "ELSS" -> Icons.Default.Savings
    "Stocks" -> Icons.Default.ShowChart
    "EPF" -> Icons.Default.Work
    "PPF" -> Icons.Default.AccountBalance
    "NPS" -> Icons.Default.BeachAccess
    "FD" -> Icons.Default.Lock
    "RD" -> Icons.Default.Event
    "NSC" -> Icons.Default.Description
    "Gold" -> Icons.Default.Diamond
    "SGB" -> Icons.Default.WorkspacePremium
    else -> Icons.Default.MoreHoriz
}

@Composable
private fun PfLabel(text: String) = Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))

@Composable
private fun PfChips(items: List<Pair<String, String>>, selectedValue: String? = null, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, value) -> PopChip(label, selectedValue == value, { onPick(value) }) }
    }
}

private fun blankZero(v: String): String = if (v == "0" || v == "0.0") "" else v

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, suffix: String? = null) {
    OutlinedTextField(
        blankZero(value), { onChange(it.filter { c -> c.isDigit() || c == '.' }) }, singleLine = true, label = { Text(label) },
        suffix = suffix?.let { s -> { Text(s) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Add / edit a finance product. Icon tiles pick the type, provider and amounts have one-tap chips,
 * dates use the date picker, and a live card shows an *illustrative* estimate of the value at maturity.
 */
@Composable
fun ProductFormBody(
    kind: String, onKind: (String) -> Unit, name: String, onName: (String) -> Unit, provider: String, onProvider: (String) -> Unit,
    invested: String, current: String, onCurrent: (String) -> Unit, monthly: String, onMonthly: (String) -> Unit,
    rate: String, onRate: (String) -> Unit, start: String, onStart: (String) -> Unit, maturity: String, onMaturity: (String) -> Unit,
    currency: String, existingProviders: List<String>
) {
    val k = IndiaKinds.of(kind)
    val scheme = MaterialTheme.colorScheme
    NeoCard {
        PfLabel("WHAT KIND OF INVESTMENT?")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IndiaKinds.all.forEach { item ->
                val on = item.id == k.id
                Column(
                    Modifier.width(78.dp).clip(RoundedCornerShape(18.dp)).background(if (on) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = .6f))
                        .clickable { onKind(item.id) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = .75f)), contentAlignment = Alignment.Center) {
                        Icon(kindIcon(item.id), null, tint = Pop.Ink, modifier = Modifier.size(22.dp))
                    }
                    Text(item.label, fontSize = 11.sp, fontWeight = if (on) FontWeight.Black else FontWeight.Medium, maxLines = 1, color = if (on) scheme.onPrimaryContainer else scheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(k.blurb, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .7f))
    }

    NeoCard {
        PfLabel("NAME & PROVIDER")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(name, onName, singleLine = true, label = { Text("Name") }, placeholder = { Text(if (k.id == "FD") "e.g. 1-year FD" else "e.g. ${k.label} for goals") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(provider, onProvider, singleLine = true, label = { Text("Provider / institution") }, leadingIcon = { Icon(Icons.Default.AccountBalance, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        val hints = (existingProviders + k.providerHints).distinct().filter { !it.equals(provider, true) }.take(12)
        if (hints.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            PfChips(hints.map { it to it }, provider, onProvider)
        }
    }

    NeoCard {
        PfLabel("MONEY")
        Spacer(Modifier.height(10.dp))
        NumberField("Current value (${currency})", current, onCurrent)
        Spacer(Modifier.height(6.dp))
        val inv = invested.toDoubleOrNull() ?: 0.0
        PfChips(
            buildList {
                if (inv > 0) add("Same as invested" to inv.toString())
                if (inv > 0) add("+5%" to (inv * 1.05).let { Math.round(it).toString() })
                if (inv > 0) add("+10%" to (inv * 1.10).let { Math.round(it).toString() })
            }
        ) { onCurrent(it) }
        if (k.recurring) {
            Spacer(Modifier.height(12.dp))
            NumberField("Monthly contribution (${currency})", monthly, onMonthly)
            Spacer(Modifier.height(6.dp))
            PfChips(listOf(500, 1000, 2000, 5000, 10000, 25000).map { "₹%,d".format(it) to it.toString() }, monthly.toDoubleOrNull()?.toLong()?.toString(), onMonthly)
        }
        Spacer(Modifier.height(12.dp))
        NumberField("Expected / current rate", rate, onRate, suffix = "% p.a.")
        Spacer(Modifier.height(6.dp))
        val common = listOfNotNull(k.defaultRate) + listOf(6.0, 8.0, 10.0, 12.0, 15.0)
        PfChips(common.distinct().map { "%s%%".format(if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()) to it.toString() }, rate.toDoubleOrNull()?.toString(), onRate)
        Text("Rates for deposits and small-savings schemes change. Check your statement or the provider for the real rate.", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(top = 6.dp))
    }

    NeoCard {
        PfLabel(if (k.hasMaturity) "DATES" else "DATES · MATURITY OPTIONAL")
        Spacer(Modifier.height(10.dp))
        DateField("Start date", start, onStart)
        Spacer(Modifier.height(12.dp))
        DateField("Maturity date", maturity, onMaturity)
        Spacer(Modifier.height(6.dp))
        val startDate = runCatching { LocalDate.parse(start) }.getOrNull() ?: LocalDate.now()
        PfChips(listOf(1, 3, 5, 10, 15).map { "$it yr" to startDate.plusYears(it.toLong()).toString() } + listOf("None" to ""), maturity, onMaturity)
    }

    // ---- Live estimate (illustrative, built only from what the user typed)
    val startDate = runCatching { LocalDate.parse(start) }.getOrNull()
    val matDate = runCatching { LocalDate.parse(maturity) }.getOrNull()
    val inv = invested.toDoubleOrNull() ?: 0.0
    val rt = rate.toDoubleOrNull() ?: 0.0
    val monthlyAmt = monthly.toDoubleOrNull() ?: 0.0
    val proj = if (startDate != null && matDate != null && rt > 0 && (inv > 0 || monthlyAmt > 0)) IndiaMath.projectedValue(k, inv, if (k.recurring) monthlyAmt else 0.0, rt, startDate, matDate) else null
    if (proj != null && startDate != null && matDate != null) {
        val months = IndiaMath.monthsBetween(startDate, matDate)
        val put = inv + if (k.recurring) monthlyAmt * months else 0.0
        NeoCard(fill = Pop.Mint) {
            Text("ESTIMATED AT MATURITY", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .65f))
            Text(money(proj, currency), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Pop.Ink, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            val frac = if (proj > 0) (put / proj).toFloat().coerceIn(0f, 1f) else 0f
            Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .6f))) {
                Box(Modifier.fillMaxHeight().weight(frac.coerceAtLeast(0.02f)).background(Pop.Forest))
                Box(Modifier.fillMaxHeight().weight((1f - frac).coerceAtLeast(0.02f)).background(Pop.Lime))
            }
            Spacer(Modifier.height(6.dp))
            Text("Put in ${money(put, currency)}  ·  growth ${money((proj - put).coerceAtLeast(0.0), currency)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Pop.Ink)
            IndiaMath.doublingYears(rt)?.let { Text("At ${rt}% money roughly doubles in $it years (rule of 72).", style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .75f)) }
            Text("Illustrative only, from the rate you typed. Not a guarantee, tax advice or a return promise.", style = MaterialTheme.typography.labelSmall, color = Pop.Ink.copy(alpha = .6f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}
