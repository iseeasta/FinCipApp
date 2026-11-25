package com.fincip.nativeapp.world

import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldBrainTest {
    @Test fun daylightCurve() {
        assertEquals(0.0, Daylight.level(2.0), 0.0); assertEquals(1.0, Daylight.level(12.0), 0.0); assertEquals(0.0, Daylight.level(22.0), 0.0)
        assertTrue(Daylight.level(6.5) in 0.3..0.7); assertTrue(Daylight.level(18.5) in 0.3..0.7)
        assertTrue(Daylight.isNight(23.0)); assertTrue(!Daylight.isNight(12.0)); assertEquals(0.0, Daylight.level(-1.0), 0.0)
    }
    @Test fun weatherIsDeterministicAndVaried() {
        val d = LocalDate.of(2026, 11, 25)
        assertEquals(Weather.auto(d, 9), Weather.auto(d, 9))
        val seen = (0 until 200).map { Weather.auto(d.plusDays(it.toLong()), 12) }.toSet()
        assertTrue(seen.size >= 3)
    }
    @Test fun snowOnlyInColdMonths() {
        val summer = (0 until 300).map { Weather.auto(LocalDate.of(2026, 7, 1).plusDays((it % 30).toLong()), (it % 6) * 4) }
        assertTrue(Weather.SNOW !in summer)
        val winter = (0 until 400).map { Weather.auto(LocalDate.of(2026, 1, 1).plusDays((it % 28).toLong()), (it % 6) * 4) }
        assertTrue(Weather.RAIN !in winter)
    }
    @Test fun nightMeansSleep() {
        val picks = (0 until 200).map { Routine.next("tv", 2.0, Weather.CLEAR, Random(it)).id }
        assertTrue(picks.count { it == "sleep" } > 170)
    }
    @Test fun morningChain() {
        val r = Random(3)
        assertEquals("wake", Routine.next("sleep", 7.0, Weather.CLEAR, r).id)
        assertEquals("brush", Routine.next("wake", 7.0, Weather.CLEAR, r).id)
        assertEquals("dress", Routine.next("brush", 7.2, Weather.CLEAR, r).id)
        assertEquals("breakfast", Routine.next("dress", 7.4, Weather.CLEAR, r).id)
    }
    @Test fun noGardenInRainAndNoRepeat() {
        repeat(300) {
            val a = Routine.next("study", 15.0, Weather.RAIN, Random(it))
            assertNotEquals("garden", a.id); assertNotEquals("exercise", a.id); assertNotEquals("study", a.id)
        }
    }
    @Test fun daytimeIsWorkHeavyAndEveningIsCozy() {
        val day = (0 until 500).map { Routine.next("tv", 11.0, Weather.CLEAR, Random(it)).id }
        assertTrue(day.count { it == "study" || it == "work" } > 100)
        val eve = (0 until 500).map { Routine.next("study", 20.0, Weather.CLEAR, Random(it)).id }
        assertTrue(eve.count { it == "tv" || it == "relax" } > 100)
        assertTrue(eve.none { it == "work" })
    }
    @Test fun everyActivityHasValidRoomAndPose() {
        Activities.all.forEach { a ->
            assertTrue(a.id, Rooms.all.any { it.id == a.room }); assertTrue(a.id, a.pose in Poses.all)
            assertTrue(a.id, a.anchorX in 40f..1160f); assertTrue(a.id, a.minSec in 1..a.maxSec)
        }
        Rooms.all.forEach { r -> assertTrue(r.id, Activities.inRoom(r.id).isNotEmpty()); assertEquals(Activities.defaultFor(r.id).room, r.id) }
        assertEquals(6, Rooms.all.size)
    }
    @Test fun poseAnglesAreFiniteAndSane() {
        Poses.all.forEach { id ->
            for (k in 0..50) {
                val p = Poses.at(id, k * 0.37f, false)
                listOf(p.lift, p.bob, p.bodyRot, p.lean, p.headTilt, p.armL, p.armLb, p.armR, p.armRb, p.legL, p.legLb, p.legR, p.legRb).forEach { v -> assertTrue("$id $v", v.isFinite() && kotlin.math.abs(v) <= 200f) }
            }
        }
        val w = Poses.at("idle", 1f, true); assertTrue(w.walking)
        assertTrue(Poses.at("sleep", 0f, false).bodyRot < -80f && Poses.at("sleep", 0f, false).eyesClosed)
    }
    @Test fun outfitRules() {
        assertEquals("pajamas", Looks.autoFor("sleep", Weather.CLEAR, false, 3.0, 6, 0.0))
        assertEquals("sporty", Looks.autoFor("exercise", Weather.CLEAR, true, 7.0, 6, 0.0))
        assertEquals("winter", Looks.autoFor("garden", Weather.RAIN, true, 12.0, 6, 0.0))
        assertEquals("winter", Looks.autoFor("garden", Weather.CLEAR, true, 12.0, 1, 0.0))
        assertEquals("school", Looks.autoFor("study", Weather.CLEAR, false, 11.0, 6, 0.0))
        assertEquals("elegant", Looks.autoFor("dance", Weather.CLEAR, false, 20.0, 6, 0.9))
        assertEquals("default", Looks.autoFor("tv", Weather.CLEAR, false, 19.0, 6, 0.0))
        assertEquals(6, Looks.all.size); assertEquals("default", Looks.of("nope").id)
    }
    @Test fun durationsStayInRange() {
        Activities.all.forEach { a -> repeat(20) { val d = Routine.durationSec(a, Random(it)); assertTrue(d in a.minSec..a.maxSec) } }
    }
}
