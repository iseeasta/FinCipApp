package com.fincip.nativeapp.csv

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** FinCip's internal transaction, the single source every export profile maps from. Amount is always positive; [type] carries direction. */
data class ExTxn(
    val id: String, val date: LocalDate, val amount: Double, val type: String, val title: String, val category: String,
    val account: String, val currency: String, val note: String, val tags: String, val ref: String
) {
    val isIn get() = type == "income" || type == "refund"
}

enum class Kind { ALL, INCOME, EXPENSE }

data class ExportFilter(
    val from: LocalDate? = null, val to: LocalDate? = null, val kind: Kind = Kind.ALL,
    val categories: Set<String> = emptySet(), val accounts: Set<String> = emptySet()
) {
    fun apply(all: List<ExTxn>): List<ExTxn> = all.filter { t ->
        (from == null || !t.date.isBefore(from)) && (to == null || !t.date.isAfter(to)) &&
            when (kind) { Kind.ALL -> true; Kind.INCOME -> t.isIn; Kind.EXPENSE -> !t.isIn && t.type != "transfer" } &&
            (categories.isEmpty() || t.category in categories) && (accounts.isEmpty() || t.account in accounts)
    }.sortedWith(compareBy({ it.date }, { it.id }))

    companion object {
        fun month(ym: YearMonth, kind: Kind = Kind.ALL) = ExportFilter(ym.atDay(1), ym.atEndOfMonth(), kind)
        fun year(y: Int, kind: Kind = Kind.ALL) = ExportFilter(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31), kind)
    }
}

enum class Compat(val label: String, val note: String) {
    OFFICIAL("Officially supported import format", "The target app documents CSV/file import."),
    BEST_EFFORT("Best-effort compatible format", "The app accepts imports, but its exact column layout is not published; this follows its own export layout."),
    STATEMENT("Statement-compatible format", "Statement-compatible format — external transaction import not officially supported."),
    NOT_SUPPORTED("Import not supported", "No import path found, so this export is disabled.")
}

enum class AppGroup(val title: String) { UPI("UPI applications"), BANK("Banking applications"), WALLET("Digital wallets"), TRACKER("Personal finance & expense trackers"), GENERIC("FinCip") }

class Col(val header: String, val required: Boolean = false, val value: (ExTxn) -> String)

/** One target app. Everything the export engine needs is data here; no app-specific branching in the engine. */
class ExportProfile(
    val id: String, val name: String, val group: AppGroup, val brandColor: Long, val initials: String,
    val compat: Compat, val source: String, val sourceUrl: String,
    val columns: List<Col>,
    val dateFormat: String = "yyyy-MM-dd",
    val delimiter: Char = ',',
    val extension: String = "csv",
    val fileTag: String = name.replace(" ", ""),
    val fileKind: String = "Export",
    val cannotTransfer: List<String> = emptyList(),
    val enabled: Boolean = compat != Compat.NOT_SUPPORTED,
    val headerLines: List<List<String>> = emptyList()
) {
    val dateFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH)
    fun fmtDate(d: LocalDate): String = d.format(dateFmt)
    val headers get() = columns.map { it.header }
    fun row(t: ExTxn) = columns.map { it.value(t) }
    fun build(txns: List<ExTxn>): String = Csv.write(headerLines + listOf(headers) + txns.map(::row), delimiter)
    fun fileName(filter: ExportFilter, txns: List<ExTxn>): String {
        val range = when {
            filter.from != null && filter.to != null && filter.from.year == filter.to.year && filter.from.dayOfYear == 1 && filter.to.monthValue == 12 && filter.to.dayOfMonth == 31 -> "${filter.from.year}"
            filter.from != null && filter.to != null && YearMonth.from(filter.from) == YearMonth.from(filter.to) && filter.from.dayOfMonth == 1 && filter.to == YearMonth.from(filter.to).atEndOfMonth() -> filter.from.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            filter.from != null && filter.to != null -> "${filter.from}_to_${filter.to}"
            txns.isNotEmpty() -> { val a = txns.minOf { it.date }; val b = txns.maxOf { it.date }; if (a.year == b.year) "${a.year}" else "${a.year}-${b.year}" }
            else -> LocalDate.now().year.toString()
        }
        return "FinCip_${fileTag}_${fileKind}_$range.$extension"
    }
}

data class ExportSummary(val count: Int, val from: LocalDate?, val to: LocalDate?, val income: Double, val expense: Double, val transfers: Int, val refunds: Int)

fun summarize(t: List<ExTxn>) = ExportSummary(
    t.size, t.minOfOrNull { it.date }, t.maxOfOrNull { it.date },
    t.filter { it.type == "income" }.sumOf { it.amount },
    t.filter { it.type != "income" && it.type != "transfer" && it.type != "refund" }.sumOf { it.amount } - t.filter { it.type == "refund" }.sumOf { it.amount },
    t.count { it.type == "transfer" }, t.count { it.type == "refund" }
)

private fun amt(v: Double) = "%.2f".format(Locale.US, v)
private fun signed(t: ExTxn) = (if (t.isIn) "" else "-") + amt(t.amount)
private fun drcr(t: ExTxn) = if (t.isIn) "CREDIT" else "DEBIT"
private fun txt(s: String) = Csv.guard(s)

object Profiles {
    private val STATEMENT_NOTE = "Layout modelled on the app's statement download; not checked against a current official sample."
    private const val NO_BAL = "Running balance (FinCip does not track bank balances)"
    
    private fun fmt(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)

    // ---- Universal ----
    val universal = ExportProfile(
        "universal", "Universal CSV", AppGroup.GENERIC, 0xFF2E7D32, "FC", Compat.OFFICIAL,
        "FinCip's own open format. Re-importable by FinCip; opens in Excel, Google Sheets and LibreOffice.", "",
        listOf(
            Col("date", true) { it.date.toString() }, Col("type", true) { it.type }, Col("title", true) { txt(it.title) },
            Col("category") { it.category }, Col("amount", true) { amt(it.amount) }, Col("currency", true) { it.currency },
            Col("account") { it.account }, Col("reference_id") { it.ref }, Col("tags") { it.tags }, Col("note") { txt(it.note) }, Col("fincip_id") { it.id }
        ), fileTag = "Universal", fileKind = "Transactions"
    )

    // ---- Verified import targets ----
    val walletBudgetBakers = ExportProfile(
        "wallet", "Wallet by BudgetBakers", AppGroup.TRACKER, 0xFF2D9CDB, "W", Compat.BEST_EFFORT,
        "BudgetBakers Help: CSV, XLS/XLSX and OFX import is supported (Date and Amount mandatory, DD/MM/YYYY style dates, decimal point or comma, no thousands separators, ~1,000 rows per file, currency must match the account). Column headers are not listed on the help page, so this follows Wallet's own export layout.",
        "https://support.budgetbakers.com/hc/en-us/articles/7152318099346",
        listOf(
            Col("account") { it.account.ifBlank { "FinCip" } }, Col("category") { it.category }, Col("currency", true) { it.currency },
            Col("amount", true) { signed(it) }, Col("type") { if (it.isIn) "Income" else "Expenses" }, Col("payment_type") { "CASH" },
            Col("note") { txt(it.note.ifBlank { it.title }) }, Col("date", true) { it.date.format(fmt("yyyy-MM-dd")) + "T00:00:00" },
            Col("payee") { txt(it.title) }, Col("labels") { it.tags }
        ), fileTag = "WalletBudgetBakers", cannotTransfer = listOf("Transfers are exported as ordinary income/expense (Wallet links transfers between its own accounts)", "Payment type is not tracked by FinCip and is written as a placeholder"),
        fileKind = "Export"
    )
    val spendee = ExportProfile(
        "spendee", "Spendee", AppGroup.TRACKER, 0xFF00B894, "Sp", Compat.OFFICIAL,
        "Spendee blog/help: CSV, XLS and XLSX import (free, web app only, under 10 MB, one currency per file). Columns are mapped by you after upload; a date, category and amount are required.",
        "https://medium.com/spendee/import-your-transactions-in-1-minute-3fea15a60f71",
        listOf(
            Col("Date", true) { it.date.format(fmt("yyyy-MM-dd")) }, Col("Category name", true) { it.category }, Col("Amount", true) { amt(it.amount) },
            Col("Type of category", true) { if (it.isIn) "income" else "expense" }, Col("Note") { txt(it.note.ifBlank { it.title }) }, Col("Currency") { it.currency }
        ), cannotTransfer = listOf("You must map the columns yourself during Spendee's import", "Every row must use one currency — export one currency at a time", "Transfers are exported as income/expense"),
        fileTag = "Spendee"
    )
    val moneyManager = ExportProfile(
        "moneymanager", "Money Manager", AppGroup.TRACKER, 0xFFE67E22, "MM", Compat.OFFICIAL,
        "Realbyte help: import goes through the Excel-import template; the file it reads is tab-separated (.tsv) with columns Date, Account, Category, Subcategory, Note, Amount, Income/Expense, Description; dates mm/dd/yyyy; Account and Category are mandatory and must match names already in the app.",
        "https://help.realbyteapps.com/hc/en-us/articles/360043223253",
        listOf(
            Col("Date", true) { it.date.format(fmt("MM/dd/yyyy")) }, Col("Account", true) { it.account.ifBlank { "Cash" } }, Col("Category", true) { it.category },
            Col("Subcategory") { "" }, Col("Note") { txt(it.title) }, Col("Amount", true) { amt(it.amount) }, Col("Income/Expense", true) { if (it.isIn) "Income" else if (it.type == "transfer") "Transfer-Out" else "Expenses" },
            Col("Description") { txt(it.note) }
        ), delimiter = '\t', extension = "tsv",
        cannotTransfer = listOf("Money Manager reads tab-separated .tsv, so this file is .tsv, not .csv", "Account and category names must already exist in Money Manager or the import fails", "Transfers need the destination account, which FinCip does not store"),
        fileTag = "MoneyManager"
    )
    val walnut = ExportProfile(
        "walnut", "Walnut", AppGroup.TRACKER, 0xFF8E44AD, "Wn", Compat.NOT_SUPPORTED,
        "No documented way to import external transactions was found, so this profile is disabled.", "", emptyList(), fileTag = "Walnut"
    )

    // ---- Statement-style layouts (no external import) ----
    private fun stmt(
        id: String, name: String, group: AppGroup, color: Long, initials: String, source: String, dateFormat: String,
        cols: List<Col>, extra: List<String> = emptyList(), compat: Compat = Compat.STATEMENT, fileKind: String = "Export", fileTag: String = name.replace(" ", "")
    ) = ExportProfile(id, name, group, color, initials, compat, source, "", cols, dateFormat, fileTag = fileTag, fileKind = fileKind, cannotTransfer = extra)

    private fun bankCols(dateHeader: String, descHeader: String, refHeader: String, debit: String, credit: String, dateFmt: String, extras: List<Col> = emptyList()) = listOf(
        Col(dateHeader, true) { it.date.format(fmt(dateFmt)) }, Col(descHeader, true) { txt(it.title + if (it.note.isNotBlank()) " | " + it.note else "") },
        Col(refHeader) { it.ref }, Col(debit, true) { if (!it.isIn) amt(it.amount) else "" }, Col(credit, true) { if (it.isIn) amt(it.amount) else "" }
    ) + extras

    private val noBalance = listOf(NO_BAL, "Category and tags (statements have no category column)")

    val phonepe = stmt("phonepe", "PhonePe", AppGroup.UPI, 0xFF5F259F, "Pe", "PhonePe only offers statement downloads (Date, Transaction Details, Type, Amount). No external import exists. $STATEMENT_NOTE", "MMM dd, yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("MMM dd, yyyy")) }, Col("Transaction Details", true) { txt((if (it.isIn) "Received from " else "Paid to ") + it.title) }, Col("Type", true) { drcr(it) }, Col("Amount", true) { amt(it.amount) }),
        listOf("Category, tags, account and notes (not part of the statement layout)", "UPI reference number is dropped"))
    val gpay = stmt("gpay", "Google Pay", AppGroup.UPI, 0xFF4285F4, "G", "Google Pay has no external import; its activity is available only as a Google Takeout export. This is a generic statement-style layout. $STATEMENT_NOTE", "dd MMM yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd MMM yyyy")) }, Col("Description", true) { txt((if (it.isIn) "Received from " else "Paid to ") + it.title) }, Col("Status", true) { "Completed" }, Col("Amount", true) { signed(it) }),
        listOf("Category, tags, notes and reference IDs"))
    val paytm = stmt("paytm", "Paytm", AppGroup.UPI, 0xFF00B9F1, "Pt", "Paytm offers UPI statement downloads (PDF/Excel) but no external import. $STATEMENT_NOTE", "dd/MM/yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd/MM/yyyy")) }, Col("Time") { "" }, Col("Transaction Details", true) { txt(it.title) }, Col("Other Transaction Details") { txt(it.note) },
            Col("Your Account") { it.account }, Col("Amount", true) { (if (it.isIn) "+ " else "- ") + "Rs." + amt(it.amount) }, Col("UPI Ref No.") { it.ref }, Col("Tag") { it.category }),
        listOf("Time of day (FinCip stores dates only)"))
    val bhim = stmt("bhim", "BHIM UPI", AppGroup.UPI, 0xFFF47216, "BH", "BHIM shows a transaction history/statement only; there is no external import. Generic statement layout. $STATEMENT_NOTE", "dd/MM/yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd/MM/yyyy")) }, Col("Transaction ID") { it.ref }, Col("Description", true) { txt(it.title) }, Col("Debit") { if (!it.isIn) amt(it.amount) else "" }, Col("Credit") { if (it.isIn) amt(it.amount) else "" }),
        listOf("Category, tags and notes"))
    val amazonPay = stmt("amazonpay", "Amazon Pay", AppGroup.WALLET, 0xFFFF9900, "Az", "Amazon Pay exposes statements/transaction history but no external import. Generic wallet statement layout. $STATEMENT_NOTE", "dd-MM-yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd-MM-yyyy")) }, Col("Transaction ID") { it.ref }, Col("Description", true) { txt(it.title) }, Col("Debit") { if (!it.isIn) amt(it.amount) else "" }, Col("Credit") { if (it.isIn) amt(it.amount) else "" }),
        noBalance)
    val cred = stmt("cred", "CRED", AppGroup.WALLET, 0xFF1B1B1B, "CR", "CRED tracks cards/bills and has no external transaction import. Generic statement layout. $STATEMENT_NOTE", "dd MMM yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd MMM yyyy")) }, Col("Merchant / Details", true) { txt(it.title) }, Col("Type", true) { drcr(it) }, Col("Amount", true) { amt(it.amount) }), listOf("Category, tags, notes and reference IDs"))
    val mobikwik = stmt("mobikwik", "Mobikwik", AppGroup.WALLET, 0xFF0E5CAD, "Mk", "Mobikwik wallet statements can be downloaded; there is no external import. Generic wallet statement layout. $STATEMENT_NOTE", "dd-MM-yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd-MM-yyyy")) }, Col("Transaction ID") { it.ref }, Col("Description", true) { txt(it.title) }, Col("Debit") { if (!it.isIn) amt(it.amount) else "" }, Col("Credit") { if (it.isIn) amt(it.amount) else "" }), noBalance)
    val freecharge = stmt("freecharge", "Freecharge", AppGroup.WALLET, 0xFFF26B21, "Fc", "Freecharge shows a transaction history; there is no external import. Generic wallet statement layout. $STATEMENT_NOTE", "dd-MM-yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd-MM-yyyy")) }, Col("Transaction ID") { it.ref }, Col("Description", true) { txt(it.title) }, Col("Debit") { if (!it.isIn) amt(it.amount) else "" }, Col("Credit") { if (it.isIn) amt(it.amount) else "" }), noBalance)

    val sbi = stmt("sbi", "YONO SBI", AppGroup.BANK, 0xFF22409A, "SB", "SBI/YONO downloads account statements; banks do not import external transactions. $STATEMENT_NOTE", "dd MMM yyyy",
        listOf(Col("Txn Date", true) { it.date.format(fmt("dd MMM yyyy")) }, Col("Value Date") { it.date.format(fmt("dd MMM yyyy")) }, Col("Description", true) { txt(it.title) }, Col("Ref No./Cheque No.") { it.ref },
            Col("Debit", true) { if (!it.isIn) amt(it.amount) else "" }, Col("Credit", true) { if (it.isIn) amt(it.amount) else "" }, Col("Balance") { "" }), noBalance, fileTag = "SBI_Bank_Statement")
    val hdfc = stmt("hdfc", "HDFC Bank", AppGroup.BANK, 0xFF004C8F, "HD", "HDFC NetBanking/MobileBanking provides statement downloads; no external import. $STATEMENT_NOTE", "dd/MM/yy",
        listOf(Col("Date", true) { it.date.format(fmt("dd/MM/yy")) }, Col("Narration", true) { txt(it.title) }, Col("Chq./Ref.No.") { it.ref }, Col("Value Dt") { it.date.format(fmt("dd/MM/yy")) },
            Col("Withdrawal Amt.", true) { if (!it.isIn) amt(it.amount) else "" }, Col("Deposit Amt.", true) { if (it.isIn) amt(it.amount) else "" }, Col("Closing Balance") { "" }), noBalance, fileTag = "HDFC_Bank_Statement")
    val icici = stmt("icici", "ICICI Bank", AppGroup.BANK, 0xFFB02A30, "IC", "iMobile/NetBanking provides statement downloads; no external import. $STATEMENT_NOTE", "dd/MM/yyyy",
        listOf(Col("S No.") { "" }, Col("Value Date") { it.date.format(fmt("dd/MM/yyyy")) }, Col("Transaction Date", true) { it.date.format(fmt("dd/MM/yyyy")) }, Col("Cheque Number") { it.ref },
            Col("Transaction Remarks", true) { txt(it.title) }, Col("Withdrawal Amount (INR )", true) { if (!it.isIn) amt(it.amount) else "" }, Col("Deposit Amount (INR )", true) { if (it.isIn) amt(it.amount) else "" }, Col("Balance (INR )") { "" }), noBalance, fileTag = "ICICI_Bank_Statement")
    val axis = stmt("axis", "Axis Mobile", AppGroup.BANK, 0xFF97144D, "Ax", "Axis Mobile/Internet Banking provides statements; no external import. $STATEMENT_NOTE", "dd-MM-yyyy",
        listOf(Col("SRL NO") { "" }, Col("Tran Date", true) { it.date.format(fmt("dd-MM-yyyy")) }, Col("CHQNO") { it.ref }, Col("PARTICULARS", true) { txt(it.title) },
            Col("DR", true) { if (!it.isIn) amt(it.amount) else "" }, Col("CR", true) { if (it.isIn) amt(it.amount) else "" }, Col("BAL") { "" }), noBalance, fileTag = "Axis_Bank_Statement")
    val kotak = stmt("kotak", "Kotak Mobile Banking", AppGroup.BANK, 0xFFED1C24, "Kt", "Kotak provides statement downloads; no external import. $STATEMENT_NOTE", "dd-MM-yyyy",
        listOf(Col("Sl. No.") { "" }, Col("Transaction Date", true) { it.date.format(fmt("dd-MM-yyyy")) }, Col("Value Date") { it.date.format(fmt("dd-MM-yyyy")) }, Col("Description", true) { txt(it.title) }, Col("Chq / Ref No.") { it.ref },
            Col("Debit", true) { if (!it.isIn) amt(it.amount) else "" }, Col("Credit", true) { if (it.isIn) amt(it.amount) else "" }, Col("Balance") { "" }, Col("Dr / Cr") { if (it.isIn) "CR" else "DR" }), noBalance, fileTag = "Kotak_Bank_Statement")
    val idfc = stmt("idfc", "IDFC FIRST Bank", AppGroup.BANK, 0xFF9C1D26, "ID", "IDFC FIRST provides statement downloads; no external import. $STATEMENT_NOTE", "dd-MMM-yyyy",
        listOf(Col("Transaction Date", true) { it.date.format(fmt("dd-MMM-yyyy")) }, Col("Value Date") { it.date.format(fmt("dd-MMM-yyyy")) }, Col("Particulars", true) { txt(it.title) }, Col("Cheque No.") { it.ref },
            Col("Debit", true) { if (!it.isIn) amt(it.amount) else "" }, Col("Credit", true) { if (it.isIn) amt(it.amount) else "" }, Col("Balance") { "" }), noBalance, fileTag = "IDFCFirst_Bank_Statement")
    val jupiter = stmt("jupiter", "Jupiter Money", AppGroup.BANK, 0xFFFFB400, "Jp", "Jupiter (neobank, partner-bank accounts) provides statements; no external import. Generic statement layout. $STATEMENT_NOTE", "dd/MM/yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd/MM/yyyy")) }, Col("Description", true) { txt(it.title) }, Col("Debit") { if (!it.isIn) amt(it.amount) else "" }, Col("Credit") { if (it.isIn) amt(it.amount) else "" }, Col("Balance") { "" }), noBalance, fileTag = "Jupiter")
    val fi = stmt("fi", "Fi Money", AppGroup.BANK, 0xFF00B386, "Fi", "Fi (neobank, partner-bank accounts) provides statements; no external import. Generic statement layout. $STATEMENT_NOTE", "dd/MM/yyyy",
        listOf(Col("Date", true) { it.date.format(fmt("dd/MM/yyyy")) }, Col("Description", true) { txt(it.title) }, Col("Debit") { if (!it.isIn) amt(it.amount) else "" }, Col("Credit") { if (it.isIn) amt(it.amount) else "" }, Col("Balance") { "" }), noBalance, fileTag = "FiMoney")

    val all: List<ExportProfile> = listOf(
        phonepe, gpay, paytm, bhim, sbi, hdfc, icici, axis, kotak, idfc, jupiter, fi,
        amazonPay, cred, mobikwik, freecharge, moneyManager, walnut, spendee, walletBudgetBakers
    )
    fun byId(id: String) = if (id == "universal") universal else all.firstOrNull { it.id == id }
    fun search(q: String) = all.filter { q.isBlank() || it.name.contains(q.trim(), true) || it.group.title.contains(q.trim(), true) }
}
