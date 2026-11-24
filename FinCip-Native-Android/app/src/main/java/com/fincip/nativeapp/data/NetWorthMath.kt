package com.fincip.nativeapp.data

/** Groups used to split net worth into readable slices. Pure Kotlin so it can be unit-tested. */
object NetWorthGroups {
    val assets = listOf("Cash & bank", "Investments", "Gold", "Property", "Vehicle", "Other asset")
    val liabilities = listOf("Home loan", "Car loan", "Personal loan", "Education loan", "Credit card", "Other debt")

    fun icon(group: String): String = when (group) {
        "Cash & bank" -> "🏦"; "Investments" -> "📈"; "Gold" -> "🪙"; "Property" -> "🏠"; "Vehicle" -> "🚗"
        "Home loan" -> "🏡"; "Car loan" -> "🚙"; "Personal loan" -> "🤝"; "Education loan" -> "🎓"; "Credit card" -> "💳"
        else -> "📦"
    }

    /** Uses the saved group when it is valid for the item type, otherwise guesses from the name. */
    fun groupOf(name: String, type: String, saved: String): String {
        val isLiability = type.equals("Liability", true)
        val pool = if (isLiability) liabilities else assets
        if (saved in pool) return saved
        val n = name.lowercase()
        fun has(vararg k: String) = k.any { n.contains(it) }
        fun word(vararg k: String) = k.any { Regex("\\b" + it + "\\b").containsMatchIn(n) }
        return if (isLiability) when {
            has("edu", "student", "study") -> "Education loan"
            has("card", "credit", "visa", "amex") -> "Credit card"
            has("home", "housing", "mortgage", "flat") -> "Home loan"
            word("car") || has("vehicle", "bike", "auto") -> "Car loan"
            has("loan", "emi", "personal", "borrow") -> "Personal loan"
            else -> "Other debt"
        } else when {
            has("gold", "silver", "sgb") -> "Gold"
            has("house", "flat", "land", "plot", "property", "home") -> "Property"
            word("car") || has("bike", "scooter", "vehicle") -> "Vehicle"
            has("stock", "share", "mutual", "fund", "sip", "etf", "ppf", "epf", "nps", "fd", "bond", "crypto", "invest") -> "Investments"
            has("cash", "bank", "saving", "account", "wallet", "upi") -> "Cash & bank"
            else -> "Other asset"
        }
    }

    /** Debt as a share of assets. Null when there are no assets. */
    fun debtRatio(assets: Double, liabilities: Double): Double? = if (assets <= 0) null else liabilities / assets * 100.0

    /** Plain-language band for the debt ratio. A rough rule of thumb, not advice. */
    fun band(ratioPct: Double?, liabilities: Double): String = when {
        liabilities <= 0 -> "Debt free"
        ratioPct == null -> "No assets recorded"
        ratioPct < 30 -> "Comfortable"
        ratioPct < 60 -> "Moderate"
        ratioPct <= 100 -> "Stretched"
        else -> "Owe more than you own"
    }
}
