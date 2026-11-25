package com.fincip.nativeapp.world

import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

/** Pure logic for FinKip Girl's world: rooms, routine, weather, daylight and outfits. No Android or Compose here, so it is unit-tested. */

data class RoomDef(val id: String, val title: String, val emoji: String, val doorX: Float)

object Rooms {
    const val W = 1200f
    const val FLOOR = 470f
    val all = listOf(
        RoomDef("bedroom", "Bedroom", "🛏️", 80f),
        RoomDef("living", "Living room", "🛋️", 80f),
        RoomDef("kitchen", "Kitchen", "🍳", 80f),
        RoomDef("study", "Study room", "📚", 80f),
        RoomDef("garden", "Garden", "🌳", 80f),
        RoomDef("relax", "Relax room", "🕯️", 80f)
    )
    fun of(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}

/** [anchorX] is where she stands or sits for it, [pose] picks her animation. */
data class Activity(val id: String, val label: String, val room: String, val anchorX: Float, val pose: String, val minSec: Int, val maxSec: Int)

object Activities {
    val all = listOf(
        Activity("sleep", "Sleeping", "bedroom", 230f, "sleep", 150, 300),
        Activity("wake", "Waking up", "bedroom", 330f, "stretch", 8, 12),
        Activity("brush", "Brushing teeth", "bedroom", 700f, "brush", 10, 16),
        Activity("dress", "Getting dressed", "bedroom", 520f, "dress", 8, 12),
        Activity("mirror", "Checking her look", "bedroom", 700f, "mirror", 10, 18),
        Activity("window", "Looking outside", "bedroom", 960f, "think", 14, 30),
        Activity("breakfast", "Eating breakfast", "kitchen", 640f, "eat", 20, 34),
        Activity("cook", "Cooking", "kitchen", 330f, "cook", 20, 40),
        Activity("snack", "Fridge raid", "kitchen", 960f, "eat", 10, 18),
        Activity("study", "Studying", "study", 520f, "write", 35, 70),
        Activity("work", "Working", "study", 520f, "type", 40, 80),
        Activity("readStudy", "Reading", "study", 930f, "read", 25, 50),
        Activity("exercise", "Exercising", "garden", 560f, "exercise", 20, 36),
        Activity("garden", "Enjoying the garden", "garden", 900f, "sitBench", 25, 50),
        Activity("tv", "Watching TV", "living", 470f, "watch", 35, 70),
        Activity("phone", "On her phone", "living", 470f, "phone", 20, 40),
        Activity("readLiving", "Reading", "living", 920f, "read", 25, 50),
        Activity("relax", "Relaxing", "relax", 520f, "sitBean", 40, 80),
        Activity("dance", "Dancing", "relax", 860f, "dance", 14, 26),
        Activity("yoga", "Stretching", "relax", 860f, "stretch", 14, 22)
    )
    fun of(id: String) = all.firstOrNull { it.id == id } ?: all.first()
    fun inRoom(room: String) = all.filter { it.room == room }
    fun defaultFor(room: String): Activity = when (room) {
        "bedroom" -> of("mirror"); "living" -> of("tv"); "kitchen" -> of("cook"); "study" -> of("study"); "garden" -> of("garden"); else -> of("relax")
    }
}

object Weather {
    const val CLEAR = "clear"; const val CLOUDY = "cloudy"; const val RAIN = "rain"; const val SNOW = "snow"
    val choices = listOf("auto" to "Auto", CLEAR to "Sunny", CLOUDY to "Cloudy", RAIN to "Rain", SNOW to "Snow")

    /** Deterministic per day and 4-hour block, so it changes gently and is the same every time you look. */
    fun auto(date: LocalDate, hour: Int): String {
        val seed = date.toEpochDay() * 6 + hour / 4
        val r = Random(seed).nextDouble()
        val cold = date.monthValue == 12 || date.monthValue <= 2
        return when { r < .18 -> if (cold) SNOW else RAIN; r < .5 -> CLOUDY; else -> CLEAR }
    }
}

object Daylight {
    /** 0 = deep night, 1 = full day, with a soft dawn (5:30-7:30) and dusk (17:30-19:30). */
    fun level(hour: Double): Double {
        val h = ((hour % 24) + 24) % 24
        return when {
            h < 5.5 -> 0.0
            h < 7.5 -> (h - 5.5) / 2.0
            h < 17.5 -> 1.0
            h < 19.5 -> 1.0 - (h - 17.5) / 2.0
            else -> 0.0
        }.coerceIn(0.0, 1.0)
    }
    fun isNight(hour: Double) = level(hour) < .25
    fun partOfDay(hour: Double): String { val h = hour.toInt(); return when (h) { in 5..10 -> "morning"; in 11..16 -> "afternoon"; in 17..21 -> "evening"; else -> "night" } }
}

data class Look(
    val id: String, val label: String, val topKind: String, val top: Long, val bottomKind: String, val bottom: Long,
    val sock: Long, val shoe: Long, val accent: Long, val extra: String, val longSleeves: Boolean
)

object Looks {
    val all = listOf(
        Look("default", "Cute casual", "sweater", 0xFFF7A8C4, "ruffle", 0xFFFFFFFF, 0xFFFFFFFF, 0xFFF4B6CC, 0xFFFFFFFF, "bow", true),
        Look("pajamas", "Cozy home", "pajama", 0xFFF9B9CF, "pants", 0xFFF7C6D8, 0xFFFFE6EF, 0xFFF2D4DE, 0xFFFFFFFF, "none", true),
        Look("school", "School", "shirt", 0xFFFFFFFF, "plaid", 0xFF3A2F3F, 0xFFFFFFFF, 0xFF3B2A2A, 0xFFB5202E, "ribbon", false),
        Look("sporty", "Sporty", "crop", 0xFFF08BB0, "pants", 0xFFF4A7C3, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, "headband", false),
        Look("winter", "Winter", "coat", 0xFFE8D2B0, "plaid", 0xFF4B3A3F, 0xFFE8D2B0, 0xFF4A332B, 0xFFFFF5F0, "beret", true),
        Look("elegant", "Elegant", "dress", 0xFFF8B8D2, "dressSkirt", 0xFFF8C6DC, 0xFFFFFFFF, 0xFFF4A6C2, 0xFFFFFFFF, "bow", false)
    )
    fun of(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    /** What she would pick for the moment. The person's own pick always wins when it is not "auto". */
    fun autoFor(activityId: String, weather: String, outdoors: Boolean, hour: Double, month: Int, partyChance: Double): String = when {
        activityId in setOf("sleep", "wake", "brush") -> "pajamas"
        activityId == "exercise" || activityId == "yoga" -> "sporty"
        outdoors && (weather == Weather.RAIN || weather == Weather.SNOW || month == 12 || month <= 2) -> "winter"
        activityId == "dance" && partyChance > .55 -> "elegant"
        activityId in setOf("study", "work", "readStudy") && hour < 17 -> "school"
        hour >= 21.5 || hour < 6 -> "pajamas"
        else -> "default"
    }
}

object Routine {
    /** Weight of each activity at a given hour. 0 means "not now". */
    fun weight(a: Activity, hour: Double, weather: String): Double {
        val h = hour
        val night = h >= 23 || h < 5.5
        val morning = h in 5.5..10.0
        val work = h in 9.0..17.5
        val evening = h in 17.5..22.5
        val wet = weather == Weather.RAIN || weather == Weather.SNOW
        return when (a.id) {
            "sleep" -> if (night) 20.0 else if (h >= 22.5) 4.0 else 0.0
            "wake" -> 0.0
            "brush" -> if (morning) 1.0 else if (h >= 22 && h < 23) 3.0 else 0.0
            "dress" -> if (morning) 2.0 else 0.0
            "mirror" -> if (!night) 1.2 else 0.0
            "window" -> if (!night) 1.0 else 0.4
            "breakfast" -> if (morning) 4.0 else 0.0
            "cook" -> if (h in 11.0..14.0 || h in 18.0..20.5) 3.0 else 0.4
            "snack" -> if (!night) 1.0 else 0.2
            "study" -> if (work) 3.5 else if (evening) 1.0 else 0.0
            "work" -> if (work) 3.5 else 0.0
            "readStudy", "readLiving" -> if (night) 0.0 else 1.5
            "exercise" -> if (wet || night) 0.0 else if (morning || h in 16.0..19.0) 2.5 else 0.3
            "garden" -> if (wet || night) 0.0 else 2.0
            "tv" -> if (evening) 3.0 else if (night) 0.0 else 0.6
            "phone" -> if (night) 0.0 else 1.5
            "relax" -> if (evening) 2.5 else if (night) 0.0 else 1.0
            "dance" -> if (night) 0.0 else 0.7
            "yoga" -> if (night) 0.0 else 0.8
            else -> 0.5
        }
    }

    /** What follows [prev]: the morning chain is fixed, everything else is a weighted pick that avoids repeating. */
    fun next(prev: String?, hour: Double, weather: String, rng: Random): Activity {
        val morning = hour in 5.0..10.5
        if (prev == "sleep" && !(hour >= 22.5 || hour < 5.0)) return Activities.of("wake")
        if (prev == "wake") return Activities.of("brush")
        if (prev == "brush" && morning) return Activities.of("dress")
        if (prev == "dress" && morning) return Activities.of("breakfast")
        val pool = Activities.all.map { it to weight(it, hour, weather) }.filter { (a, w) -> w > 0 && a.id != prev }
        if (pool.isEmpty()) return Activities.of("relax")
        val total = pool.sumOf { it.second }
        var roll = rng.nextDouble() * total
        for ((a, w) in pool) { roll -= w; if (roll <= 0) return a }
        return pool.last().first
    }

    fun durationSec(a: Activity, rng: Random): Int = a.minSec + rng.nextInt((a.maxSec - a.minSec).coerceAtLeast(1) + 1)

    /** A plain-language outline of her usual day, for the "Her day" panel. */
    val outline = listOf(
        "6–9" to "Wakes up, stretches, brushes her teeth, gets dressed and has breakfast",
        "9–17" to "Studies or works at her desk, with breaks for reading, a snack or the garden",
        "16–19" to "Exercise in the garden when the weather is nice",
        "18–22" to "Cooks, watches TV, relaxes, dances a little",
        "22–6" to "Pyjamas, bed, and a long sleep"
    )
}

/** Body angles for a pose at time [t] seconds. Angles are degrees; 0 points straight down, positive swings forward. */
data class Pose(
    val lift: Float = 120f, val bob: Float = 0f, val bodyRot: Float = 0f, val lean: Float = 0f, val headTilt: Float = 0f,
    val armL: Float = 6f, val armLb: Float = 8f, val armR: Float = 6f, val armRb: Float = 8f,
    val legL: Float = 0f, val legLb: Float = 0f, val legR: Float = 0f, val legRb: Float = 0f,
    val face: String = "smile", val eyesClosed: Boolean = false, val prop: String = "", val walking: Boolean = false
)

object Poses {
    private fun s(x: Double) = kotlin.math.sin(x).toFloat()
    private const val SIT = 66f

    fun at(id: String, t: Float, walking: Boolean): Pose {
        if (walking) {
            val p = t * 7.0
            return Pose(bob = abs(s(p)) * 4f, lean = 3f, armL = 24f * s(p), armLb = 10f, armR = -24f * s(p), armRb = 10f,
                legL = 30f * s(p), legLb = -(maxOf(0f, -s(p))) * 36f, legR = -30f * s(p), legRb = -(maxOf(0f, s(p))) * 36f, walking = true)
        }
        val sit = Pose(lift = SIT, legL = 88f, legLb = -88f, legR = 80f, legRb = -80f)
        return when (id) {
            "sleep" -> Pose(lift = 84f, bodyRot = -90f, bob = s(t * 1.2.toDouble()) * 1.5f, armL = 4f, armR = 4f, legL = 2f, legR = 2f, face = "sleep", eyesClosed = true)
            "stretch" -> Pose(bob = s(t * 1.6.toDouble()) * 2f, lean = -4f, armL = 170f + 6f * s(t * 2.0), armLb = 4f, armR = 170f + 6f * s(t * 2.0), armRb = 4f, face = "laugh", eyesClosed = true)
            "brush" -> Pose(bob = s(t * 2.0) * 1f, armR = 70f, armRb = 95f + 14f * s(t * 12.0), armL = 8f, face = "talk", prop = "brush")
            "dress" -> Pose(bob = s(t * 3.0) * 2f, lean = s(t * 3.0) * 5f, armL = 30f + 30f * s(t * 3.0), armR = 30f - 30f * s(t * 3.0), armLb = 30f, armRb = 30f, face = "happy")
            "mirror" -> Pose(headTilt = 6f * s(t * 1.2.toDouble()), armR = 100f, armRb = 110f + 5f * s(t * 2.0), armL = 8f, face = "wink")
            "think" -> Pose(headTilt = -7f, armR = 70f, armRb = 125f, armL = 8f, face = "smile", bob = s(t * 1.3.toDouble()) * 1.5f)
            "eat" -> Pose(armR = 55f + 30f * maxOf(0f, s(t * 2.4.toDouble())), armRb = 105f, armL = 20f, armLb = 50f, face = "happy", prop = "cup", bob = s(t * 2.4.toDouble()) * 1.5f)
            "cook" -> Pose(armR = 40f, armRb = 70f + 10f * s(t * 5.0), armL = 30f, armLb = 50f, face = "smile", lean = 3f, prop = "spoon")
            "write" -> sit.copy(lean = 10f, headTilt = 8f, armR = 55f, armRb = 70f + 6f * s(t * 4.0), armL = 45f, armLb = 60f, face = "think", prop = "pen")
            "type" -> sit.copy(lean = 8f, armR = 60f + 4f * s(t * 9.0), armRb = 62f, armL = 60f - 4f * s(t * 9.0), armLb = 62f, face = "smile", prop = "")
            "read" -> sit.copy(lean = 4f, headTilt = 6f, armR = 62f, armRb = 82f, armL = 62f, armLb = 82f, face = "smile", prop = "book")
            "phone" -> sit.copy(lean = 6f, headTilt = 9f, armR = 70f, armRb = 105f, armL = 20f, armLb = 30f, face = "laugh", prop = "phone", bob = s(t * 2.0) * 1f)
            "watch" -> sit.copy(headTilt = -4f, armR = 20f, armRb = 30f, armL = 20f, armLb = 30f, face = if (s(t * 0.7.toDouble()) > .6f) "surprise" else "smile")
            "sitBench" -> sit.copy(armL = 18f, armLb = 20f, armR = 18f, armRb = 20f, headTilt = s(t * 0.9.toDouble()) * 6f, face = "smile")
            "sitBean" -> sit.copy(lift = 48f, lean = -6f, legL = 70f, legLb = -50f, legR = 64f, legRb = -40f, headTilt = 6f, face = "smile", armL = 25f, armLb = 40f, armR = 25f, armRb = 40f)
            "exercise" -> { val c = s(t * 6.0); Pose(bob = abs(c) * 10f, armL = 100f + 70f * (c + 1) / 2f, armLb = 4f, armR = 100f + 70f * (c + 1) / 2f, armRb = 4f, legL = 6f + 14f * (c + 1) / 2f, legR = -6f - 14f * (c + 1) / 2f, face = "happy") }
            "dance" -> { val c = s(t * 5.0); Pose(bob = abs(c) * 6f, lean = c * 7f, headTilt = c * 8f, armL = 120f + 40f * c, armLb = 30f, armR = 120f - 40f * c, armRb = 30f, legL = 12f * c, legLb = -10f, legR = -12f * c, legRb = -10f, face = "laugh") }
            "wave" -> Pose(armR = 150f + 18f * s(t * 8.0), armRb = 24f, armL = 8f, face = "happy", headTilt = 4f)
            "laugh" -> Pose(bob = abs(s(t * 9.0)) * 4f, headTilt = 6f * s(t * 9.0), armL = 40f, armLb = 70f, armR = 40f, armRb = 70f, face = "laugh", eyesClosed = true)
            "cry" -> Pose(headTilt = 8f, armL = 85f, armLb = 120f, armR = 85f, armRb = 120f, face = "sad", bob = abs(s(t * 7.0)) * 1.5f)
            "jump" -> { val c = maxOf(0f, s(t * 5.0)); Pose(bob = c * 40f, armL = 150f, armR = 150f, legL = 10f, legLb = -30f, legR = -10f, legRb = -30f, face = "happy") }
            "blush" -> Pose(armL = 80f, armLb = 130f, armR = 80f, armRb = 130f, headTilt = 6f, face = "smile")
            else -> Pose(bob = s(t * 2.0) * 1.5f, armL = 6f + 2f * s(t * 1.7.toDouble()), armR = 6f - 2f * s(t * 1.7.toDouble()))
        }
    }

    val reactions = listOf("wave", "laugh", "jump", "blush", "dance")
    val all = listOf("sleep", "stretch", "brush", "dress", "mirror", "think", "eat", "cook", "write", "type", "read", "phone", "watch", "sitBench", "sitBean", "exercise", "dance", "wave", "laugh", "cry", "jump", "blush", "idle")
}
