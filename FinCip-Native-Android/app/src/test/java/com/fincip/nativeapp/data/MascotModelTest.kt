package com.fincip.nativeapp.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotModelTest {
    @Test fun fourCharactersWithDistinctDefaults() {
        assertEquals(4, MascotOptions.characters.size)
        assertEquals(4, MascotOptions.characters.map { it.name }.toSet().size)
        assertTrue("headphones" in MascotLook.defaultFor(2).accessories); assertTrue("glasses" in MascotLook.defaultFor(3).accessories)
        assertTrue(MascotOptions.character(0).female && !MascotOptions.character(1).female)
    }
    @Test fun jsonRoundTrip() {
        val l = MascotLook(2, "suit", -1, 0xFF112233L, "twin", 4, setOf("bow", "coin"), "night", "joy", true)
        assertEquals(l, MascotLook.fromJson(l.toJson()))
    }
    @Test fun garbageIsSanitised() {
        val o = MascotLook().toJson().apply { addProperty("character", 99); addProperty("outfit", "nope"); addProperty("hair", "x"); addProperty("hairColor", -5); addProperty("bg", "???") }
        val l = MascotLook.fromJson(o)
        assertEquals(3, l.character); assertEquals("hoodie", l.outfit); assertEquals("long", l.hair); assertEquals(0, l.hairColor); assertEquals("lavender", l.bg)
    }
    @Test fun missingMeansDefault() { assertEquals(MascotLook(), MascotLook.fromJson(null)); assertEquals(MascotLook(), MascotLook.fromJson(com.google.gson.JsonObject())) }
    @Test fun premiumLocksUntilLevel() {
        val l = MascotLook(outfit = "royal")
        assertEquals("hoodie", l.forLevel(1).outfit); assertEquals("royal", l.forLevel(MascotOptions.PREMIUM_LEVEL).outfit)
    }
    @Test fun randomIsValidAndRespectsLock() {
        val rng = Random(7)
        repeat(200) {
            val r = MascotLook.random(MascotLook.defaultFor(1), rng, 1)
            assertEquals(r, r.sanitized()); assertFalse(MascotOptions.outfits.first { o -> o.id == r.outfit }.premium); assertEquals(1, r.character)
        }
    }
    @Test fun customColourUsedWhenIndexIsMinusOne() {
        assertEquals(0xFF010203L, MascotLook(outfitColor = -1, customColor = 0xFF010203L).outfitArgb)
        assertEquals(MascotOptions.colors[4], MascotLook(outfitColor = 4).outfitArgb)
    }
}
