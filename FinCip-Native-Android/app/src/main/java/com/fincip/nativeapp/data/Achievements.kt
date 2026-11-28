package com.fincip.nativeapp.data

import com.google.gson.JsonObject
import java.time.DayOfWeek
import java.time.LocalDate

/** One achievement with three stages. Targets are per stage; [glyph] and [shape] pick its badge art. */
data class AchDef(val id: Int, val title: String, val tpl: String, val metric: String, val t1: Double, val t2: Double, val t3: Double, val glyph: Int, val shape: Int, val hue: Int) {
    fun target(stage: Int) = when (stage) { 0 -> t1; 1 -> t2; else -> t3 }
    fun text(stage: Int): String = tpl.replace("{n}", fmt(target(stage)))
    private fun fmt(v: Double) = if (v >= 1000) { val k = v / 1000; if (k == k.toLong().toDouble()) "${k.toLong()}k" else "%.1fk".format(k) } else v.toLong().toString()
}

/** [tier] is how many stages are done, 0..3. */
data class AchState(val def: AchDef, val value: Double, val tier: Int) {
    val done get() = tier >= 3
    val nextTarget get() = if (tier >= 3) def.t3 else def.target(tier)
    val progress get() = if (tier >= 3) 1f else (value / nextTarget).toFloat().coerceIn(0f, 1f)
}

object Achievements {
    const val GLYPHS = 25
    const val SHAPES = 10

    private class B(val title: String, val tpl: String, val metric: String, val a: Double, val b: Double, val c: Double, val glyph: Int)
    private fun b(title: String, tpl: String, metric: String, a: Number, bb: Number, c: Number, glyph: Int) = B(title, tpl, metric, a.toDouble(), bb.toDouble(), c.toDouble(), glyph)

    // glyph ids: 0 coin 1 leaf 2 flame 3 target 4 chart 5 book 6 key 7 lock 8 crown 9 rocket 10 heart 11 gem 12 clock 13 calendar 14 flag 15 bag 16 card 17 wallet 18 bank 19 piggy 20 bolt 21 globe 22 scroll 23 trophy 24 star
    private val raw = listOf(
        b("First Steps", "Log {n} entries", "tx", 1, 10, 50, 1), b("Ledger Keeper", "Log {n} entries", "tx", 100, 300, 1000, 5),
        b("Spender's Log", "Log {n} expenses", "exp", 10, 100, 500, 15), b("Income Tracker", "Log {n} incomes", "inc", 3, 25, 100, 0),
        b("Daily Habit", "Log on {n} different days", "days", 3, 30, 100, 13), b("Streak Spark", "Reach a {n}-day streak", "streakBest", 3, 14, 45, 2),
        b("Unbroken", "Reach a {n}-day streak", "streakBest", 60, 120, 365, 20), b("Busy Day", "Log {n} entries in one day", "perDay", 3, 6, 12, 12),
        b("Weekend Warrior", "Log {n} weekend entries", "weekend", 5, 25, 100, 24), b("Payday Planner", "Log {n} entries in the last days of a month", "monthEnd", 5, 20, 60, 13),
        b("Category Explorer", "Use {n} different categories", "cats", 3, 8, 15, 21), b("World Wallet", "Use {n} currencies", "curr", 2, 3, 5, 21),
        b("Month Marathon", "Log across {n} different months", "months", 2, 6, 12, 13), b("Long Game", "Keep a {n}-day history", "span", 30, 180, 365, 12),
        b("Payee Network", "Record {n} different payees", "payees", 5, 30, 100, 22), b("Split Master", "Split {n} transactions", "splits", 1, 5, 25, 22),
        b("Many Incomes", "Have {n} different income sources", "incSources", 2, 4, 8, 0), b("No-Spend Hero", "Have {n} no-spend days", "noSpend", 3, 15, 45, 3),
        b("Foodie", "Log {n} food entries", "kw:food|restaurant|dining|cafe|swiggy|zomato", 5, 30, 100, 10), b("Grocery Run", "Log {n} grocery entries", "kw:grocer|supermarket|vegetable|bigbasket|blinkit", 5, 25, 80, 15),
        b("Wanderer", "Log {n} travel entries", "kw:travel|trip|flight|hotel|holiday|train", 3, 15, 50, 9), b("Commuter", "Log {n} transport entries", "kw:transport|metro|bus|uber|ola|cab|auto", 5, 30, 100, 14),
        b("Shopper", "Log {n} shopping entries", "kw:shopping|amazon|flipkart|clothes|myntra", 5, 30, 100, 15), b("Bill Slayer", "Log {n} bill payments", "kw:bill|electric|water|internet|recharge|utility", 3, 20, 60, 20),
        b("Wellness", "Log {n} health entries", "kw:health|medic|doctor|pharma|hospital", 3, 15, 40, 10), b("Scholar", "Log {n} education entries", "kw:educat|course|tuition|book|school|college|fee", 2, 10, 30, 5),
        b("Fun Budget", "Log {n} entertainment entries", "kw:entertain|movie|game|netflix|fun|concert", 5, 25, 80, 24), b("Home Sweet Home", "Log {n} rent entries", "kw:rent", 1, 6, 12, 6),
        b("Gift Giver", "Log {n} gift entries", "kw:gift|present|birthday", 2, 10, 30, 10), b("Fuel Up", "Log {n} fuel entries", "kw:fuel|petrol|diesel", 3, 15, 50, 20),
        b("Subscriber", "Log {n} subscription entries", "kw:subscri|prime|spotify|youtube", 2, 8, 20, 16), b("Investor's Entry", "Log {n} investment entries", "kw:invest|sip|mutual|stock|etf", 1, 6, 24, 4),
        b("UPI Native", "Log {n} UPI entries", "kw:upi|gpay|phonepe|paytm", 5, 50, 200, 20), b("Cash Handler", "Log {n} cash entries", "kw:cash|atm", 3, 20, 60, 0),
        b("Giving Back", "Log {n} charity entries", "kw:charity|donat|temple|ngo", 1, 5, 20, 10), b("Gym Rat", "Log {n} fitness entries", "kw:gym|fitness|sport|yoga", 3, 20, 60, 2),
        b("Pet Parent", "Log {n} pet entries", "kw:pet|vet|dog|cat", 2, 10, 30, 10), b("Coffee Break", "Log {n} coffee entries", "kw:coffee|tea|chai|starbucks", 5, 30, 100, 0),
        b("Earner", "Earn a total of {n}", "incTotal", 10000, 100000, 1000000, 0), b("Saver", "Save a total of {n}", "saved", 5000, 50000, 500000, 19),
        b("Big Day", "Receive {n} in one go", "maxIncome", 5000, 50000, 500000, 8), b("Heavy Hitter", "Make one purchase of {n}", "maxExpense", 1000, 10000, 100000, 15),
        b("Safety Net", "Hold {n} in your emergency fund", "efBal", 1000, 25000, 250000, 7), b("Goal Fuel", "Put {n} into goals", "goalsSaved", 1000, 25000, 250000, 19),
        b("In The Green", "Finish {n} months with more in than out", "greenMonths", 1, 3, 6, 23), b("Savings Rate", "Hit a {n}% savings rate in a month", "bestRate", 10, 25, 50, 4),
        b("Debt Free", "Clear {n} debts", "debtsCleared", 1, 2, 5, 6), b("Net Worth Builder", "Track {n} net-worth items", "nw", 1, 5, 15, 11),
        b("Portfolio", "Hold {n} investments", "inv", 1, 5, 15, 4), b("Wealth Tracker", "Track {n} finance products", "prod", 1, 3, 10, 18),
        b("Budget Beginner", "Set {n} budgets", "budgets", 1, 5, 12, 3), b("On Track", "Keep {n} budgets on track this month", "onTrack", 1, 3, 8, 3),
        b("Goal Setter", "Create {n} goals", "goals", 1, 3, 8, 14), b("Goal Crusher", "Reach {n} goals", "reached", 1, 3, 10, 23),
        b("Debt Planner", "Plan {n} debts", "debts", 1, 2, 5, 7), b("Autopilot", "Set {n} recurring payments", "rec", 1, 5, 15, 12),
        b("Smart Rules", "Create {n} smart rules", "rules", 1, 5, 15, 20), b("Receipt Vault", "Store {n} receipts", "rcpt", 1, 10, 50, 22),
        b("Household", "Add {n} household members", "members", 1, 3, 6, 21), b("Level Up", "Reach companion level {n}", "level", 3, 8, 15, 24),
        b("XP Hunter", "Earn {n} XP", "xp", 300, 1500, 5000, 9), b("Every Weekday", "Log on {n} different weekdays", "weekdays", 3, 5, 7, 13),
        b("Round Numbers", "Log {n} round-number entries", "round", 5, 30, 100, 0), b("Pennywise", "Log {n} small expenses under 100", "small", 10, 50, 200, 19),
        b("Big Ticket", "Log {n} purchases over 5000", "big", 1, 5, 20, 8), b("Salary Day", "Log {n} salary entries", "kw:salary|payroll|wages", 1, 6, 12, 8),
        b("Refund Hunter", "Log {n} refunds", "refunds", 1, 5, 15, 6), b("Transfer Pro", "Log {n} transfers", "transfers", 1, 10, 40, 14),
        b("Early Bird", "Log {n} entries in the first 5 days of a month", "early", 3, 15, 50, 12), b("Mid-Month Check", "Log {n} entries around the 15th", "mid", 3, 10, 30, 13),
        b("New Year Start", "Log {n} entries in January", "m1", 1, 10, 40, 24), b("Festive Season", "Log {n} entries in Oct and Nov", "festive", 3, 15, 50, 24),
        b("Summer Spender", "Log {n} entries in Apr to Jun", "summer", 3, 15, 50, 2), b("Monsoon Mode", "Log {n} entries in Jul to Sep", "monsoon", 3, 15, 50, 21),
        b("Winter Ledger", "Log {n} entries in Dec to Feb", "winter", 3, 15, 50, 11), b("Monday Reset", "Log {n} Monday entries", "mon", 5, 25, 80, 3),
        b("Friday Night", "Log {n} Friday entries", "fri", 5, 25, 80, 24), b("Sunday Review", "Log {n} Sunday entries", "sun", 5, 25, 80, 5),
        b("Dense Month", "Log {n} entries in a single month", "monthPeak", 20, 60, 150, 13), b("Tidy Month", "Use {n} categories in one month", "monthCats", 3, 6, 10, 3),
        b("Income Months", "Have income in {n} months", "incMonths", 1, 3, 6, 0), b("Account Collector", "Use {n} accounts", "accounts", 2, 4, 8, 17),
        b("Card Carrier", "Use {n} card accounts", "cardAccts", 1, 3, 5, 16), b("Credit Care", "Log {n} credit-card entries", "cardTx", 5, 30, 100, 16),
        b("Fresh Start", "Log {n} entries in the first month", "firstMonth", 5, 20, 60, 1), b("Receipt Hoarder", "Log {n} entries with titles", "titled", 10, 100, 400, 22),
        b("Quiet Week", "Log {n} entries in a week", "weekPeak", 5, 15, 40, 12), b("Budget Master", "Keep {n} budget categories", "budgetCats", 2, 6, 12, 18),
        b("Goal Streak", "Hold {n} active goals", "activeGoals", 1, 3, 6, 14), b("Rule Maker", "Have {n} rules and recurring items", "rulesRec", 2, 8, 25, 7),
        b("Money Mover", "Move {n} in transfers", "transferTotal", 5000, 50000, 500000, 18), b("Diverse Spender", "Spend in {n} categories", "spendCats", 4, 10, 18, 21),
        b("Balanced Books", "Log as many incomes as expenses ({n}+ each)", "balanced", 3, 15, 60, 23), b("Habit Month", "Log on {n} days in one month", "monthDays", 7, 15, 28, 13),
        b("Streak Keeper", "Hold a {n}-day current streak", "streakNow", 2, 7, 21, 2), b("Century Club", "Reach {n} total entries and goals", "century", 20, 100, 400, 8),
        b("Planner Pro", "Use {n} planning tools", "tools", 2, 4, 6, 11),
        b("Tax Time", "Log {n} tax entries", "kw:tax|gst|itr", 1, 5, 15, 22), b("Insurance Cover", "Log {n} insurance entries", "kw:insur|premium|lic", 1, 4, 10, 7),
        b("Loan Crusher", "Log {n} EMI or loan entries", "kw:emi|loan|mortgage", 1, 6, 24, 6),
    )

    val all: List<AchDef> = run {
        val used = IntArray(GLYPHS)
        raw.mapIndexed { i, r ->
            val k = used[r.glyph]++
            AchDef(i + 1, r.title, r.tpl, r.metric, r.a, r.b, r.c, r.glyph, (k + r.glyph) % SHAPES, (i * 37) % 360)
        }
    }

    private fun d(s: String): LocalDate? = runCatching { LocalDate.parse(s.take(10)) }.getOrNull()

    /** All numbers behind the achievements, computed from the user's own data. Nothing is invented. */
    fun stats(ws: JsonObject, level: Int, xp: Int, today: LocalDate = LocalDate.now()): Map<String, Double> {
        val m = HashMap<String, Double>()
        val txs = transactions(ws)
        val dated = txs.mapNotNull { t -> d(t.string("date"))?.let { it to t } }
        val days = dated.map { it.first }.toSet()
        fun amt(t: JsonObject) = toBase(ws, t.double("amount"), t.string("currency", "INR"))
        fun txt(t: JsonObject) = (t.string("category") + " " + t.string("title") + " " + t.string("account")).lowercase()
        val spend = txs.filter { isSpend(it) }; val inc = txs.filter { it.string("type") == "income" }
        m["tx"] = txs.size.toDouble(); m["exp"] = spend.size.toDouble(); m["inc"] = inc.size.toDouble(); m["days"] = days.size.toDouble()
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (x in days.sorted()) { run = if (prev != null && prev.plusDays(1) == x) run + 1 else 1; best = maxOf(best, run); prev = x }
        m["streakBest"] = best.toDouble()
        var cur = 0; var c = today; if (c !in days) c = c.minusDays(1); while (c in days) { cur++; c = c.minusDays(1) }
        m["streakNow"] = cur.toDouble()
        m["perDay"] = (dated.groupingBy { it.first }.eachCount().values.maxOrNull() ?: 0).toDouble()
        m["weekend"] = dated.count { it.first.dayOfWeek == DayOfWeek.SATURDAY || it.first.dayOfWeek == DayOfWeek.SUNDAY }.toDouble()
        m["monthEnd"] = dated.count { it.first.dayOfMonth >= 28 }.toDouble(); m["early"] = dated.count { it.first.dayOfMonth <= 5 }.toDouble()
        m["mid"] = dated.count { it.first.dayOfMonth in 14..16 }.toDouble()
        m["cats"] = txs.map { it.string("category").lowercase() }.filter { it.isNotBlank() }.toSet().size.toDouble()
        m["spendCats"] = spend.map { it.string("category").lowercase() }.filter { it.isNotBlank() }.toSet().size.toDouble()
        m["curr"] = txs.map { it.string("currency", "INR") }.toSet().size.toDouble()
        val ym = dated.map { it.first.year * 12 + it.first.monthValue }
        m["months"] = ym.toSet().size.toDouble()
        m["span"] = if (days.isEmpty()) 0.0 else (days.max().toEpochDay() - days.min().toEpochDay() + 1).toDouble()
        m["payees"] = txs.map { it.string("title").lowercase() }.filter { it.isNotBlank() }.toSet().size.toDouble()
        m["titled"] = txs.count { it.string("title").isNotBlank() }.toDouble()
        m["splits"] = txs.count { it.has("splits") && it.get("splits").isJsonArray && it.getAsJsonArray("splits").size() > 0 }.toDouble()
        m["incSources"] = inc.map { it.string("title").lowercase() }.filter { it.isNotBlank() }.toSet().size.toDouble()
        if (days.isNotEmpty()) { val lo = days.min(); val hi = minOf(days.max(), today); val spendDays = spend.mapNotNull { d(it.string("date")) }.toSet(); var n = 0; var x = lo; while (!x.isAfter(hi)) { if (x !in spendDays) n++; x = x.plusDays(1) }; m["noSpend"] = n.toDouble() } else m["noSpend"] = 0.0
        m["incTotal"] = inc.sumOf { amt(it) }; m["maxIncome"] = inc.maxOfOrNull { amt(it) } ?: 0.0; m["maxExpense"] = spend.maxOfOrNull { amt(it) } ?: 0.0
        m["saved"] = (m["incTotal"]!! - spend.sumOf { amt(it) }).coerceAtLeast(0.0)
        val byMonth = dated.groupBy { it.first.year * 12 + it.first.monthValue }
        var green = 0; var bestRate = 0.0
        byMonth.values.forEach { l -> val i = l.filter { it.second.string("type") == "income" }.sumOf { amt(it.second) }; val e = l.filter { isSpend(it.second) }.sumOf { amt(it.second) }; if (i > e && i > 0) { green++; bestRate = maxOf(bestRate, (i - e) / i * 100) } }
        m["greenMonths"] = green.toDouble(); m["bestRate"] = bestRate
        m["incMonths"] = byMonth.values.count { l -> l.any { it.second.string("type") == "income" } }.toDouble()
        m["monthPeak"] = (byMonth.values.maxOfOrNull { it.size } ?: 0).toDouble()
        m["monthCats"] = (byMonth.values.maxOfOrNull { l -> l.map { it.second.string("category").lowercase() }.filter { it.isNotBlank() }.toSet().size } ?: 0).toDouble()
        m["monthDays"] = (byMonth.values.maxOfOrNull { l -> l.map { it.first }.toSet().size } ?: 0).toDouble()
        m["weekPeak"] = (dated.groupingBy { it.first.toEpochDay() / 7 }.eachCount().values.maxOrNull() ?: 0).toDouble()
        m["firstMonth"] = (byMonth.minByOrNull { it.key }?.value?.size ?: 0).toDouble()
        m["weekdays"] = dated.map { it.first.dayOfWeek }.toSet().size.toDouble()
        m["mon"] = dated.count { it.first.dayOfWeek == DayOfWeek.MONDAY }.toDouble(); m["fri"] = dated.count { it.first.dayOfWeek == DayOfWeek.FRIDAY }.toDouble(); m["sun"] = dated.count { it.first.dayOfWeek == DayOfWeek.SUNDAY }.toDouble()
        m["m1"] = dated.count { it.first.monthValue == 1 }.toDouble(); m["festive"] = dated.count { it.first.monthValue in 10..11 }.toDouble()
        m["summer"] = dated.count { it.first.monthValue in 4..6 }.toDouble(); m["monsoon"] = dated.count { it.first.monthValue in 7..9 }.toDouble()
        m["winter"] = dated.count { it.first.monthValue == 12 || it.first.monthValue <= 2 }.toDouble()
        m["round"] = txs.count { val a = it.double("amount"); a >= 100 && a % 100 == 0.0 }.toDouble()
        m["small"] = spend.count { it.double("amount") in 0.01..99.99 }.toDouble(); m["big"] = spend.count { amt(it) > 5000 }.toDouble()
        m["refunds"] = txs.count { it.string("type") == "refund" }.toDouble()
        val tr = txs.filter { it.string("type") == "transfer" }; m["transfers"] = tr.size.toDouble(); m["transferTotal"] = tr.sumOf { amt(it) }
        val accts = txs.map { it.string("account").lowercase() }.filter { it.isNotBlank() }.toSet()
        m["accounts"] = accts.size.toDouble(); m["cardAccts"] = accts.count { it.contains("card") || it.contains("credit") }.toDouble()
        m["cardTx"] = txs.count { val a = it.string("account").lowercase(); a.contains("card") || a.contains("credit") }.toDouble()
        m["balanced"] = minOf(inc.size, spend.size).toDouble()
        val goals = ws.arrayOrEmpty("goals").objects(); val sg = goals.filter { it.string("type") != "debt" }; val debts = goals.filter { it.string("type") == "debt" }
        m["goals"] = sg.size.toDouble(); m["debts"] = debts.size.toDouble()
        m["reached"] = sg.count { it.double("target") > 0 && it.double("saved") >= it.double("target") }.toDouble()
        m["activeGoals"] = sg.count { it.double("target") <= 0 || it.double("saved") < it.double("target") }.toDouble()
        m["debtsCleared"] = debts.count { it.double("target") > 0 && it.double("saved") >= it.double("target") }.toDouble()
        m["goalsSaved"] = sg.sumOf { it.double("saved") }
        val budgets = ws.arrayOrEmpty("budgets").objects(); m["budgets"] = budgets.size.toDouble(); m["budgetCats"] = budgets.map { it.string("category").lowercase() }.toSet().size.toDouble()
        val cm = today.toString().take(7)
        val spentNow = monthlyTransactions(ws, cm).filter { isSpend(it) }
        m["onTrack"] = budgets.count { bd -> spentNow.filter { it.string("category").equals(bd.string("category"), true) }.sumOf { amt(it) } <= toBase(ws, bd.double("limit"), bd.string("currency", "INR")) && bd.double("limit") > 0 }.toDouble()
        m["rec"] = ws.arrayOrEmpty("recurringRules").size().toDouble(); m["rules"] = ws.arrayOrEmpty("smartRules").size().toDouble(); m["rcpt"] = ws.arrayOrEmpty("receipts").size().toDouble()
        m["rulesRec"] = m["rec"]!! + m["rules"]!!
        m["members"] = ws.arrayOrEmpty("sharedMembers").size().toDouble(); m["nw"] = ws.arrayOrEmpty("netWorthItems").size().toDouble()
        m["inv"] = ws.arrayOrEmpty("investments").size().toDouble(); m["prod"] = ws.arrayOrEmpty("financeProducts").size().toDouble()
        m["efBal"] = ws.objectOrEmpty("emergencyFund").double("balance"); m["level"] = level.toDouble(); m["xp"] = xp.toDouble()
        m["century"] = m["tx"]!! + sg.size + debts.size + budgets.size
        m["tools"] = listOf("budgets", "goals", "rec", "rules", "rcpt", "nw", "inv", "prod", "members").count { (m[it] ?: 0.0) > 0 }.toDouble()
        for (d in all) if (d.metric.startsWith("kw:")) {
            val re = Regex(d.metric.removePrefix("kw:")); m[d.metric] = txs.count { re.containsMatchIn(txt(it)) }.toDouble()
        }
        return m
    }

    fun evaluate(stats: Map<String, Double>): List<AchState> = all.map { d ->
        val v = stats[d.metric] ?: 0.0
        AchState(d, v, (if (v >= d.t1) 1 else 0) + (if (v >= d.t2) 1 else 0) + (if (v >= d.t3) 1 else 0))
    }
}
