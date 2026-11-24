package com.fincip.nativeapp.data

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Planning estimates of annual income tax for the ten countries the app supports.
 * Bracket data was checked against official or major published sources in Nov 2026; see [CountryInfo.rulesNote].
 * These are simplified employee-style estimates, never a filing calculation.
 */
data class TaxBracket(val upTo: Double, val rate: Double)
data class TaxSlice(val from: Double, val to: Double, val rate: Double, val taxed: Double, val tax: Double)
data class TaxLine(val label: String, val amount: Double, val kind: String) // kind: tax, social, regional, credit

data class TaxInput(
    val country: String,
    val gross: Double,
    val status: String = "single",       // single / joint (US, DE)
    val extraDeductions: Double = 0.0,   // itemised / pension / other deductible amounts
    val regime: String = "new",          // India
    val ageGroup: String = "below60",    // India
    val resident: Boolean = true,        // India
    val regionalPct: Double = 0.0,       // state / provincial / church tax / Swiss override, see CountryInfo.regionalLabel
    val socialPct: Double? = null        // null = built-in rules, otherwise a flat % of gross
)

data class TaxResult(
    val country: String, val currency: String, val taxYear: String,
    val gross: Double, val deduction: Double, val taxable: Double,
    val slices: List<TaxSlice>, val lines: List<TaxLine>,
    val incomeTax: Double, val social: Double, val regional: Double,
    val total: Double, val takeHome: Double, val effectivePct: Double, val marginalPct: Double,
    val notes: List<String>
)

data class CountryInfo(
    val code: String, val name: String, val currency: String, val flag: String, val taxYear: String,
    val hasStatus: Boolean, val regionalLabel: String?, val socialLabel: String?, val approximate: Boolean, val rulesNote: String
)

object TaxEngine {
    val countries = listOf(
        CountryInfo("IN", "India", "INR", "🇮🇳", "FY 2026-27", false, null, null, false, "Slabs unchanged in Budget 2026. Surcharge and many deductions are not modelled."),
        CountryInfo("US", "United States", "USD", "🇺🇸", "Tax year 2026", true, "State income tax (effective %)", null, false, "Federal brackets and standard deduction per IRS Rev. Proc. 2025-32. FICA included. No local or credits."),
        CountryInfo("GB", "United Kingdom", "GBP", "🇬🇧", "2026/27", false, null, null, false, "England, Wales and NI rates. Scotland has its own bands. Employee NI included."),
        CountryInfo("DE", "Germany", "EUR", "🇩🇪", "2026", true, "Church tax (% of income tax, 0, 8 or 9)", "Social insurance (flat % to override)", true, "Official 2026 tariff formula. Social-insurance caps, solidarity-surcharge threshold and deductible share are approximate."),
        CountryInfo("JP", "Japan", "JPY", "🇯🇵", "2026", false, null, "Social insurance (flat % to override)", true, "Salaried-employee estimate. National tax, 2.1% surtax and a flat 10% resident tax. Special deductions are not modelled."),
        CountryInfo("CN", "China", "CNY", "🇨🇳", "2026", false, null, "Social insurance (flat % to override)", false, "Annual comprehensive-income brackets with the 60,000 basic deduction. Housing fund and special deductions go in extra deductions."),
        CountryInfo("CH", "Switzerland", "CHF", "🇨🇭", "2026", false, "Your total income-tax rate % (federal + canton + municipality)", "Social contributions (flat % to override)", true, "Swiss tax depends heavily on canton and municipality. Without your own rate this uses a rough national-average curve."),
        CountryInfo("CA", "Canada", "CAD", "🇨🇦", "2026", false, "Provincial tax (effective % of taxable income)", "CPP and EI (flat % to override)", true, "Federal brackets and the basic personal amount at the 14% credit rate. Provincial tax is your own estimate. CPP and EI are approximate."),
        CountryInfo("AU", "Australia", "AUD", "🇦🇺", "2026-27", false, null, null, false, "Resident rates from 1 July 2026, 2% Medicare levy and the low income tax offset. Medicare low-income phase-in not modelled."),
        CountryInfo("SG", "Singapore", "SGD", "🇸🇬", "YA 2026", false, null, "CPF employee share (flat % to override, 0 for foreigners)", true, "Resident rates with earned-income and CPF relief. CPF ceiling is approximate and assumes a citizen or PR under 55.")
    )
    fun info(code: String): CountryInfo = countries.firstOrNull { it.code == code } ?: countries.first()

    // ---------- helpers
    fun progressive(taxable: Double, b: List<TaxBracket>): Double {
        var prev = 0.0; var tax = 0.0
        for (x in b) { tax += (min(taxable, x.upTo) - prev).coerceAtLeast(0.0) * x.rate; prev = x.upTo; if (taxable <= x.upTo) break }
        return tax
    }
    fun slices(taxable: Double, b: List<TaxBracket>): List<TaxSlice> {
        var prev = 0.0
        return b.map { x ->
            val taxed = (min(taxable, x.upTo) - prev).coerceAtLeast(0.0)
            val s = TaxSlice(prev, x.upTo, x.rate, taxed, taxed * x.rate); prev = x.upTo; s
        }
    }
    private fun cut(vararg p: Pair<Double, Double>) = p.map { TaxBracket(it.first, it.second) }
    private val INF = Double.POSITIVE_INFINITY

    // ---------- per-country bracket tables
    private val inNew = cut(400000.0 to 0.0, 800000.0 to .05, 1200000.0 to .10, 1600000.0 to .15, 2000000.0 to .20, 2400000.0 to .25, INF to .30)
    private fun inOld(age: String) = when (age) {
        "super80" -> cut(500000.0 to 0.0, 1000000.0 to .20, INF to .30)
        "senior60" -> cut(300000.0 to 0.0, 500000.0 to .05, 1000000.0 to .20, INF to .30)
        else -> cut(250000.0 to 0.0, 500000.0 to .05, 1000000.0 to .20, INF to .30)
    }
    private val usSingle = cut(12400.0 to .10, 50400.0 to .12, 105700.0 to .22, 201775.0 to .24, 256225.0 to .32, 640600.0 to .35, INF to .37)
    private val usJoint = cut(24800.0 to .10, 100800.0 to .12, 211400.0 to .22, 403550.0 to .24, 512450.0 to .32, 768700.0 to .35, INF to .37)
    private val gbBands = cut(37700.0 to .20, 125140.0 to .40, INF to .45)
    private val jpBands = cut(1950000.0 to .05, 3300000.0 to .10, 6950000.0 to .20, 9000000.0 to .23, 18000000.0 to .33, 40000000.0 to .40, INF to .45)
    private val cnBands = cut(36000.0 to .03, 144000.0 to .10, 300000.0 to .20, 420000.0 to .25, 660000.0 to .30, 960000.0 to .35, INF to .45)
    private val caBands = cut(58523.0 to .14, 117045.0 to .205, 181440.0 to .26, 258482.0 to .29, INF to .33)
    private val auBands = cut(18200.0 to 0.0, 45000.0 to .15, 135000.0 to .30, 190000.0 to .37, INF to .45)
    private val sgBands = cut(20000.0 to 0.0, 30000.0 to .02, 40000.0 to .035, 80000.0 to .07, 120000.0 to .115, 160000.0 to .15, 200000.0 to .18, 240000.0 to .19, 280000.0 to .195, 320000.0 to .20, 500000.0 to .22, 1000000.0 to .23, INF to .24)

    /** German income tax (Grundtarif 2026), result rounded down to whole euros as the law prescribes. */
    fun deTariff(zvE: Double): Double {
        val x = floor(zvE)
        return floor(when {
            x <= 12348 -> 0.0
            x <= 17799 -> { val y = (x - 12348) / 10000.0; (914.51 * y + 1400.0) * y }
            x <= 69878 -> { val z = (x - 17799) / 10000.0; (173.10 * z + 2397.0) * z + 1034.87 }
            x <= 277825 -> 0.42 * x - 11135.63
            else -> 0.45 * x - 19470.38
        })
    }
    private val deBands = cut(12348.0 to 0.0, 17799.0 to 0.14, 69878.0 to 0.30, 277825.0 to .42, INF to .45)

    private fun swissCurve(income: Double): Double {
        val pts = listOf(15000.0 to 0.0, 30000.0 to 3.0, 50000.0 to 7.5, 100000.0 to 14.0, 150000.0 to 18.0, 250000.0 to 22.0, 500000.0 to 25.0)
        if (income <= pts.first().first) return 0.0
        if (income >= pts.last().first) return pts.last().second
        val i = pts.indexOfFirst { income <= it.first }
        val (x0, y0) = pts[i - 1]; val (x1, y1) = pts[i]
        return y0 + (y1 - y0) * (income - x0) / (x1 - x0)
    }

    // ---------- main entry
    fun compute(i: TaxInput): TaxResult {
        val ci = info(i.country)
        val total = fun(g: Double): Double = calc(i.copy(gross = g), ci).total
        val r = calc(i, ci)
        val d = max(1.0, i.gross * 0.01)
        val marginal = if (i.gross > 0) ((total(i.gross + d) - total(i.gross)) / d * 100).coerceIn(0.0, 100.0) else 0.0
        return r.copy(marginalPct = marginal)
    }

    private fun calc(i: TaxInput, ci: CountryInfo): TaxResult {
        val g = max(0.0, i.gross)
        val lines = mutableListOf<TaxLine>()
        val notes = mutableListOf<String>()
        var deduction = 0.0; var taxable = 0.0; var sl: List<TaxSlice> = emptyList()
        var incomeTax = 0.0; var social = 0.0; var regional = 0.0

        fun flat(pct: Double?, base: Double): Double? = pct?.let { base * it.coerceIn(0.0, 60.0) / 100.0 }

        when (ci.code) {
            "IN" -> {
                val std = if (i.regime == "new") 75000.0 else 50000.0
                deduction = std + if (i.regime == "old") i.extraDeductions else 0.0
                taxable = max(0.0, g - deduction)
                val bands = if (i.regime == "new") inNew else inOld(i.ageGroup)
                val before = progressive(taxable, bands); sl = slices(taxable, bands)
                val rebate = if (i.resident && i.regime == "new") { if (taxable <= 1200000.0) min(60000.0, before) else max(0.0, before - (taxable - 1200000.0)) }
                else if (i.resident && i.regime == "old" && taxable <= 500000.0) min(12500.0, before) else 0.0
                val after = max(0.0, before - rebate); val cess = after * .04
                incomeTax = after + cess
                lines += TaxLine("Income tax on slabs", before, "tax")
                if (rebate > 0) lines += TaxLine("Section 87A rebate", -rebate, "credit")
                lines += TaxLine("Health & education cess (4%)", cess, "tax")
                notes += "Standard deduction ${if (i.regime == "new") "₹75,000 (new regime)" else "₹50,000 (old regime)"}. Surcharge above ₹50 lakh is not modelled."
            }
            "US" -> {
                val joint = i.status == "joint"
                val std = if (joint) 32200.0 else 16100.0
                deduction = max(std, i.extraDeductions); taxable = max(0.0, g - deduction)
                val bands = if (joint) usJoint else usSingle
                incomeTax = progressive(taxable, bands); sl = slices(taxable, bands)
                val ss = min(g, 184500.0) * .062
                val addl = max(0.0, g - if (joint) 250000.0 else 200000.0) * .009
                val medicare = g * .0145 + addl
                social = flat(i.socialPct, g) ?: (ss + medicare)
                regional = taxable * i.regionalPct.coerceIn(0.0, 25.0) / 100.0
                lines += TaxLine("Federal income tax", incomeTax, "tax")
                lines += TaxLine("Social Security + Medicare (FICA)", social, "social")
                if (regional > 0) lines += TaxLine("State income tax (your estimate)", regional, "regional")
                notes += "Standard deduction ${if (joint) "$32,200" else "$16,100"} applied unless your itemised amount is higher."
            }
            "GB" -> {
                val pensionAdj = max(0.0, g - i.extraDeductions)
                val pa = if (pensionAdj <= 100000.0) 12570.0 else max(0.0, 12570.0 - (pensionAdj - 100000.0) / 2.0)
                deduction = pa + min(g, i.extraDeductions); taxable = max(0.0, pensionAdj - pa)
                incomeTax = progressive(taxable, gbBands); sl = slices(taxable, gbBands)
                val ni = (min(g, 50270.0) - 12570.0).coerceAtLeast(0.0) * .08 + (g - 50270.0).coerceAtLeast(0.0) * .02
                social = flat(i.socialPct, g) ?: ni
                lines += TaxLine("Income tax", incomeTax, "tax")
                lines += TaxLine("National Insurance (employee)", social, "social")
                if (pensionAdj > 100000.0) notes += "Your personal allowance is tapered above £100,000."
                notes += "Student loan repayments are not included."
            }
            "DE" -> {
                val joint = i.status == "joint"
                val caps = if (i.socialPct != null) g * i.socialPct.coerceIn(0.0, 60.0) / 100.0 else (min(g, 101400.0) * .106 + min(g, 69750.0) * .106)
                social = caps
                val wk = max(1230.0, i.extraDeductions)
                deduction = wk + social * .9
                taxable = max(0.0, g - deduction)
                val est = if (joint) 2 * deTariff(taxable / 2.0) else deTariff(taxable)
                val fr = if (joint) 40700.0 else 20350.0
                val soli = if (est <= fr) 0.0 else min(est * .055, (est - fr) * .119)
                regional = est * i.regionalPct.coerceIn(0.0, 9.0) / 100.0
                incomeTax = est + soli
                sl = slices(if (joint) taxable / 2.0 else taxable, deBands).map { it.copy(taxed = it.taxed * (if (joint) 2 else 1), tax = it.tax * (if (joint) 2 else 1)) }
                lines += TaxLine("Income tax (Einkommensteuer)", est, "tax")
                if (soli > 0) lines += TaxLine("Solidarity surcharge", soli, "tax")
                if (regional > 0) lines += TaxLine("Church tax", regional, "regional")
                lines += TaxLine("Pension, health, care, unemployment", social, "social")
                notes += "Sozialversicherung is approximate: about 21% up to the contribution ceilings. Employer-side costs are not shown."
            }
            "JP" -> {
                val emp = when {
                    g <= 1625000.0 -> 650000.0
                    g <= 1800000.0 -> g * .40 - 100000.0
                    g <= 3600000.0 -> g * .30 + 80000.0
                    g <= 6600000.0 -> g * .20 + 440000.0
                    g <= 8500000.0 -> g * .10 + 1100000.0
                    else -> 1950000.0
                }
                social = flat(i.socialPct, g) ?: (g * .147)
                deduction = emp + social + 580000.0 + i.extraDeductions
                taxable = max(0.0, g - deduction)
                val national = progressive(taxable, jpBands); sl = slices(taxable, jpBands)
                val surtax = national * .021
                val resTaxable = max(0.0, g - emp - social - 430000.0 - i.extraDeductions)
                regional = if (g > 0) resTaxable * .10 + 5000.0 else 0.0
                incomeTax = national + surtax
                lines += TaxLine("National income tax", national, "tax")
                lines += TaxLine("Reconstruction surtax (2.1%)", surtax, "tax")
                lines += TaxLine("Resident tax (approx. 10% + per-capita)", regional, "regional")
                lines += TaxLine("Social insurance", social, "social")
                notes += "Salaried employee estimate. Dependants, housing loan and other special deductions are not modelled."
            }
            "CN" -> {
                social = flat(i.socialPct, g) ?: (g * .105)
                deduction = 60000.0 + social + i.extraDeductions
                taxable = max(0.0, g - deduction)
                incomeTax = progressive(taxable, cnBands); sl = slices(taxable, cnBands)
                lines += TaxLine("Individual income tax", incomeTax, "tax")
                lines += TaxLine("Social insurance (pension, medical, unemployment)", social, "social")
                notes += "Enter housing fund and special additional deductions (children, education, rent) as extra deductions."
            }
            "CH" -> {
                social = flat(i.socialPct, g) ?: (g * .053 + min(g, 148200.0) * .011)
                deduction = social + i.extraDeductions
                taxable = max(0.0, g - deduction)
                val rate = if (i.regionalPct > 0) i.regionalPct.coerceIn(0.0, 50.0) else swissCurve(taxable)
                incomeTax = taxable * rate / 100.0
                sl = listOf(TaxSlice(0.0, taxable, rate / 100.0, taxable, incomeTax))
                lines += TaxLine("Income tax (federal + canton + municipality)", incomeTax, "tax")
                lines += TaxLine("AHV/IV/EO + unemployment", social, "social")
                notes += if (i.regionalPct > 0) "Using your own total tax rate of ${"%.1f".format(i.regionalPct)}%." else "Rough national-average curve. Enter your canton's rate for a real estimate. Pillar 2 (BVG) is not included."
            }
            "CA" -> {
                val cpp = (min(g, 74600.0) - 3500.0).coerceAtLeast(0.0) * .0595 + (min(g, 85000.0) - 74600.0).coerceAtLeast(0.0) * .04
                val ei = min(g, 68900.0) * .0163
                social = flat(i.socialPct, g) ?: (cpp + ei)
                deduction = i.extraDeductions; taxable = max(0.0, g - deduction)
                val bpa = when { taxable <= 181440.0 -> 16452.0; taxable >= 258482.0 -> 14829.0; else -> 16452.0 - (16452.0 - 14829.0) * (taxable - 181440.0) / (258482.0 - 181440.0) }
                val gross14 = progressive(taxable, caBands); sl = slices(taxable, caBands)
                val credit = bpa * .14
                incomeTax = max(0.0, gross14 - credit)
                regional = taxable * i.regionalPct.coerceIn(0.0, 30.0) / 100.0
                lines += TaxLine("Federal tax on brackets", gross14, "tax")
                lines += TaxLine("Basic personal amount credit", -min(credit, gross14), "credit")
                if (regional > 0) lines += TaxLine("Provincial tax (your estimate)", regional, "regional")
                lines += TaxLine("CPP + EI (approx.)", social, "social")
                notes += "Quebec has its own system and abatement. RRSP contributions go in extra deductions."
            }
            "AU" -> {
                deduction = i.extraDeductions; taxable = max(0.0, g - deduction)
                val tax = progressive(taxable, auBands); sl = slices(taxable, auBands)
                val lito = when { taxable <= 37500.0 -> 700.0; taxable <= 45000.0 -> 700.0 - (taxable - 37500.0) * .05; taxable <= 66667.0 -> 325.0 - (taxable - 45000.0) * .015; else -> 0.0 }
                incomeTax = max(0.0, tax - min(lito, tax))
                social = flat(i.socialPct, taxable) ?: (taxable * .02)
                lines += TaxLine("Income tax on brackets", tax, "tax")
                if (lito > 0) lines += TaxLine("Low income tax offset", -min(lito, tax), "credit")
                lines += TaxLine("Medicare levy (2%)", social, "social")
                notes += "Employer super (about 12%) is paid on top of salary and is not counted here."
            }
            "SG" -> {
                social = flat(i.socialPct, g) ?: (min(g, 96000.0) * .20)
                deduction = 1000.0 + social + i.extraDeductions; taxable = max(0.0, g - deduction)
                incomeTax = progressive(taxable, sgBands); sl = slices(taxable, sgBands)
                lines += TaxLine("Income tax", incomeTax, "tax")
                lines += TaxLine("CPF employee contribution (also tax-relieved)", social, "social")
                notes += "Foreigners without CPF should enter 0 for the social share. Reliefs beyond earned income and CPF go in extra deductions."
            }
        }
        val totalTax = incomeTax + social + regional
        val take = max(0.0, g - totalTax)
        return TaxResult(
            ci.code, ci.currency, ci.taxYear, g, deduction, taxable, sl, lines, incomeTax, social, regional, totalTax, take,
            if (g > 0) totalTax / g * 100 else 0.0, 0.0, notes
        )
    }
}
