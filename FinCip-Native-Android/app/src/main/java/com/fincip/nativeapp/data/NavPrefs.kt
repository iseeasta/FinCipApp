package com.fincip.nativeapp.data

import com.google.gson.JsonObject

/** One central description of how primary navigation looks and where it sits. Every layout reads this and nothing else. */
data class NavPrefs(
    val position: String = "left",      // left | top | bottom | dock
    val style: String = "expanded",
    val labels: String = "both",        // both | icons | text
    val iconSize: Int = 24,             // dp
    val spacing: Int = 4,               // dp between items
    val bgAlpha: Float = 0.95f,
    val corner: Int = 24,               // dp, for rounded styles
    val activeStyle: String = "pill",   // pill | underline | glow | none
    val height: Int = 64                // dp, bar height (or rail width / 1.1 for left)
) {
    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("position", position); addProperty("style", style); addProperty("labels", labels); addProperty("iconSize", iconSize)
        addProperty("spacing", spacing); addProperty("bgAlpha", bgAlpha); addProperty("corner", corner); addProperty("activeStyle", activeStyle); addProperty("height", height)
    }

    /** Never trust stored values: unknown or out-of-range input falls back to something valid. */
    fun sanitized(): NavPrefs {
        val pos = if (position in POSITIONS) position else "left"
        val styles = stylesFor(pos).map { it.first }
        return copy(
            position = pos, style = if (style in styles) style else styles.first(),
            labels = if (labels in LABEL_MODES) labels else "both", iconSize = iconSize.coerceIn(18, 34), spacing = spacing.coerceIn(0, 16),
            bgAlpha = bgAlpha.coerceIn(0.3f, 1f), corner = corner.coerceIn(0, 36), activeStyle = if (activeStyle in ACTIVE_STYLES) activeStyle else "pill", height = height.coerceIn(48, 96)
        )
    }

    /** True when the app should keep the classic slide-in drawer exactly as before. */
    val isDefaultDrawer: Boolean get() = position == "left" && style == "expanded"

    /**
     * Fits the preference to the screen. Returns the adjusted prefs and a plain explanation when something had to change,
     * so the UI can tell the person instead of silently overriding their choice.
     */
    fun fitted(widthDp: Int, itemCount: Int): Pair<NavPrefs, String?> {
        if (position == "left") return this to null
        val perItem = widthDp / itemCount.coerceAtLeast(1)
        if (position != "top" && labels != "icons" && perItem < 58) return copy(labels = "icons") to "Your screen is narrow, so labels are hidden to keep every item reachable."
        return this to null
    }

    companion object {
        val POSITIONS = listOf("left", "top", "bottom", "dock")
        val LABEL_MODES = listOf("both", "icons", "text")
        val ACTIVE_STYLES = listOf("pill", "underline", "glow", "none")
        const val DEFAULT_ITEMS = 6

        fun stylesFor(position: String): List<Pair<String, String>> = when (position) {
            "left" -> listOf("expanded" to "Expanded", "compact" to "Compact icons", "floating" to "Floating", "glass" to "Glass", "collapsible" to "Collapsible")
            "top" -> listOf("leftAligned" to "Left-aligned", "centered" to "Centered", "floating" to "Floating", "pill" to "Pill", "compact" to "Compact")
            "bottom" -> listOf("classic" to "Classic tabs", "floating" to "Floating dock", "pill" to "Pill", "curved" to "Curved", "compact" to "Compact")
            "dock" -> listOf("rounded" to "Rounded dock", "glass" to "Glass dock", "minimal" to "Minimal icons", "labels" to "Dock with labels")
            else -> emptyList()
        }
        fun positionLabel(p: String) = when (p) { "left" -> "Left sidebar"; "top" -> "Top bar"; "bottom" -> "Bottom bar"; else -> "Floating dock" }

        fun fromJson(o: JsonObject?): NavPrefs {
            if (o == null) return NavPrefs()
            fun s(k: String, d: String) = if (o.has(k) && o.get(k).isJsonPrimitive) o.get(k).asString else d
            fun n(k: String, d: Double) = if (o.has(k) && o.get(k).isJsonPrimitive) runCatching { o.get(k).asDouble }.getOrDefault(d) else d
            val d = NavPrefs()
            return NavPrefs(s("position", d.position), s("style", d.style), s("labels", d.labels), n("iconSize", d.iconSize.toDouble()).toInt(), n("spacing", d.spacing.toDouble()).toInt(),
                n("bgAlpha", d.bgAlpha.toDouble()).toFloat(), n("corner", d.corner.toDouble()).toInt(), s("activeStyle", d.activeStyle), n("height", d.height.toDouble()).toInt()).sanitized()
        }
    }
}

/** The destinations every layout shows. Everything else stays reachable through "More", which opens the full menu. */
data class NavItemDef(val id: String, val label: String)

object NavItems {
    val primary = listOf(
        NavItemDef("overview", "Home"), NavItemDef("transactions", "Money"), NavItemDef("budgets", "Budgets"),
        NavItemDef("portfolio", "Portfolio"), NavItemDef("settings", "Settings")
    )
    const val MORE = "more"

    /** Which item should look selected for a given page id. Pages that are not primary light up "More". */
    fun activeFor(page: String): String {
        val base = page.substringBefore(":")
        return when (base) {
            in primary.map { it.id } -> base
            "profile", "security", "customize", "navstudio" -> "settings"
            else -> MORE
        }
    }
}
