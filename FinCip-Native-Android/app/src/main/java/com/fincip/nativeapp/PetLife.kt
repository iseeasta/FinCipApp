package com.fincip.nativeapp

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincip.nativeapp.data.PetBrain
import com.fincip.nativeapp.data.PetCtx
import com.fincip.nativeapp.data.arrayOrEmpty
import com.fincip.nativeapp.data.currentMonth
import com.fincip.nativeapp.data.double
import com.fincip.nativeapp.data.expenseTotal
import com.fincip.nativeapp.data.incomeTotal
import com.fincip.nativeapp.data.money
import com.fincip.nativeapp.data.monthlyTransactions
import com.fincip.nativeapp.data.objectOrEmpty
import com.fincip.nativeapp.data.objects
import com.fincip.nativeapp.data.string
import com.fincip.nativeapp.data.toBase
import com.fincip.nativeapp.data.transactions
import com.fincip.nativeapp.ui.PetCharacter
import com.fincip.nativeapp.ui.PetLook
import com.fincip.nativeapp.ui.PetMood
import com.fincip.nativeapp.ui.PetOptions
import com.fincip.nativeapp.ui.petStage
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Turns the saved workspace into the facts the companion can talk about. */
fun buildPetCtx(workspace: JsonObject, name: String, streak: Int): PetCtx {
    val base = workspace.objectOrEmpty("settings").string("baseCurrency", "INR")
    val month = currentMonth()
    val today = LocalDate.now()
    val income = incomeTotal(workspace, month)
    val expenses = expenseTotal(workspace, month)
    val spendByCat = monthlyTransactions(workspace, month).filter { com.fincip.nativeapp.data.isSpend(it) }
        .groupBy { it.string("category", "Other") }.mapValues { (_, v) -> v.sumOf { toBase(workspace, it.double("amount"), it.string("currency", "INR")) } }
    val top = spendByCat.maxByOrNull { it.value }
    val lastEntry = transactions(workspace).mapNotNull { runCatching { LocalDate.parse(it.string("date")) }.getOrNull() }.maxOrNull()
    val bill = workspace.arrayOrEmpty("recurringRules").objects().filter { it.string("type") != "income" }
        .mapNotNull { r -> runCatching { LocalDate.parse(r.string("nextDate")) }.getOrNull()?.let { r to ChronoUnit.DAYS.between(today, it).toInt() } }
        .filter { it.second >= 0 }.minByOrNull { it.second }
    val goals = workspace.arrayOrEmpty("goals").objects()
    val savingsGoal = goals.filter { it.string("type") != "debt" && it.double("target") > 0 }.maxByOrNull { it.double("saved") / it.double("target") }
    val debt = goals.firstOrNull { it.string("type") == "debt" }
    val overBudget = workspace.arrayOrEmpty("budgets").objects().firstOrNull { b ->
        val spent = spendByCat[b.string("category")] ?: 0.0
        b.double("limit") > 0 && spent > toBase(workspace, b.double("limit"), b.string("currency", base))
    }
    val ym = YearMonth.now()
    return PetCtx(
        name = name, hour = java.time.LocalTime.now().hour, dayOfMonth = today.dayOfMonth, daysLeftInMonth = ym.lengthOfMonth() - today.dayOfMonth,
        txCount = transactions(workspace).size, income = income, expenses = expenses,
        incomeStr = money(income, base), expensesStr = money(expenses, base), balanceStr = money(income - expenses, base),
        savingsRate = if (income > 0) (income - expenses) / income * 100 else 0.0,
        topCategory = top?.key.orEmpty(), topCategoryShare = if (top != null && expenses > 0) (top.value / expenses * 100).toInt() else 0, topCategoryAmtStr = top?.let { money(it.value, base) }.orEmpty(),
        streak = streak, daysSinceLastEntry = lastEntry?.let { ChronoUnit.DAYS.between(it, today).toInt() },
        billName = bill?.first?.string("title").orEmpty(), billDays = bill?.second, billAmtStr = bill?.first?.let { money(it.double("amount"), it.string("currency", base)) }.orEmpty(),
        goalName = savingsGoal?.string("title", savingsGoal.string("name")).orEmpty(),
        goalPct = savingsGoal?.let { (it.double("saved") / it.double("target") * 100).toInt() } ?: -1,
        overBudgetCategory = overBudget?.string("category").orEmpty(),
        debtCount = goals.count { it.string("type") == "debt" }, debtName = debt?.string("title", debt.string("name")).orEmpty()
    )
}

private fun floatersFor(mood: PetMood): List<String> = when (mood) {
    PetMood.THRIVING -> listOf("💰", "✨", "📈")
    PetMood.CELEBRATE -> listOf("🎉", "⭐", "🎊")
    PetMood.STRESSED -> listOf("💸", "😰", "📉")
    PetMood.RAMEN -> listOf("🍜", "💨", "🥢")
    else -> listOf("🎵", "✨", "💛")
}

/** The companion with life in it: bobbing, swaying, a waving arm and a mouth that moves while it talks, plus floating mood emoji. */
@Composable
fun LivingPet(look: PetLook, mood: PetMood, level: Int, talking: Boolean, hopKey: Int, diameter: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "life")
    val sway by t.animateFloat(-2.2f, 2.2f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wave")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "drift")
    val hop = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(hopKey) {
        if (hopKey > 0) repeat(2) { hop.animateTo(-1f, tween(130)); hop.animateTo(0f, tween(170)) }
    }
    val density = LocalDensity.current
    val d = diameter.value
    val arm = if (look.species == 7) null else {
        val animal = look.species >= 2
        val skin = if (look.species == 6) Color(0xFFF5F0E6) else if (animal) PetOptions.furs[look.skin.coerceIn(0, PetOptions.furs.lastIndex)] else PetOptions.skins[look.skin.coerceIn(0, PetOptions.skins.lastIndex)]
        skin to PetOptions.outfitColors[look.outfitColor.coerceIn(0, PetOptions.outfitColors.lastIndex)]
    }
    val armUp = talking || mood == PetMood.CELEBRATE || mood == PetMood.THRIVING
    Box(modifier.size(diameter)) {
        Box(Modifier.size(diameter).graphicsLayer {
            translationY = hop.value * with(density) { (d * 0.1f).dp.toPx() }
            rotationZ = sway; transformOrigin = TransformOrigin(0.5f, 1f)
        }) {
            PetCharacter(look, mood, level, diameter = diameter, showBackground = false, talking = talking)
            if (arm != null && armUp && mood != PetMood.RAMEN) Canvas(Modifier.size(diameter)) {
                val s = size.minDimension
                val shoulder = Offset(s * 0.70f, s * 0.76f)
                val ang = (-62f + (wave - .5f) * 44f) * (PI.toFloat() / 180f)
                val len = s * 0.22f
                val hand = Offset(shoulder.x + cos(ang) * len, shoulder.y + sin(ang) * len)
                drawLine(arm.second, shoulder, hand, strokeWidth = s * 0.085f, cap = StrokeCap.Round)
                drawCircle(arm.first, s * 0.052f, hand)
            }
        }
        // floating mood emoji
        floatersFor(mood).forEachIndexed { i, emoji ->
            val phase = (drift + i / 3f) % 1f
            val alpha = sin(phase * PI.toFloat()).coerceIn(0f, 1f)
            Text(
                emoji, fontSize = (d * 0.14f).sp,
                modifier = Modifier.offset { IntOffset(((0.1f + 0.28f * i + 0.03f * sin(phase * 7f)) * d * density.density).toInt(), ((0.5f - 0.5f * phase) * d * density.density).toInt()) }
                    .graphicsLayer { this.alpha = alpha * 0.9f }
            )
        }
    }
}

/** Speech bubble with a tail on the left and a typewriter effect. [onTyping] reports whether the pet is still talking. */
@Composable
fun PetSpeech(line: String, bubble: Color, ink: Color, onTyping: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(line) { mutableStateOf(0) }
    LaunchedEffect(line) {
        shown = 0; onTyping(true)
        while (shown < line.length) { kotlinx.coroutines.delay(26); shown++ }
        onTyping(false)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).offset(x = 6.dp).rotate(45f).background(bubble))
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(bubble).padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(line, color = Color.Transparent, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(line.take(shown), color = ink, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
