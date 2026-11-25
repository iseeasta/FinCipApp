package com.fincip.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavPrefsTest {
    @Test fun defaultIsClassicDrawer() { assertTrue(NavPrefs().isDefaultDrawer) }
    @Test fun roundTrip() {
        val p = NavPrefs("bottom", "curved", "icons", 28, 8, 0.7f, 30, "glow", 72)
        assertEquals(p, NavPrefs.fromJson(p.toJson()))
    }
    @Test fun everyPositionHasStylesAndTheDefaultIsValid() {
        NavPrefs.POSITIONS.forEach { pos -> assertTrue(pos, NavPrefs.stylesFor(pos).size >= 4) }
        assertEquals(5, NavPrefs.stylesFor("left").size); assertEquals(5, NavPrefs.stylesFor("top").size)
        assertEquals(5, NavPrefs.stylesFor("bottom").size); assertEquals(4, NavPrefs.stylesFor("dock").size)
    }
    @Test fun incompatibleStyleIsReplaced() {
        assertEquals("rounded", NavPrefs("dock", "curved").sanitized().style)
        assertEquals("classic", NavPrefs("bottom", "expanded").sanitized().style)
    }
    @Test fun garbageIsClamped() {
        val s = NavPrefs("sideways", "x", "weird", 500, -4, 9f, 999, "??", 1).sanitized()
        assertEquals("left", s.position); assertEquals("expanded", s.style); assertEquals("both", s.labels)
        assertEquals(34, s.iconSize); assertEquals(0, s.spacing); assertEquals(1f, s.bgAlpha, 0f); assertEquals(36, s.corner); assertEquals("pill", s.activeStyle); assertEquals(48, s.height)
    }
    @Test fun missingJsonGivesDefault() { assertEquals(NavPrefs(), NavPrefs.fromJson(null)); assertEquals(NavPrefs(), NavPrefs.fromJson(com.google.gson.JsonObject())) }
    @Test fun narrowScreenHidesLabelsAndSaysSo() {
        val (p, note) = NavPrefs("bottom", "classic", "both").fitted(300, 6)
        assertEquals("icons", p.labels); assertNotNull(note)
        val (q, n2) = NavPrefs("bottom", "classic", "both").fitted(411, 6)
        assertEquals("both", q.labels); assertNull(n2)
        assertNull(NavPrefs("left", "compact").fitted(200, 6).second)
    }
    @Test fun activeItemMapping() {
        assertEquals("overview", NavItems.activeFor("overview")); assertEquals("settings", NavItems.activeFor("settings:vibe"))
        assertEquals("settings", NavItems.activeFor("profile")); assertEquals(NavItems.MORE, NavItems.activeFor("goals")); assertEquals("portfolio", NavItems.activeFor("portfolio"))
    }
}
