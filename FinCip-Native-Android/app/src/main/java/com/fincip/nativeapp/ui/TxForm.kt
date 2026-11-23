package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Values already used in the user's data, offered as one-tap suggestions so nothing has to be typed twice. */
data class FormSuggestions(val projects: List<String> = emptyList(), val tags: List<String> = emptyList(), val people: List<String> = emptyList(), val accounts: List<String> = emptyList())

val EXPENSE_CATEGORIES = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Education", "Entertainment", "Travel", "Home", "Subscriptions", "Personal care", "EMI", "Investment", "Gift", "Other")
val INCOME_CATEGORIES = listOf("Salary", "Freelance", "Business", "Investment", "Gift", "Refund", "Other")
private val PAYMENT_METHODS: List<Pair<String, ImageVector>> = listOf(
    "UPI" to Icons.Default.Smartphone, "Net banking" to Icons.Default.AccountBalance, "Cash" to Icons.Default.Payments,
    "Card" to Icons.Default.CreditCard, "Wallet" to Icons.Default.AccountBalanceWallet, "Other" to Icons.Default.MoreHoriz
)
private val DEFAULT_TAGS = listOf("recurring", "work", "personal", "family", "trip", "essential")

@Composable
private fun SectionLabel(text: String) = Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))

@Composable
private fun SuggestionRow(items: List<String>, selected: Set<String> = emptySet(), onPick: (String) -> Unit) {
    if (items.isEmpty()) return
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { PopChip(it, it in selected, { onPick(it) }) }
    }
}

/** Category picker made of icon tiles, so the category you pick is the icon you will see in history. */
@Composable
fun IconCategoryPicker(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val list = if (selected.isNotBlank() && selected !in options) listOf(selected) + options else options
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { c ->
            val on = c.equals(selected, true)
            Column(
                Modifier.width(74.dp).clip(RoundedCornerShape(18.dp)).background(if (on) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = .6f)).clickable { onSelect(c) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryBadge(c, 40.dp)
                Text(c, fontSize = 11.sp, fontWeight = if (on) FontWeight.Black else FontWeight.Medium, maxLines = 1, color = if (on) scheme.onPrimaryContainer else scheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            OutlinedTextField(value, {}, readOnly = true, singleLine = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
            Box(Modifier.matchParentSize().clickable { open = true })
        }
        val today = LocalDate.now()
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PopChip("Today", value == today.toString(), { onChange(today.toString()) })
            PopChip("Yesterday", value == today.minusDays(1).toString(), { onChange(today.minusDays(1).toString()) })
            PopChip("Pick date", false, { open = true })
        }
    }
    if (open) {
        val initial = runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() ?: System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton({ state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }; open = false }) { Text("Set date") } },
            dismissButton = { TextButton({ open = false }) { Text("Cancel") } }
        ) { DatePicker(state) }
    }
}

@Composable
fun DropdownField(label: String, value: String, options: List<Pair<String, ImageVector?>>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val icon = options.firstOrNull { it.first.equals(value, true) }?.second
    Box(modifier) {
        OutlinedTextField(value, {}, readOnly = true, singleLine = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            leadingIcon = icon?.let { ic -> { Icon(ic, null) } }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
        Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(open, { open = false }) {
            options.forEach { (name, ic) ->
                DropdownMenuItem(text = { Text(name) }, leadingIcon = ic?.let { i -> { Icon(i, null) } }, onClick = { open = false; onSelect(name) })
            }
        }
    }
}

/** The whole transaction form body: three grouped cards (what / when & how / organise). */
@Composable
fun TransactionFormBody(
    income: Boolean, name: String, onName: (String) -> Unit, category: String, onCategory: (String) -> Unit,
    date: String, onDate: (String) -> Unit, account: String, onAccount: (String) -> Unit, person: String, onPerson: (String) -> Unit,
    project: String, onProject: (String) -> Unit, tags: String, onTags: (String) -> Unit, extra: String, onExtra: (String) -> Unit,
    suggestions: FormSuggestions
) {
    val tagList = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    NeoCard {
        SectionLabel("WHAT WAS IT?")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(name, onName, singleLine = true, label = { Text(if (income) "Source / description" else "Name / description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        Spacer(Modifier.height(12.dp))
        IconCategoryPicker(if (income) INCOME_CATEGORIES else EXPENSE_CATEGORIES, category, onCategory)
    }
    NeoCard {
        SectionLabel("WHEN & HOW")
        Spacer(Modifier.height(10.dp))
        DateField("Date", date, onDate)
        Spacer(Modifier.height(12.dp))
        val methods = PAYMENT_METHODS + suggestions.accounts.filter { a -> PAYMENT_METHODS.none { it.first.equals(a, true) } }.map { it to (null as ImageVector?) }
        DropdownField("Payment method", account, methods, onAccount)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(person, onPerson, singleLine = true, label = { Text(if (income) "Received from" else "Paid to") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        Spacer(Modifier.height(6.dp))
        SuggestionRow(suggestions.people.filter { !it.equals(person, true) }.take(10), emptySet(), onPerson)
    }
    NeoCard {
        SectionLabel("ORGANISE · OPTIONAL")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(project, onProject, singleLine = true, label = { Text("Project") }, leadingIcon = { Icon(Icons.Default.Folder, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        Spacer(Modifier.height(6.dp))
        SuggestionRow(suggestions.projects.take(10), setOf(project), onProject)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(tags, onTags, singleLine = true, label = { Text("Tags") }, placeholder = { Text("tap below or type, comma separated") }, leadingIcon = { Icon(Icons.Default.Sell, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        Spacer(Modifier.height(6.dp))
        SuggestionRow((suggestions.tags + DEFAULT_TAGS).distinct().take(14), tagList.toSet()) { t ->
            onTags((if (t in tagList) tagList - t else tagList + t).joinToString(", "))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(extra, onExtra, label = { Text("Split across categories") }, placeholder = { Text("Food:250, Travel:100") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
    }
}
