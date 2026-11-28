package com.fincip.nativeapp.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test

class AchievementsTest {
    @Test fun exactlyOneHundredDistinct() {
        val a = Achievements.all
        assertEquals(100, a.size)
        assertEquals(100, a.map { it.title }.toSet().size)
        assertEquals("every badge design (glyph + shape) is unique", 100, a.map { it.glyph to it.shape }.toSet().size)
        assertTrue(a.all { it.t1 < it.t2 && it.t2 < it.t3 })
        assertTrue(a.all { it.glyph in 0 until Achievements.GLYPHS && it.shape in 0 until Achievements.SHAPES })
    }
    @Test fun emptyWorkspaceUnlocksNothing() {
        val ws = defaultWorkspace()
        val st = Achievements.evaluate(Achievements.stats(ws, 1, 0))
        assertEquals(0, st.sumOf { it.tier })
    }
    @Test fun tiersFollowTheNumbers() {
        val ws = defaultWorkspace()
        val arr = JsonArray()
        repeat(12) { i -> arr.add(JsonObject().apply { addProperty("id", "t$i"); addProperty("title", "Lunch $i"); addProperty("amount", 200); addProperty("currency", "INR"); addProperty("category", "Food"); addProperty("date", "2026-11-%02d".format(i + 1)); addProperty("type", "expense") }) }
        ws.add("transactions", arr)
        val st = Achievements.evaluate(Achievements.stats(ws, 1, 120, java.time.LocalDate.of(2026, 11, 12))).associateBy { it.def.title }
        assertEquals(2, st["First Steps"]!!.tier)          // 12 entries: 1 and 10 met, 50 not
        assertEquals(1, st["Foodie"]!!.tier)               // 12 food entries: 5 met, 30 not
    }
}
