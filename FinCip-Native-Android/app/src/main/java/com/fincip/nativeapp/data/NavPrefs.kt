package com.fincip.nativeapp.data

import com.google.gson.JsonObject

/** A complete way of navigating the whole app. Exactly one is active at a time and each one reaches every page. */
data class NavLayout(val id: String, val title: String, val category: String, val blurb: String, val region: String, val looks: List<Pair<String, String>>)

object NavLayouts {
    const val DRAWER = "drawer"
    val categories = listOf("Sidebar", "Top", "Bottom", "Floating", "Overlay")
    val all = listOf(
        NavLayout("drawer", "Slide-in drawer", "Sidebar", "The classic menu: tap the logo, pick from grouped sections.", "drawer", emptyList()),
        NavLayout("rail", "Rail + flyout", "Sidebar", "A slim icon rail. Tap a section and its pages fly out beside it.", "side", listOf("flat" to "Flat", "floating" to "Floating", "glass" to "Glass")),
        NavLayout("collapsible", "Collapsible sidebar", "Sidebar", "Permanent sidebar that folds to icons and unfolds to every page.", "side", listOf("flat" to "Flat", "floating" to "Floating", "glass" to "Glass")),
        NavLayout("topTabs", "Two-level tabs", "Top", "Section tabs on top with that section's pages underneath.", "top", listOf("flat" to "Flat", "floating" to "Floating", "pill" to "Pill")),
        NavLayout("topChips", "Chip strip", "Top", "One scrolling strip with every page in it.", "top", listOf("flat" to "Flat", "floating" to "Floating", "pill" to "Pill", "compact" to "Compact")),
        NavLayout("bottomGroups", "Section tabs + pages", "Bottom", "Five section tabs, with the section's pages in a row above.", "bottom", listOf("flat" to "Classic", "floating" to "Floating", "pill" to "Pill", "curved" to "Curved", "compact" to "Compact")),
        NavLayout("bottomMore", "Quick tabs + More", "Bottom", "Your four main pages, plus More for a full-page picker.", "bottom", listOf("flat" to "Classic", "floating" to "Floating", "pill" to "Pill", "curved" to "Curved", "compact" to "Compact")),
        NavLayout("dock", "Floating dock", "Floating", "Four main tabs floating over your content. Tap one and its pages pop up as circles.", "overlay", listOf("rounded" to "Rounded", "glass" to "Glass", "minimal" to "Minimal icons", "labels" to "With labels")),
        NavLayout("pillMenu", "Expanding pill", "Floating", "A small floating pill of sections. Tap one and its pages pop up as circles.", "overlay", listOf("pill" to "Solid", "glass" to "Glass")),
        NavLayout("fab", "Fan menu button", "Floating", "One floating button that fans out into sections.", "overlay", listOf("round" to "Solid", "glass" to "Glass")),
        NavLayout("overlay", "Full-screen menu", "Overlay", "A menu button in the header opens a full-screen page picker.", "overlay", listOf("grid" to "Tiles", "list" to "List")),
        NavLayout("search", "Search menu", "Overlay", "Type to jump to any page, like a command palette.", "overlay", listOf("flat" to "Solid", "glass" to "Glass"))
    )
    fun of(id: String): NavLayout = all.firstOrNull { it.id == id } ?: all.first()
    fun inCategory(c: String) = all.filter { it.category == c }
}

/** One central description of how navigation behaves and looks. Every layout reads this and nothing else. */
data class NavPrefs(
    val layout: String = "drawer",
    val look: String = "flat",
    val labels: String = "both",        // both | icons | text
    val iconSize: Int = 24,
    val spacing: Int = 4,
    val bgAlpha: Float = 0.95f,
    val corner: Int = 24,
    val activeStyle: String = "pill",   // pill | underline | glow | none
    val height: Int = 64
) {
    val isDefaultDrawer: Boolean get() = layout == NavLayouts.DRAWER
    val region: String get() = NavLayouts.of(layout).region

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("layout", layout); addProperty("look", look); addProperty("labels", labels); addProperty("iconSize", iconSize)
        addProperty("spacing", spacing); addProperty("bgAlpha", bgAlpha); addProperty("corner", corner); addProperty("activeStyle", activeStyle); addProperty("height", height)
    }

    /** Never trust stored values: unknown or out-of-range input falls back to something valid. */
    fun sanitized(): NavPrefs {
        val l = NavLayouts.all.firstOrNull { it.id == layout } ?: NavLayouts.all.first()
        val looks = l.looks.map { it.first }
        return copy(
            layout = l.id, look = if (look in looks) look else looks.firstOrNull() ?: "flat",
            labels = if (labels in LABEL_MODES) labels else "both", iconSize = iconSize.coerceIn(18, 34), spacing = spacing.coerceIn(0, 16),
            bgAlpha = bgAlpha.coerceIn(0.3f, 1f), corner = corner.coerceIn(0, 36), activeStyle = if (activeStyle in ACTIVE_STYLES) activeStyle else "pill", height = height.coerceIn(48, 96)
        )
    }

    companion object {
        val LABEL_MODES = listOf("both", "icons", "text")
        val ACTIVE_STYLES = listOf("pill", "underline", "glow", "none")

        fun fromJson(o: JsonObject?): NavPrefs {
            if (o == null) return NavPrefs()
            fun s(k: String, d: String) = if (o.has(k) && o.get(k).isJsonPrimitive) o.get(k).asString else d
            fun n(k: String, d: Double) = if (o.has(k) && o.get(k).isJsonPrimitive) runCatching { o.get(k).asDouble }.getOrDefault(d) else d
            val d = NavPrefs()
            // settings saved by the first version used position/style; map them onto the new layouts
            val layout = if (o.has("layout")) s("layout", d.layout) else when (s("position", "left")) {
                "left" -> if (s("style", "expanded") == "expanded") "drawer" else "rail"
                "top" -> "topChips"; "bottom" -> "bottomMore"; "dock" -> "dock"; else -> "drawer"
            }
            return NavPrefs(layout, s("look", d.look), s("labels", d.labels), n("iconSize", d.iconSize.toDouble()).toInt(), n("spacing", d.spacing.toDouble()).toInt(),
                n("bgAlpha", d.bgAlpha.toDouble()).toFloat(), n("corner", d.corner.toDouble()).toInt(), s("activeStyle", d.activeStyle), n("height", d.height.toDouble()).toInt()).sanitized()
        }
    }
}

/** Every destination, grouped. All layouts draw from this one list, so none can hide a page. */
object NavDests {
    val groups = listOf("Home", "Money", "Plan", "Explore", "You")
    val all: List<NavPage> = NAV_PAGES + listOf(NavPage("profile", "Profile", "You", "☺"), NavPage("customize", "Customize", "You", "✎"))
    /** The floating dock shows four tabs; Explore and You share "More" so every page is still reachable. */
    val dockTabs = listOf("Home", "Money", "Plan", "More")
    fun tabPages(tab: String): List<NavPage> = if (tab == "More") pagesIn("Explore") + pagesIn("You") else pagesIn(tab)
    fun dockTabOf(page: String): String = groupOf(page).let { if (it == "Explore" || it == "You") "More" else it }
    val quick = listOf("overview", "transactions", "budgets", "world")

    fun title(id: String) = all.firstOrNull { it.id == id.substringBefore(":") }?.title ?: "FinCip"
    fun groupOf(page: String): String = all.firstOrNull { it.id == page.substringBefore(":") }?.group
        ?: if (page.substringBefore(":") in setOf("settings", "security")) "Explore" else "Home"
    fun pagesIn(group: String) = all.filter { it.group == group }
    /** Settings sub-pages like "settings:vibe" count as their parent for highlighting. */
    fun baseId(page: String) = page.substringBefore(":")
}
