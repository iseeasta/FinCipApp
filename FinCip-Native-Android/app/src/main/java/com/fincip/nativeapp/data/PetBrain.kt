package com.fincip.nativeapp.data

/** Everything the companion may comment on. Amounts arrive pre-formatted so this stays pure and testable. */
data class PetCtx(
    val name: String = "Mochi",
    val hour: Int = 12,
    val dayOfMonth: Int = 15,
    val daysLeftInMonth: Int = 15,
    val txCount: Int = 0,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val incomeStr: String = "",
    val expensesStr: String = "",
    val balanceStr: String = "",
    val savingsRate: Double = 0.0,
    val topCategory: String = "",
    val topCategoryShare: Int = 0,
    val topCategoryAmtStr: String = "",
    val streak: Int = 0,
    val daysSinceLastEntry: Int? = null,
    val billName: String = "",
    val billDays: Int? = null,
    val billAmtStr: String = "",
    val goalName: String = "",
    val goalPct: Int = -1,
    val overBudgetCategory: String = "",
    val debtCount: Int = 0,
    val debtName: String = "",
    val takeoutOver: Boolean = false
)

/** Writes the companion's lines: real numbers from the user's data first, then harmless nonsense. */
object PetBrain {
    private val nonsense = listOf(
        "I counted all your coins twice. One of them was a button.",
        "Is a samosa an asset or a liability? Asking for a friend.",
        "I tried to budget my naps. Over budget again.",
        "Compound interest is just patience wearing a suit.",
        "My net worth is mostly vibes and one very good sock.",
        "I dreamt of a spreadsheet. It was beautiful. I cried a little.",
        "Fun fact: inflation is not my fault. I just live here.",
        "If money grew on trees I'd be a forest. Anyway, log your spends.",
        "Somewhere a credit card is crying. Hopefully not yours.",
        "I stared at the ledger so long it blinked first.",
        "Tip: chai is a recurring expense with a personality.",
        "I don't always save money, but when I do, I tell everyone.",
        "Rich people also forget where their money went. They just forget it in bigger numbers.",
        "Breaking news: you are doing better than the version of you from last year."
    )

    fun lines(c: PetCtx): List<String> {
        val out = mutableListOf<String>()
        val n = c.name
        when {
            c.hour in 5..10 -> out += "Good morning! Did you log yesterday's chai? I'm watching. Kindly."
            c.hour in 11..16 -> out += "Lunch hour. Reminder: delivery fees are tiny taxes on laziness."
            c.hour in 17..21 -> out += "Evening check-in. Anything you bought today that you forgot to log?"
            else -> out += "It's late. Impulse buys love midnight. Put the phone down, hero."
        }
        if (c.txCount == 0) {
            out += listOf("Your ledger is empty and so is my snack bowl. Log one transaction?", "Blank slate. I could do this all day. Feed me an expense.", "Hi, I'm $n. I run on transactions. Please.")
            return (out + nonsense.shuffled().take(2)).distinct()
        }
        if (c.income <= 0 && c.expenses > 0) out += "You've spent ${c.expensesStr} but no income is logged. Add your salary so I can do real math."
        if (c.income > 0 && c.expenses > c.income) out += "Spent ${c.expensesStr} against ${c.incomeStr} income. There is a sweat drop on my forehead. That's not decoration."
        if (c.income > 0 && c.expenses <= c.income) {
            if (c.savingsRate >= 20) out += "Saving ${c.savingsRate.toInt()}% this month. I'd hug your wallet. Do I have arms? Yes. Yes I do."
            else if (c.savingsRate > 0) out += "Saving ${c.savingsRate.toInt()}% so far. Target is 20. We're on the road, just in slippers."
            else out += "Income and spending are neck and neck. Tiny trims would tip it in our favour."
        }
        if (c.topCategory.isNotBlank() && c.topCategoryShare >= 25) out += "${c.topCategory} ate ${c.topCategoryShare}% of your spending (${c.topCategoryAmtStr}). It's the main character this month."
        if (c.streak >= 3) out += "${c.streak} days in a row! I told the other pets. They didn't care. I did."
        if (c.streak == 0 && (c.daysSinceLastEntry ?: 0) >= 3) out += "${c.daysSinceLastEntry} days of silence. Did you stop spending or stop writing it down? Be honest."
        if (c.billDays != null && c.billDays in 0..5 && c.billName.isNotBlank()) {
            out += if (c.billDays == 0) "${c.billName} is due TODAY (${c.billAmtStr}). Go. I'll wait. Dramatically." else "${c.billName} is due in ${c.billDays} day${if (c.billDays == 1) "" else "s"} (${c.billAmtStr}). I'll remind you twice. This was once."
        }
        if (c.goalPct in 1..99) out += "${c.goalName.ifBlank { "Your goal" }} is ${c.goalPct}% funded. Every little push counts. Even the lazy ones."
        if (c.goalPct >= 100) out += "${c.goalName.ifBlank { "A goal" }} is fully funded. I'm throwing imaginary confetti."
        if (c.overBudgetCategory.isNotBlank()) out += "The ${c.overBudgetCategory} budget is blown. I'm not mad. Just quietly disappointed, in a cute way."
        if (c.takeoutOver) out += "Takeout crossed the food budget. Instant noodles are calling. They sound smug."
        if (c.debtCount > 0 && c.debtName.isNotBlank()) out += "${c.debtName} is still out there. One extra payment makes it nervous."
        if (c.dayOfMonth <= 3) out += "New month, clean slate. Please don't ruin it by Tuesday."
        if (c.daysLeftInMonth in 0..3 && c.expenses <= c.income) out += "${c.daysLeftInMonth} days left in the month and you're still standing. Mostly."
        val situational = out.take(1) + out.drop(1).shuffled()
        return (situational + nonsense.shuffled().take(3)).distinct()
    }
}
