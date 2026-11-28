package com.fincip.nativeapp.data

import org.junit.Assert.*
import org.junit.Test

class SocialTest {
    private val foodie = Signature(mapOf("food" to .6, "travel" to .4), setOf("budgets", "goals"), 4)
    private val foodie2 = Signature(mapOf("food" to .5, "travel" to .3, "bills" to .2), setOf("budgets", "goals", "saving"), 5)
    private val gamer = Signature(mapOf("games" to .8, "bills" to .2), setOf("investing"), 12)

    @Test fun similarIsCloserThanDifferent() { assertTrue(Social.similarity(foodie, foodie2) > Social.similarity(foodie, gamer)) }
    @Test fun symmetricAndBounded() {
        assertEquals(Social.similarity(foodie, gamer), Social.similarity(gamer, foodie), 1e-9)
        listOf(foodie, foodie2, gamer, Signature(emptyMap(), emptySet(), 1)).forEach { a -> listOf(foodie, gamer).forEach { b -> assertTrue(Social.similarity(a, b) in 0.0..1.0) } }
    }
    @Test fun identicalIsOne() { assertEquals(1.0, Social.similarity(foodie, foodie), 1e-9) }
    @Test fun orbitsAreOrderedAndSpaced() {
        val o = Social.orbits(listOf("a" to .9, "b" to .91, "c" to .2, "d" to .5), 90.0, 300.0, 24.0)
        assertEquals("b", o.first().id)
        o.zipWithNext().forEach { (x, y) -> assertTrue(y.radius - x.radius >= 24.0 - 1e-9); assertTrue(y.similarity <= x.similarity) }
        assertTrue(o.first().radius >= 90.0)
    }
    @Test fun qrRoundTrip() {
        val tok = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8"
        assertEquals(tok, Social.parseQr(Social.qrPayload(tok)))
        assertNull(Social.parseQr("https://evil.example/x")); assertNull(Social.parseQr("fincip://friend/short")); assertNull(Social.parseQr(null))
    }
    @Test fun signatureJsonRoundTrip() { assertEquals(foodie.tags, Signature.fromJson(foodie.toJson()).tags); assertEquals(4, Signature.fromJson(foodie.toJson()).level) }
    @Test fun signatureHasNoAmounts() {
        val ws = defaultWorkspace(); val s = Social.signature(ws, 3).toJson().toString()
        assertFalse(s.contains("amount")); assertEquals(3, Social.signature(ws, 3).level)
    }
}
