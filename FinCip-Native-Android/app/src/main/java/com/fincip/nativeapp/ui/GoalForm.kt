package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.GoalMath
import com.fincip.nativeapp.data.GoalPurposes
import com.fincip.nativeapp.data.money
import java.time.LocalDate

@Composable
private fun GfLabel(text: String) = Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))

@Composable
private fun GfChips(items: List<Pair<String, String>>, selected: String? = null, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, value) -> PopChip(label, selected == value, { onPick(value) }) }
    }
}

@Composable
private fun GfNumber(label: String, value: String, onChange: (String) -> Unit, suffix: String? = null) {
    val shown = if (value == "0" || value == "0.0") "" else value
    OutlinedTextField(
        shown, { onChange(it.filter { c -> c.isDigit() || c == '.' }) }, singleLine = true, label = { Text(label) },
        suffix = suffix?.let { s -> { Text(s) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)
    )
}

private fun cleanNum(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else "%.2f".format(d)

/** Goal / debt form body. The target is the hero amount in the dialog header, so it is not repeated here. */
@Composable
fun GoalFormBody(
    debt: Boolean, purpose: String, onPurpose: (String) -> Unit, name: String, onName: (String) -> Unit,
    target: String, saved: String, onSaved: (String) -> Unit, monthly: String, onMonthly: (String) -> Unit,
    rate: String, onRate: (String) -> Unit, deadline: String, onDeadline: (String) -> Unit, currency: String
) {
    val scheme = MaterialTheme.colorScheme
    val purposes = if (debt) GoalPurposes.debt else GoalPurposes.saving
    val activePurpose = purposes.firstOrNull { it == purpose } ?: "Other"
    val tgt = target.toDoubleOrNull() ?: 0.0
    val sv = saved.toDoubleOrNull() ?: 0.0
    val mo = monthly.toDoubleOrNull() ?: 0.0
    val apr = rate.toDoubleOrNull() ?: 0.0
    val remaining = (tgt - sv).coerceAtLeast(0.0)
    val today = LocalDate.now()
    val dl = runCatching { LocalDate.parse(deadline) }.getOrNull()

    NeoCard {
        GfLabel(if (debt) "WHAT KIND OF DEBT?" else "WHAT ARE YOU SAVING FOR?")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            purposes.forEach { p ->
                val on = p == activePurpose
                Column(
                    Modifier.width(82.dp).clip(RoundedCornerShape(18.dp)).background(if (on) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = .6f))
                        .clickable { onPurpose(p) }.padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = .75f)), contentAlignment = Alignment.Center) { Text(GoalPurposes.icon(p), fontSize = 20.sp) }
                    Text(p, fontSize = 11.sp, fontWeight = if (on) FontWeight.Black else FontWeight.Medium, maxLines = 1, color = if (on) scheme.onPrimaryContainer else scheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(name, onName, singleLine = true, label = { Text("Name") }, placeholder = { Text(if (debt) "e.g. HDFC card" else "e.g. Goa trip") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        val hints = GoalPurposes.suggestions(activePurpose).filter { !it.equals(name, true) }
        if (hints.isNotEmpty()) { Spacer(Modifier.height(6.dp)); GfChips(hints.map { it to it }, name, onName) }
    }

    NeoCard {
        GfLabel(if (debt) "HOW MUCH IS ALREADY REPAID?" else "HOW MUCH IS ALREADY SAVED?")
        Spacer(Modifier.height(10.dp))
        GfNumber(if (debt) "Repaid so far ($currency)" else "Saved so far ($currency)", saved, onSaved)
        if (tgt > 0) {
            Spacer(Modifier.height(6.dp))
            GfChips(listOf("None" to "0") + listOf(10, 25, 50).map { "$it%" to cleanNum(tgt * it / 100.0) }, saved.toDoubleOrNull()?.let(::cleanNum), onSaved)
            Spacer(Modifier.height(8.dp))
            ProgressBar((sv / tgt).toFloat().coerceIn(0f, 1f), if (debt) Pop.Coral else Pop.Forest)
            Text("${money(sv, currency)} of ${money(tgt, currency)}  ·  ${money(remaining, currency)} to go", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        } else {
            Text("Type the target in the big box at the top first.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = .6f), modifier = Modifier.padding(top = 6.dp))
        }
    }

    NeoCard {
        GfLabel(if (debt) "PAYOFF DATE" else "TARGET DATE")
        Spacer(Modifier.height(10.dp))
        DateField(if (debt) "Be debt-free by" else "Reach it by", deadline, onDeadline)
        Spacer(Modifier.height(6.dp))
        GfChips(listOf(3, 6, 12, 24, 36, 60).map { m -> (if (m < 12) "$m mo" else "${m / 12} yr") to today.plusMonths(m.toLong()).toString() } + listOf("None" to ""), deadline, onDeadline)
    }

    NeoCard {
        GfLabel(if (debt) "MONTHLY REPAYMENT" else "MONTHLY CONTRIBUTION")
        Spacer(Modifier.height(10.dp))
        GfNumber(if (debt) "Pay each month ($currency)" else "Save each month ($currency)", monthly, onMonthly)
        Spacer(Modifier.height(6.dp))
        val needed = dl?.let { d -> val m = GoalMath.monthsUntil(d, today); if (m > 0 && remaining > 0) GoalMath.requiredMonthly(remaining, m, if (debt) 0.0 else 0.0) else null }
        GfChips(
            buildList {
                if (needed != null && !debt) add("Needed ${money(Math.ceil(needed), currency)}" to Math.ceil(needed).toLong().toString())
                listOf(500, 1000, 2000, 5000, 10000, 25000).forEach { add("%,d".format(it) to it.toString()) }
            }, monthly.toDoubleOrNull()?.toLong()?.toString(), onMonthly
        )
        if (debt) {
            Spacer(Modifier.height(12.dp))
            GfNumber("Interest rate", rate, onRate, suffix = "% p.a.")
            Spacer(Modifier.height(6.dp))
            GfChips(listOf(0, 8, 12, 18, 24, 36, 42).map { "$it%" to it.toString() }, rate.toDoubleOrNull()?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }, onRate)
            Text("Credit cards often charge 36-42% a year. Check your statement for the real number.", style = MaterialTheme.typography.labelSmall, color = scheme.onSurface.copy(alpha = .55f), modifier = Modifier.padding(top = 6.dp))
        }
    }

    // ---- Live plan, computed only from what was typed
    if (tgt > 0 && remaining > 0) {
        NeoCard(fill = if (debt) Pop.Peach else Pop.Mint) {
            Text(if (debt) "YOUR PAYOFF PLAN" else "YOUR PLAN", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Pop.Ink.copy(alpha = .65f))
            Spacer(Modifier.height(6.dp))
            if (debt) {
                val plan = GoalMath.simulateDebt(remaining, mo, apr)
                when {
                    mo <= 0 -> Text("Add a monthly repayment to see when you'd be free.", color = Pop.Ink, fontWeight = FontWeight.Bold)
                    plan == null -> Text("At ${money(mo, currency)} a month you pay less than the interest, so the balance never shrinks. Raise the payment.", color = Pop.Ink, fontWeight = FontWeight.Bold)
                    else -> {
                        val free = GoalMath.dateAfter(plan.months, today)
                        Text("Debt-free by ${free.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${free.year}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Pop.Ink)
                        Text("${plan.months} months  ·  interest ≈ ${money(plan.totalInterest, currency)}", color = Pop.Ink, fontWeight = FontWeight.Bold)
                        val more = GoalMath.simulateDebt(remaining, mo * 1.2, apr)
                        if (more != null && more.months < plan.months) Text("Pay 20% more and you finish ${plan.months - more.months} months sooner and save ≈ ${money(plan.totalInterest - more.totalInterest, currency)} interest.", style = MaterialTheme.typography.bodySmall, color = Pop.Ink.copy(alpha = .8f), modifier = Modifier.padding(top = 4.dp))
                        if (dl != null) {
                            val need = GoalMath.monthsUntil(dl, today)
                            if (need > 0 && plan.months > need) Text("That misses your ${dl} date by ${plan.months - need} months.", style = MaterialTheme.typography.bodySmall, color = Pop.Ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            } else {
                val eta = GoalMath.monthsToSave(remaining, mo)
                if (eta == null) Text("Add a monthly amount to see when you'd get there.", color = Pop.Ink, fontWeight = FontWeight.Bold)
                else {
                    val d = GoalMath.dateAfter(eta, today)
                    Text("Done by ${d.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${d.year}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Pop.Ink)
                    Text("$eta months at ${money(mo, currency)} a month", color = Pop.Ink, fontWeight = FontWeight.Bold)
                }
                if (dl != null) {
                    val m = GoalMath.monthsUntil(dl, today)
                    if (m > 0) {
                        val need = GoalMath.requiredMonthly(remaining, m)
                        val ok = mo >= need - 0.5
                        Text(if (ok) "On track for ${dl}." else "To hit ${dl} you need ${money(Math.ceil(need), currency)} a month, ${money(Math.ceil(need - mo), currency)} more than now.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Pop.Ink, modifier = Modifier.padding(top = 4.dp))
                    } else Text("That date has passed. Pick a later one.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Pop.Ink, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Text("Estimate from the numbers you entered. No returns assumed on savings.", style = MaterialTheme.typography.labelSmall, color = Pop.Ink.copy(alpha = .6f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}
