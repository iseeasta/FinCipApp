package com.fincip.nativeapp.world

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** A tappable object in a room. Rectangles are in the 1200 x 600 room space. [activity] is what she does there, "" = just a comment. */
data class Prop(val id: String, val label: String, val x: Float, val y: Float, val w: Float, val h: Float, val activity: String, val line: String) {
    fun hit(px: Float, py: Float) = px in x..(x + w) && py in y..(y + h)
}

object RoomArt {
    const val H = 600f

    fun props(room: String): List<Prop> = when (room) {
        "bedroom" -> listOf(
            Prop("bed", "Bed", 80f, 320f, 330f, 150f, "sleep", "Nap time. The bed is undefeated."),
            Prop("wardrobe", "Wardrobe", 440f, 140f, 170f, 330f, "dress", "Outfit check! Use the Outfit tab to pick one."),
            Prop("mirror", "Mirror", 650f, 150f, 120f, 250f, "mirror", "Looking cute. Budget-friendly too."),
            Prop("sink", "Vanity", 640f, 400f, 150f, 70f, "brush", "Clean teeth, clean books."),
            Prop("window", "Window", 880f, 80f, 160f, 220f, "window", "Let's see what the weather is doing."),
            Prop("poster", "Poster", 1080f, 120f, 90f, 120f, "", "Goal poster! What are you saving for?")
        )
        "living" -> listOf(
            Prop("sofa", "Sofa", 300f, 330f, 320f, 140f, "phone", "This sofa has seen many budget plans."),
            Prop("tv", "TV", 690f, 270f, 190f, 200f, "tv", "Movie time?"),
            Prop("books", "Bookshelf", 880f, 120f, 190f, 350f, "readLiving", "I should finish a book. Or at least start."),
            Prop("plant", "Plant", 1090f, 300f, 80f, 170f, "", "I water it. Mostly.")
        )
        "kitchen" -> listOf(
            Prop("stove", "Stove", 200f, 300f, 270f, 170f, "cook", "Cooking at home beats ordering in. Every time."),
            Prop("table", "Table", 520f, 340f, 260f, 130f, "breakfast", "Breakfast is served."),
            Prop("fridge", "Fridge", 880f, 120f, 150f, 350f, "snack", "Just checking what's inside. Again."),
            Prop("cabinet", "Cabinets", 200f, 90f, 360f, 130f, "", "All the cups. Zero spent today, I hope.")
        )
        "study" -> listOf(
            Prop("desk", "Desk", 380f, 330f, 340f, 140f, "study", "Let's focus."),
            Prop("pc", "Computer", 450f, 200f, 170f, 130f, "work", "Work mode on."),
            Prop("calendar", "Calendar", 760f, 120f, 100f, 130f, "", "Bills have due dates. Calendars remember."),
            Prop("books", "Bookshelf", 860f, 120f, 190f, 350f, "readStudy", "Knowledge is the best investment.")
        )
        "garden" -> listOf(
            Prop("bench", "Bench", 790f, 360f, 220f, 110f, "garden", "Perfect spot to sit."),
            Prop("tree", "Tree", 120f, 60f, 240f, 410f, "", "This tree has been compounding for years."),
            Prop("flowers", "Flowers", 410f, 400f, 260f, 70f, "", "Small things grow when you're consistent."),
            Prop("mat", "Mat", 460f, 450f, 200f, 30f, "exercise", "Workout time!")
        )
        else -> listOf(
            Prop("bean", "Beanbag", 400f, 360f, 250f, 110f, "relax", "Soft. Quiet. Perfect."),
            Prop("candle", "Candles", 700f, 340f, 90f, 130f, "", "Smells like a calm month."),
            Prop("speaker", "Speaker", 800f, 330f, 120f, 140f, "dance", "Music on! Let's dance."),
            Prop("lights", "Fairy lights", 120f, 70f, 960f, 70f, "", "Tiny lights, tiny joys.")
        )
    }

    private fun DrawScope.rr(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 8f) =
        drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r, r))

    fun skyColors(level: Float, weather: String): Pair<Color, Color> {
        val dayTop = when (weather) { Weather.RAIN -> Color(0xFF8FA3B5); Weather.CLOUDY -> Color(0xFFB3CFE3); Weather.SNOW -> Color(0xFFCAD6E3); else -> Color(0xFF8ED3F4) }
        val dayBot = when (weather) { Weather.RAIN -> Color(0xFFB7C5D0); Weather.CLOUDY -> Color(0xFFDDEBF4); Weather.SNOW -> Color(0xFFEDF2F8); else -> Color(0xFFFFE9F1) }
        val dusk = Color(0xFFFF9EB8); val nightTop = Color(0xFF111A44); val nightBot = Color(0xFF3A3A7E)
        val l = level.coerceIn(0f, 1f)
        val top = if (l < .5f) lerp(nightTop, dusk.copy(alpha = 1f), l * 2f).let { lerp(it, dayTop, 0f) } else lerp(lerp(nightTop, dusk, 1f), dayTop, (l - .5f) * 2f)
        val bot = if (l < .5f) lerp(nightBot, dusk, l * 2f) else lerp(dusk, dayBot, (l - .5f) * 2f)
        return top to bot
    }

    private fun DrawScope.sky(x: Float, y: Float, w: Float, h: Float, level: Float, weather: String, t: Float, hour: Double) {
        val (a, b) = skyColors(level, weather)
        drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(a, b), startY = y, endY = y + h), Offset(x, y), Size(w, h))
        if (level < .45f) for (i in 0 until 18) { val sx = x + ((i * 97) % 1000) / 1000f * w; val sy = y + ((i * 53) % 700) / 1000f * h * .9f; drawCircle(Color.White.copy(alpha = (.5f + .4f * sin(t * 2f + i)) * (1f - level * 2f)), 1.6f, Offset(sx, sy)) }
        // sun or moon travels across
        val p = (((hour - 6.0) / 13.0).coerceIn(0.0, 1.0)).toFloat()
        if (level > .3f && weather != Weather.RAIN) drawCircle(Color(0xFFFFE08A).copy(alpha = .9f), 22f, Offset(x + w * (.15f + .7f * p), y + h * (.7f - .5f * sin(p * PI.toFloat()))))
        else if (level < .35f) { val mp = ((hour + 6.0) % 24.0 / 12.0).coerceIn(0.0, 1.0).toFloat(); drawCircle(Color(0xFFFFF6D0), 18f, Offset(x + w * (.15f + .7f * mp), y + h * .28f)); drawCircle(a, 14f, Offset(x + w * (.15f + .7f * mp) + 8f, y + h * .28f - 4f)) }
        if (weather != Weather.CLEAR) for (i in 0 until 3) {
            val cx = x + ((t * 8f + i * w * .4f) % (w + 160f)) - 80f; val cy = y + h * (.2f + .2f * i)
            val cc = if (weather == Weather.RAIN) Color(0xFF7E8E9E) else Color.White.copy(alpha = .92f)
            drawCircle(cc, 22f, Offset(cx, cy)); drawCircle(cc, 30f, Offset(cx + 26f, cy - 8f)); drawCircle(cc, 22f, Offset(cx + 56f, cy)); drawRect(cc, Offset(cx, cy), Size(56f, 22f))
        }
    }

    private fun DrawScope.precip(x: Float, y: Float, w: Float, h: Float, weather: String, t: Float, count: Int) {
        if (weather != Weather.RAIN && weather != Weather.SNOW) return
        for (i in 0 until count) {
            val px = x + ((i * 137) % 1000) / 1000f * w
            if (weather == Weather.RAIN) {
                val py = y + ((t * 520f + i * 71f) % h)
                drawLine(Color(0xFFBFE3FF).copy(alpha = .75f), Offset(px, py), Offset(px - 4f, py + 14f), strokeWidth = 1.6f)
            } else {
                val py = y + ((t * 60f + i * 43f) % h)
                drawCircle(Color.White, 2.4f, Offset(px + 8f * sin(t + i), py))
            }
        }
    }

    private fun DrawScope.window(x: Float, y: Float, w: Float, h: Float, level: Float, weather: String, t: Float, hour: Double) {
        rr(Color(0xFFFFFFFF), x - 10f, y - 10f, w + 20f, h + 20f, 12f)
        sky(x, y, w, h, level, weather, t, hour)
        precip(x, y, w, h, weather, t, 12)
        drawRect(Color.White, Offset(x + w / 2 - 4f, y), Size(8f, h)); drawRect(Color.White, Offset(x, y + h / 2 - 4f), Size(w, 8f))
        rr(Color(0xFFF3D6DF), x - 16f, y + h + 8f, w + 32f, 12f, 6f)
        drawPath(Path().apply { moveTo(x - 6f, y); lineTo(x + w * .2f, y); lineTo(x + w * .08f, y + h); lineTo(x - 6f, y + h); close() }, Color(0xFFF9C6D8).copy(alpha = .9f))
        drawPath(Path().apply { moveTo(x + w + 6f, y); lineTo(x + w * .8f, y); lineTo(x + w * .92f, y + h); lineTo(x + w + 6f, y + h); close() }, Color(0xFFF9C6D8).copy(alpha = .9f))
    }

    private fun DrawScope.plant(x: Float, base: Float, s: Float = 1f, t: Float) {
        rr(Color(0xFFE59A7A), x - 26f * s, base - 54f * s, 52f * s, 54f * s, 8f)
        for (i in -2..2) { val sw = sin(t * 1.2f + i) * 4f; drawOval(Color(0xFF5DBB7E), Offset(x + i * 20f * s - 12f * s + sw, base - 150f * s + abs(i) * 18f * s), Size(26f * s, 100f * s)) }
    }

    private fun DrawScope.shelf(x: Float, y: Float, w: Float, h: Float, rows: Int, seed: Int) {
        rr(Color(0xFFC49A6C), x, y, w, h, 6f); rr(Color(0xFFF1DCC0), x + 8f, y + 8f, w - 16f, h - 16f, 4f)
        val pal = listOf(0xFFF08FB1, 0xFF7FB7E6, 0xFFF3C969, 0xFF8FD3A8, 0xFFB79CE8, 0xFFEE9E7A)
        val rh = (h - 16f) / rows
        for (r in 0 until rows) {
            val by = y + 8f + (r + 1) * rh
            var bx = x + 14f; var k = 0
            while (bx < x + w - 30f) { val bw = 12f + ((seed + r * 7 + k * 5) % 4) * 4f; val bh = rh * (.55f + ((seed + k * 3 + r) % 4) * .1f); drawRect(Color(pal[(seed + r + k) % pal.size].toInt()), Offset(bx, by - bh - 4f), Size(bw, bh)); bx += bw + 2f; k++ }
            drawRect(Color(0xFFC49A6C), Offset(x + 8f, by - 4f), Size(w - 16f, 6f))
        }
    }

    private fun DrawScope.lamp(x: Float, base: Float, on: Boolean) {
        drawRect(Color(0xFF8E7A8E), Offset(x - 3f, base - 150f), Size(6f, 150f)); rr(Color(0xFF8E7A8E), x - 24f, base - 8f, 48f, 8f, 4f)
        drawPath(Path().apply { moveTo(x - 24f, base - 120f); lineTo(x + 24f, base - 120f); lineTo(x + 36f, base - 160f); lineTo(x - 36f, base - 160f); close() }, if (on) Color(0xFFFFE6A1) else Color(0xFFF3D6DF))
    }

    /** Lamp positions per room, used for the night glow. */
    private fun lamps(room: String): List<Offset> = when (room) {
        "bedroom" -> listOf(Offset(1120f, 320f), Offset(440f, 130f)); "living" -> listOf(Offset(240f, 320f), Offset(1010f, 80f))
        "kitchen" -> listOf(Offset(600f, 80f), Offset(380f, 80f)); "study" -> listOf(Offset(560f, 180f), Offset(780f, 80f))
        "relax" -> listOf(Offset(710f, 350f), Offset(300f, 100f), Offset(900f, 100f)); else -> emptyList()
    }

    fun DrawScope.drawRoom(room: String, hour: Double, weather: String, t: Float) {
        val level = Daylight.level(hour).toFloat()
        val night = (1f - level).coerceIn(0f, 1f)
        val F = Rooms.FLOOR
        if (room == "garden") { garden(level, weather, t, hour); return }
        val wall = when (room) { "bedroom" -> Color(0xFFFFE3EC); "living" -> Color(0xFFFCEBD9); "kitchen" -> Color(0xFFFFF3D6); "study" -> Color(0xFFE6EEFB); else -> Color(0xFFEBE1FA) }
        val floorC = when (room) { "kitchen" -> Color(0xFFF2D8C2); "relax" -> Color(0xFFD9C8F0); else -> Color(0xFFE8C9A6) }
        drawRect(wall, Offset(0f, 0f), Size(Rooms.W, F))
        for (i in 0 until 12) drawRect(Color.White.copy(alpha = .22f), Offset(i * 100f + 40f, 0f), Size(34f, F))
        drawRect(Color(0xFFFFFFFF), Offset(0f, F - 18f), Size(Rooms.W, 18f))
        drawRect(floorC, Offset(0f, F), Size(Rooms.W, H - F))
        for (i in 0 until 14) drawLine(Color.Black.copy(alpha = .05f), Offset(i * 90f - 10f, F), Offset(i * 130f - 100f, H), strokeWidth = 2f)
        drawLine(Color.Black.copy(alpha = .06f), Offset(0f, F + 60f), Offset(Rooms.W, F + 60f), strokeWidth = 2f)
        // door the way she comes in
        rr(Color(0xFFF3D6DF), 20f, 150f, 110f, F - 150f, 10f); rr(Color(0xFFE9A3BD), 34f, 164f, 82f, F - 180f, 6f); drawCircle(Color(0xFFF8E3A0), 6f, Offset(102f, 320f))

        when (room) {
            "bedroom" -> {
                window(880f, 80f, 160f, 220f, level, weather, t, hour)
                rr(Color(0xFFD9A57E), 100f, 410f, 300f, 60f, 6f); rr(Color(0xFFB98263), 90f, 300f, 22f, 170f, 6f)
                rr(Color.White, 112f, 360f, 280f, 62f, 18f); rr(Color(0xFFF7A8C4), 112f, 372f, 280f, 50f, 14f)
                rr(Color.White, 122f, 340f, 90f, 36f, 16f); rr(Color(0xFFFFF0F6), 360f, 350f, 40f, 50f, 8f)
                rr(Color(0xFFF2C6D6), 440f, 140f, 170f, 330f, 10f); drawLine(Color(0xFFD9A3B7), Offset(525f, 150f), Offset(525f, 460f), strokeWidth = 3f)
                drawCircle(Color(0xFFF8E3A0), 6f, Offset(512f, 320f)); drawCircle(Color(0xFFF8E3A0), 6f, Offset(538f, 320f))
                rr(Color(0xFFD9A57E), 640f, 400f, 150f, 70f, 6f); rr(Color.White, 690f, 410f, 50f, 22f, 10f); drawRect(Color(0xFF7FD3E0), Offset(760f, 392f), Size(8f, 16f))
                drawOval(Color(0xFFCDEFFF), Offset(655f, 150f), Size(110f, 250f)); drawOval(Color.White.copy(alpha = .8f), Offset(655f, 150f), Size(110f, 250f), style = Stroke(8f))
                drawLine(Color.White.copy(alpha = .7f), Offset(680f, 190f), Offset(700f, 170f), strokeWidth = 4f, cap = StrokeCap.Round)
                rr(Color.White, 1080f, 120f, 90f, 120f, 6f); heartAt(1125f, 175f, 22f, Color(0xFFF59AB8))
                rr(Color(0xFFFFD9E6), 480f, 478f, 360f, 70f, 40f)
                lamp(1120f, F, night > .35f)
                plant(1010f, F, .8f, t)
            }
            "living" -> {
                window(130f, 90f, 150f, 190f, level, weather, t, hour)
                rr(Color(0xFFF3A9B4), 300f, 380f, 320f, 90f, 24f); rr(Color(0xFFF7BDC6), 300f, 330f, 70f, 140f, 24f); rr(Color(0xFFF7BDC6), 550f, 330f, 70f, 140f, 24f); rr(Color(0xFFFFD9E3), 380f, 380f, 160f, 40f, 14f)
                rr(Color(0xFF2E2A3F), 690f, 270f, 190f, 110f, 8f)
                val on = true; rr(if (on) Color(0xFF5C5A8E).copy(alpha = .75f + .25f * sin(t * 3f)) else Color.Black, 698f, 278f, 174f, 94f, 4f)
                drawCircle(Color(0xFFFFD0E0).copy(alpha = .7f), 18f, Offset(785f + 30f * sin(t), 325f + 8f * cos(t * 1.6f)))
                rr(Color(0xFFC49A6C), 700f, 400f, 170f, 70f, 6f)
                shelf(880f, 120f, 190f, 350f, 4, 3)
                plant(1130f, F, .95f, t)
                lamp(240f, F, night > .35f)
                rr(Color(0xFFFFE0A8), 330f, 478f, 400f, 70f, 40f)
                rr(Color.White, 520f, 130f, 120f, 90f, 6f); drawRect(Color(0xFFCDEBDA), Offset(530f, 140f), Size(100f, 70f)); drawCircle(Color(0xFFF59AB8), 16f, Offset(580f, 175f))
            }
            "kitchen" -> {
                rr(Color(0xFFFFFFFF), 200f, 90f, 360f, 130f, 8f); for (i in 0 until 3) { rr(Color(0xFFFFD9B0), 208f + i * 118f, 98f, 112f, 114f, 6f); drawCircle(Color(0xFFB9855D), 5f, Offset(300f + i * 118f, 160f)) }
                rr(Color(0xFFFFFFFF), 200f, 340f, 270f, 130f, 6f); rr(Color(0xFFE9D2BC), 200f, 330f, 270f, 18f, 4f)
                rr(Color(0xFF6F6A80), 230f, 312f, 120f, 20f, 4f); for (i in 0 until 2) drawCircle(Color(0xFF4A4658), 14f, Offset(262f + i * 56f, 322f))
                rr(Color(0xFFC9C3D6), 350f, 282f, 60f, 42f, 8f)
                if (true) for (i in 0 until 3) drawCircle(Color.White.copy(alpha = .5f * (1f - ((t * .6f + i * .3f) % 1f))), 8f, Offset(380f + 6f * sin(t + i), 275f - ((t * 30f + i * 40f) % 90f)))
                window(620f, 100f, 170f, 120f, level, weather, t, hour)
                rr(Color(0xFFF2B6C6), 520f, 380f, 260f, 22f, 6f); drawRect(Color(0xFFB9855D), Offset(545f, 402f), Size(12f, 68f)); drawRect(Color(0xFFB9855D), Offset(745f, 402f), Size(12f, 68f))
                rr(Color.White, 580f, 360f, 70f, 20f, 8f); drawCircle(Color(0xFFF59AB8), 11f, Offset(700f, 368f)); drawOval(Color(0xFFFFE08A), Offset(560f, 364f), Size(24f, 14f))
                rr(Color(0xFFDDEBF5), 880f, 120f, 150f, 350f, 14f); drawLine(Color(0xFFB4C8D8), Offset(880f, 270f), Offset(1030f, 270f), strokeWidth = 5f); drawRect(Color(0xFFB4C8D8), Offset(1005f, 180f), Size(8f, 60f)); drawRect(Color(0xFFB4C8D8), Offset(1005f, 300f), Size(8f, 80f))
                rr(Color(0xFFFFB9CC), 905f, 150f, 30f, 26f, 4f)
                rr(Color(0xFFFFE3A0), 1090f, 380f, 70f, 90f, 10f); rr(Color(0xFFFFF4D2), 1104f, 340f, 42f, 46f, 10f)
                plant(1160f, 300f, .5f, t)
            }
            "study" -> {
                window(130f, 90f, 170f, 180f, level, weather, t, hour)
                rr(Color(0xFFD9A57E), 380f, 330f, 340f, 24f, 6f); drawRect(Color(0xFFB98263), Offset(400f, 354f), Size(14f, 116f)); drawRect(Color(0xFFB98263), Offset(690f, 354f), Size(14f, 116f))
                rr(Color(0xFF2E2A3F), 450f, 200f, 170f, 110f, 8f); rr(Color(0xFFBFE3FF), 458f, 208f, 154f, 94f, 4f); drawRect(Color(0xFF2E2A3F), Offset(525f, 310f), Size(20f, 20f))
                for (i in 0 until 5) drawRect(Color(0xFF6B5B9A).copy(alpha = .6f), Offset(468f, 220f + i * 16f), Size(40f + (i * 23 % 70), 5f))
                rr(Color.White, 640f, 306f, 60f, 24f, 4f); rr(Color(0xFFF7A8C4), 560f, 316f, 50f, 14f, 4f)
                drawRect(Color(0xFFB7C7E6), Offset(660f, 290f), Size(12f, 16f)); drawRect(Color(0xFFF3C969), Offset(676f, 296f), Size(10f, 10f))
                rr(Color.White, 760f, 120f, 100f, 130f, 6f); rr(Color(0xFFF08FB1), 760f, 120f, 100f, 26f, 6f); for (i in 0 until 12) drawCircle(Color(0xFFB9C4D8), 4f, Offset(776f + (i % 4) * 22f, 168f + (i / 4) * 24f)); drawCircle(Color(0xFFF08FB1), 9f, Offset(820f, 192f))
                shelf(860f, 120f, 190f, 350f, 5, 9)
                rr(Color(0xFFF4B6CC), 500f, 380f, 80f, 90f, 18f); rr(Color(0xFFFFFFFF), 490f, 366f, 100f, 24f, 12f)
                lamp(1120f, F, night > .35f); plant(330f, F, .7f, t)
                rr(Color(0xFFDCD0F2), 420f, 480f, 330f, 60f, 36f)
            }
            else -> {
                for (i in 0..14) { val lx = 120f + i * 70f; val ly = 90f + 18f * sin(i * .9f); drawLine(Color(0xFFD9C3E8), Offset(lx, ly), Offset(lx + 70f, 90f + 18f * sin((i + 1) * .9f)), strokeWidth = 2f); drawCircle(Color(0xFFFFE9A0).copy(alpha = .7f + .3f * sin(t * 2f + i)), 6f, Offset(lx, ly + 8f)) }
                window(130f, 160f, 150f, 170f, level, weather, t, hour)
                rr(Color(0xFFE79BC6), 400f, 380f, 250f, 90f, 44f); rr(Color(0xFFF2BAD8), 430f, 360f, 190f, 60f, 30f)
                rr(Color(0xFFFFFFFF), 700f, 420f, 50f, 50f, 6f); for (i in 0 until 3) { rr(Color(0xFFF59AB8), 708f + i * 14f, 392f - i * 12f, 10f, 40f + i * 12f, 3f) }
                rr(Color(0xFF3F3A58), 800f, 330f, 120f, 140f, 10f); drawCircle(Color(0xFF6B6690), 34f + 3f * sin(t * 8f), Offset(860f, 410f)); drawCircle(Color(0xFF6B6690), 16f, Offset(860f, 360f))
                plant(1050f, F, 1.1f, t); plant(300f, F, .8f, t)
                rr(Color(0xFFC9B4EE), 420f, 480f, 520f, 64f, 36f)
            }
        }
    }

    /** Night darkening and lamp glow, drawn after the girl so she sits in the same light as the room. */
    fun DrawScope.drawRoomLight(room: String, hour: Double, weather: String) {
        val night = (1f - Daylight.level(hour).toFloat()).coerceIn(0f, 1f)
        if (night > .01f) {
            drawRect(Color(0xFF16134A).copy(alpha = (if (room == "garden") .35f else .38f) * night), Offset(0f, 0f), Size(Rooms.W, H))
            if (room != "garden") for (l in lamps(room)) drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFFFFE2A0).copy(alpha = .5f * night), Color.Transparent), center = l, radius = 230f), 230f, l)
        } else if (weather == Weather.RAIN || weather == Weather.CLOUDY) drawRect(Color(0xFF3A4A66).copy(alpha = .09f), Offset(0f, 0f), Size(Rooms.W, H))
    }

    private fun DrawScope.heartAt(x: Float, y: Float, r: Float, c: Color) {
        drawCircle(c, r * .55f, Offset(x - r * .5f, y - r * .2f)); drawCircle(c, r * .55f, Offset(x + r * .5f, y - r * .2f))
        drawPath(Path().apply { moveTo(x - r * 1.02f, y - r * .05f); lineTo(x, y + r * 1.05f); lineTo(x + r * 1.02f, y - r * .05f); close() }, c)
    }

    private fun DrawScope.garden(level: Float, weather: String, t: Float, hour: Double) {
        val F = Rooms.FLOOR
        sky(0f, 0f, Rooms.W, F, level, weather, t, hour)
        drawOval(Color(0xFF9FD8A5), Offset(-200f, 330f), Size(900f, 260f)); drawOval(Color(0xFFB4E1B0), Offset(500f, 350f), Size(900f, 220f))
        drawRect(Color(0xFF8FD39B), Offset(0f, F), Size(Rooms.W, H - F))
        for (i in 0 until 40) { val gx = (i * 61f) % Rooms.W; drawLine(Color(0xFF6DBB7C), Offset(gx, F + 10f + (i * 17f) % 110f), Offset(gx + 3f * sin(t + i), F - 8f + (i * 17f) % 110f), strokeWidth = 2f) }
        for (i in 0 until 24) { drawRect(Color.White, Offset(i * 52f, F - 50f), Size(10f, 50f)); drawPath(Path().apply { moveTo(i * 52f, F - 50f); lineTo(i * 52f + 5f, F - 62f); lineTo(i * 52f + 10f, F - 50f); close() }, Color.White) }
        drawRect(Color.White, Offset(0f, F - 38f), Size(Rooms.W, 7f))
        // tree
        drawRect(Color(0xFFA9764F), Offset(224f, 230f), Size(36f, 240f))
        for ((dx, dy, r) in listOf(Triple(-60f, 0f, 90f), Triple(70f, -10f, 100f), Triple(0f, -80f, 90f), Triple(-100f, 70f, 60f), Triple(110f, 70f, 64f))) drawCircle(Color(0xFF63C07F), r, Offset(242f + dx + 3f * sin(t * .8f + dx), 170f + dy))
        for (i in 0 until 7) drawCircle(Color(0xFFFFC0D4), 7f, Offset(242f + (i * 37f % 180f) - 90f, 120f + (i * 53f % 120f)))
        // flowers
        for (i in 0 until 12) { val fx = 420f + i * 22f; val fy = F + 22f + (i % 3) * 8f; drawLine(Color(0xFF4FA866), Offset(fx, fy), Offset(fx + 2f * sin(t + i), fy - 38f), strokeWidth = 3f); val c = listOf(0xFFF59AB8, 0xFFFFE08A, 0xFFFFFFFF, 0xFFB79CE8)[i % 4].toInt(); for (k in 0 until 5) { val a = k * 2f * PI.toFloat() / 5f; drawCircle(Color(c), 6f, Offset(fx + 2f * sin(t + i) + 8f * cos(a), fy - 38f + 8f * sin(a))) }; drawCircle(Color(0xFFFFC94D), 4f, Offset(fx + 2f * sin(t + i), fy - 38f)) }
        // bench
        rr(Color(0xFFB98263), 790f, 400f, 220f, 18f, 6f); rr(Color(0xFFB98263), 790f, 340f, 220f, 14f, 6f); drawRect(Color(0xFF9B6B4E), Offset(800f, 418f), Size(12f, 52f)); drawRect(Color(0xFF9B6B4E), Offset(988f, 418f), Size(12f, 52f)); drawRect(Color(0xFF9B6B4E), Offset(800f, 354f), Size(10f, 46f)); drawRect(Color(0xFF9B6B4E), Offset(990f, 354f), Size(10f, 46f))
        // exercise mat
        rr(Color(0xFFB79CE8), 460f, 452f, 200f, 18f, 8f)
        if (level > .35f && weather != Weather.RAIN) { for (i in 0 until 4) { val bx = 560f + 160f * sin(t * .3f + i * 1.7f) + i * 120f; val by = 250f + 40f * sin(t * 1.4f + i * 2f); val fl = sin(t * 14f + i) * 5f
            drawOval(Color(0xFFFFB86B), Offset(bx - 9f, by - 5f - fl), Size(10f, 12f)); drawOval(Color(0xFFFFB86B), Offset(bx + 1f, by - 5f - fl), Size(10f, 12f)) } }
        else if (level < .3f) { for (i in 0 until 9) drawCircle(Color(0xFFFFF4A8).copy(alpha = .4f + .6f * abs(sin(t * 1.3f + i))), 3.4f, Offset(300f + (i * 97f) % 700f + 10f * sin(t + i), 280f + (i * 41f) % 160f + 8f * cos(t * .8f + i))) }
        precip(0f, 0f, Rooms.W, H, weather, t, 70)
    }
}
