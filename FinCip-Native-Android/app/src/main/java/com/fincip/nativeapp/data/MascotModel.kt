package com.fincip.nativeapp.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlin.random.Random

/** One of the four official characters. Their default look is the starting point; every layer can be changed afterwards. */
data class MascotCharacter(
    val id: Int, val name: String, val blurb: String, val female: Boolean, val hair: String, val hairColor: Int,
    val accessories: List<String>, val expression: String, val outfit: String, val outfitColor: Int, val bg: String
)

data class Opt(val id: String, val title: String, val premium: Boolean = false)

/** Everything that can be chosen. The drawing code reads these ids, so adding an option here and a branch there is all it takes. */
object MascotOptions {
    val characters = listOf(
        MascotCharacter(0, "Mira", "Cute and cheerful, long dark-purple hair.", true, "long", 0, listOf("clipArrow"), "wink", "hoodie", 1, "lavender"),
        MascotCharacter(1, "Kai", "Cool and confident, dark hair, violet aesthetic.", false, "messy", 1, emptyList(), "calm", "hoodie", 2, "dusk"),
        MascotCharacter(2, "Nova", "Gamer energy, headphones, expressive eyes.", true, "long", 0, listOf("headphones"), "smile", "hoodie", 1, "night"),
        MascotCharacter(3, "Ren", "Friendly and clever, glasses, soft personality.", false, "messy", 1, listOf("glasses"), "smile", "hoodie", 3, "lavender")
    )
    fun character(id: Int) = characters.getOrElse(id) { characters.first() }

    val outfits = listOf(
        Opt("hoodie", "Hoodie"), Opt("sweater", "Oversized sweater"), Opt("suit", "Formal suit"), Opt("casual", "Casual tee"),
        Opt("jacket", "Jacket"), Opt("anime", "Anime uniform"), Opt("winter", "Winter coat"),
        Opt("royal", "Royal coat", premium = true), Opt("cape", "Star cape", premium = true)
    )
    val hairStyles = listOf(Opt("long", "Long"), Opt("bob", "Bob"), Opt("twin", "Twin tails"), Opt("pony", "Ponytail"), Opt("bun", "Bun"),
        Opt("messy", "Messy"), Opt("side", "Side swept"), Opt("spiky", "Spiky"), Opt("curly", "Curly"))
    val expressions = listOf(Opt("happy", "Happy"), Opt("wink", "Wink"), Opt("calm", "Calm"), Opt("smile", "Soft smile"), Opt("joy", "Joy"), Opt("worried", "Worried"))
    val accessories = listOf(
        Opt("glasses", "Glasses"), Opt("headphones", "Headphones"), Opt("bow", "Bow"), Opt("starClip", "Star clip"), Opt("clipArrow", "Growth clip"),
        Opt("beanie", "Beanie"), Opt("cap", "Cap"), Opt("catEars", "Cat ears"), Opt("coin", "Rupee coin"), Opt("chart", "Chart pin")
    )
    val backgrounds = listOf(Opt("lavender", "Lavender"), Opt("dusk", "Dusk"), Opt("night", "Night"), Opt("ocean", "Ocean"), Opt("mint", "Mint"), Opt("rose", "Rose"))

    /** Outfit / colour palette, ARGB. Last slot is the custom colour. */
    val colorNames = listOf("Purple", "Lavender", "Black", "White", "Pink", "Blue")
    val colors = listOf(0xFF7C5CFFL, 0xFFB9A7FFL, 0xFF1E1B2EL, 0xFFF5F3FFL, 0xFFFF8FC0L, 0xFF5B8DEFL)
    val hairColorNames = listOf("Deep purple", "Black", "Brown", "Lavender", "Pink", "Blue", "Blonde", "Silver")
    val hairColors = listOf(0xFF2E1B4DL, 0xFF1A1420L, 0xFF5A3A2EL, 0xFFB9A7FFL, 0xFFFF9EC7L, 0xFF4D6BFFL, 0xFFEBC66BL, 0xFFD8D6E6L)

    /** Premium outfits open up as the pet levels up, so they are earned by using the app, not bought. */
    const val PREMIUM_LEVEL = 5
}

data class MascotLook(
    val character: Int = 0, val outfit: String = "hoodie", val outfitColor: Int = 1, val customColor: Long = 0xFF7C5CFFL,
    val hair: String = "long", val hairColor: Int = 0, val accessories: Set<String> = emptySet(), val bg: String = "lavender",
    val expression: String = "happy", val useAsLogo: Boolean = false
) {
    /** ARGB of the outfit; index -1 means the custom colour. */
    val outfitArgb: Long get() = if (outfitColor in MascotOptions.colors.indices) MascotOptions.colors[outfitColor] else customColor
    val hairArgb: Long get() = MascotOptions.hairColors[hairColor.coerceIn(0, MascotOptions.hairColors.lastIndex)]

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("character", character); addProperty("outfit", outfit); addProperty("outfitColor", outfitColor); addProperty("customColor", customColor)
        addProperty("hair", hair); addProperty("hairColor", hairColor); add("accessories", JsonArray().also { a -> accessories.sorted().forEach { a.add(it) } })
        addProperty("bg", bg); addProperty("expression", expression); addProperty("useAsLogo", useAsLogo)
    }

    /** Clamp everything to something drawable, so old or hand-edited saves never crash the renderer. */
    fun sanitized(): MascotLook = copy(
        character = character.coerceIn(0, MascotOptions.characters.lastIndex),
        outfit = if (MascotOptions.outfits.any { it.id == outfit }) outfit else "hoodie",
        outfitColor = if (outfitColor == -1 || outfitColor in MascotOptions.colors.indices) outfitColor else 1,
        hair = if (MascotOptions.hairStyles.any { it.id == hair }) hair else "long",
        hairColor = hairColor.coerceIn(0, MascotOptions.hairColors.lastIndex),
        accessories = accessories.filter { a -> MascotOptions.accessories.any { it.id == a } }.toSet(),
        bg = if (MascotOptions.backgrounds.any { it.id == bg }) bg else "lavender",
        expression = if (MascotOptions.expressions.any { it.id == expression }) expression else "happy"
    )

    /** A premium outfit the user has not unlocked yet falls back to the hoodie. */
    fun forLevel(level: Int): MascotLook =
        if (MascotOptions.outfits.firstOrNull { it.id == outfit }?.premium == true && level < MascotOptions.PREMIUM_LEVEL) copy(outfit = "hoodie") else this

    companion object {
        fun defaultFor(c: Int): MascotLook {
            val ch = MascotOptions.character(c)
            return MascotLook(ch.id, ch.outfit, ch.outfitColor, 0xFF7C5CFFL, ch.hair, ch.hairColor, ch.accessories.toSet(), ch.bg, ch.expression)
        }

        fun fromJson(o: JsonObject?): MascotLook {
            if (o == null || !o.has("character")) return MascotLook()
            fun s(k: String, d: String) = if (o.has(k) && o.get(k).isJsonPrimitive) o.get(k).asString else d
            fun n(k: String, d: Long) = if (o.has(k) && o.get(k).isJsonPrimitive) runCatching { o.get(k).asDouble.toLong() }.getOrDefault(d) else d
            val acc = if (o.has("accessories") && o.get("accessories").isJsonArray) o.getAsJsonArray("accessories").mapNotNull { runCatching { it.asString }.getOrNull() }.toSet() else emptySet()
            return MascotLook(n("character", 0).toInt(), s("outfit", "hoodie"), n("outfitColor", 1).toInt(), n("customColor", 0xFF7C5CFFL), s("hair", "long"), n("hairColor", 0).toInt(), acc,
                s("bg", "lavender"), s("expression", "happy"), o.has("useAsLogo") && runCatching { o.get("useAsLogo").asBoolean }.getOrDefault(false)).sanitized()
        }

        /** A fresh random outfit for the same character. Never picks a locked premium outfit. */
        fun random(base: MascotLook, rng: Random, level: Int): MascotLook {
            val outfits = MascotOptions.outfits.filter { !it.premium || level >= MascotOptions.PREMIUM_LEVEL }
            val accPool = MascotOptions.accessories.map { it.id }
            val picked = accPool.shuffled(rng).take(rng.nextInt(0, 3)).toSet()
            return base.copy(
                outfit = outfits.random(rng).id, outfitColor = rng.nextInt(MascotOptions.colors.size), hair = MascotOptions.hairStyles.random(rng).id,
                hairColor = rng.nextInt(MascotOptions.hairColors.size), accessories = picked, bg = MascotOptions.backgrounds.random(rng).id, expression = MascotOptions.expressions.random(rng).id
            ).sanitized()
        }
    }
}
