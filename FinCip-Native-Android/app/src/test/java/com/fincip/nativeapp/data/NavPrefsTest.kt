package com.fincip.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavPrefsTest {
    @Test fun defaultIsDrawer() { assertTrue(NavPrefs().isDefaultDrawer); assertEquals(NavPrefs(), NavPrefs.fromJson(null)) }
    @Test fun roundTrip() {
        val p = NavPrefs("bottomGroups", "curved", "icons", 28, 8, 0.7f, 30, "glow", 72)
        assertEquals(p, NavPrefs.fromJson(p.toJson()))
    }
    @Test fun layoutIdsAreUniqueAndEveryRegionIsKnown() {
        assertEquals(NavLayouts.all.size, NavLayouts.all.map { it.id }.toSet().size)
        assertTrue(NavLayouts.all.size >= 11)
        NavLayouts.all.forEach { assertTrue(it.id, it.region in setOf("drawer", "side", "top", "bottom", "overlay")) }
        NavLayouts.categories.forEach { c -> assertTrue(c, NavLayouts.inCategory(c).isNotEmpty()) }
    }
    @Test fun badLookIsReplacedByAValidOne() {
        assertEquals("rounded", NavPrefs("dock", "curved").sanitized().look)
        assertEquals("flat", NavPrefs("bottomMore", "rounded").sanitized().look)
    }
    @Test fun garbageIsClamped() {
        val s = NavPrefs("nope", "x", "weird", 500, -4, 9f, 999, "??", 1).sanitized()
        assertEquals("drawer", s.layout); assertEquals("both", s.labels); assertEquals(34, s.iconSize); assertEquals(0, s.spacing)
        assertEquals(1f, s.bgAlpha, 0f); assertEquals(36, s.corner); assertEquals("pill", s.activeStyle); assertEquals(48, s.height)
    }
    @Test fun oldPositionStyleSettingsAreMigrated() {
        fun old(pos: String, style: String) = com.google.gson.JsonObject().apply { addProperty("position", pos); addProperty("style", style) }
        assertEquals("drawer", NavPrefs.fromJson(old("left", "expanded")).layout)
        assertEquals("rail", NavPrefs.fromJson(old("left", "glass")).layout)
        assertEquals("topChips", NavPrefs.fromJson(old("top", "pill")).layout)
        assertEquals("bottomMore", NavPrefs.fromJson(old("bottom", "classic")).layout)
        assertEquals("dock", NavPrefs.fromJson(old("dock", "rounded")).layout)
    }
    @Test fun everyExistingPageIsReachable() {
        NAV_PAGES.forEach { p -> assertTrue(p.id, NavDests.all.any { it.id == p.id }) }
        NavDests.all.forEach { assertTrue(it.id, it.group in NavDests.groups) }
        NavDests.groups.forEach { assertTrue(it, NavDests.pagesIn(it).isNotEmpty()) }
        assertTrue(NavDests.all.any { it.id == "profile" } && NavDests.all.any { it.id == "customize" })
    }
    @Test fun groupLookup() {
        assertEquals("Money", NavDests.groupOf("transactions")); assertEquals("Explore", NavDests.groupOf("settings:vibe"))
        assertEquals("You", NavDests.groupOf("customize")); assertEquals("Plan", NavDests.groupOf("goals"))
        assertEquals("Settings & vibe", NavDests.title("settings:x"))
    }
    @Test fun quickPagesExist() { NavDests.quick.forEach { q -> assertTrue(q, NavDests.all.any { it.id == q }) } }
}
