package com.fincip.nativeapp.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fincip.nativeapp.R
import androidx.compose.ui.unit.sp


/**
 * FinCip "Leaf" design tokens: warm cream, deep forest green, soft pastel tiles. Legacy colour names are
 * kept so every screen re-themes at once: Teal = positive green, Coral = restrained red, Sun = amber,
 * Lavender/Peach/Mint = soft pastel blocks.
 */
object Pop {
    val Cream = Color(0xFFFBF7EE)
    val Ink = Color(0xFF16231B)
    val Forest = Color(0xFF1F5C3A)
    val Lime = Color(0xFFB9F227)
    val Coral = Color(0xFFD9706A)
    val Teal = Color(0xFF3C9460)
    val Sun = Color(0xFFF0C14B)
    val Lavender = Color(0xFFD9D0F5)
    val Peach = Color(0xFFFAD9BC)
    val Mint = Color(0xFFDDEFD6)
    val Indigo = Color(0xFF0E100E)
    val IndigoHi = Color(0xFF1B1D1B)
    val Gain = Color(0xFF3C9460)
    val Loss = Color(0xFFD9706A)
    val Radius = 22.dp
    val RadiusSmall = 14.dp
}

/** Bundled Plus Jakarta Sans: friendly geometric sans used across the whole app. */
val PopFont = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_bold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
    Font(R.font.jakarta_extrabold, FontWeight.Black)
)

/** Readable text colour for any block colour, in light and dark mode. */
fun contentOn(bg: Color): Color = if (bg.luminance() > 0.42f) Pop.Ink else Pop.Cream

// ---------------------------------------------------------------------------------------------
// Theme engine: a "vibe" bundles a card style (shape, border, shadow, fill), a font and default colours.
// An accent colour can be layered on top of any vibe, and every scheme exists in light and dark.
// ---------------------------------------------------------------------------------------------

enum class ShadowKind { NONE, SOFT, HARD }

/** How cards, buttons and chips are built. Changing the vibe swaps this, so the whole UI changes character. */
data class CardStyle(
    val radius: Dp = 22.dp,
    val border: Dp = 1.dp,
    val strongBorder: Boolean = false,
    val primaryBorder: Boolean = false,
    val shadow: ShadowKind = ShadowKind.SOFT,
    val hardOffset: Dp = 4.dp,
    val tintedShadow: Boolean = false,
    val glass: Boolean = false,
    val gradient: Boolean = false,
    val pill: Boolean = true
)

val LocalCardStyle = staticCompositionLocalOf { CardStyle() }

internal enum class Face { POP, MONO, SERIF }

class VibeSpec internal constructor(
    val name: String,
    val blurb: String,
    val seed: Color,
    val style: CardStyle,
    internal val face: Face = Face.POP,
    internal val tintHue: Float? = null,
    internal val tintSat: Float = 0.35f,
    internal val pureSurface: Boolean = false,
    internal val bgDarkL: Float = 0.07f,
    internal val handcrafted: ((Boolean) -> ColorScheme)? = null
)

private fun brutalLight() = lightColorScheme(
    primary = Pop.Ink, onPrimary = Pop.Cream, secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
    background = Color(0xFFF6F5EF), onBackground = Pop.Ink, surface = Color.White, onSurface = Pop.Ink,
    surfaceVariant = Color(0xFFEAECE3), onSurfaceVariant = Color(0xFF3B3B36), secondaryContainer = Color(0xFFE3F7A6), onSecondaryContainer = Pop.Ink,
    primaryContainer = Pop.Lime, onPrimaryContainer = Pop.Ink, outline = Pop.Ink, outlineVariant = Color(0xFFCBCDC0)
)

private fun brutalDark() = darkColorScheme(
    primary = Pop.Lime, onPrimary = Pop.Ink, secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
    background = Color(0xFF121212), onBackground = Color(0xFFF6F5EF), surface = Color(0xFF1F1F1D), onSurface = Color(0xFFF6F5EF),
    surfaceVariant = Color(0xFF2B2B28), onSurfaceVariant = Color(0xFFD8D8CE), secondaryContainer = Color(0xFF34402A), onSecondaryContainer = Pop.Cream,
    primaryContainer = Color(0xFF3E5210), onPrimaryContainer = Pop.Cream, outline = Color(0xFF8C8C82), outlineVariant = Color(0xFF3A3A36)
)

private fun popLight() = lightColorScheme(
    primary = Pop.Forest, onPrimary = Color.White, secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
    background = Pop.Cream, onBackground = Pop.Ink, surface = Color(0xFFFFFDF8), onSurface = Pop.Ink,
    surfaceVariant = Color(0xFFF1EDDF), onSurfaceVariant = Color(0xFF45524A), secondaryContainer = Pop.Mint, onSecondaryContainer = Pop.Ink,
    primaryContainer = Pop.Mint, onPrimaryContainer = Pop.Forest, outline = Color(0xFFC9CDBE), outlineVariant = Color(0xFFE8E6D8)
)

private fun popDark() = darkColorScheme(
    primary = Color(0xFFA8D98A), onPrimary = Pop.Ink, secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
    background = Pop.Indigo, onBackground = Pop.Cream, surface = Pop.IndigoHi, onSurface = Pop.Cream,
    surfaceVariant = Color(0xFF22352A), onSurfaceVariant = Color(0xFFCFD9CF), secondaryContainer = Color(0xFF2B4A36), onSecondaryContainer = Pop.Cream,
    primaryContainer = Color(0xFF2B4A36), onPrimaryContainer = Pop.Cream, outline = Color(0xFF4A6354), outlineVariant = Color(0xFF2C4034)
)

val Vibes: List<VibeSpec> = listOf(
    VibeSpec("Leaf", "Soft cream, deep green, gentle shadows", Color(0xFF1F5C3A), CardStyle(), handcrafted = { if (it) popDark() else popLight() }),
    VibeSpec("Brutalist", "Thick ink borders, hard offset shadows, lime", Color(0xFF7BB500), CardStyle(radius = 14.dp, border = 1.5.dp, strongBorder = true, shadow = ShadowKind.HARD, pill = false), handcrafted = { if (it) brutalDark() else brutalLight() }),
    VibeSpec("Glass", "Frosted translucent cards, big curves", Color(0xFF3D6FE8), CardStyle(radius = 28.dp, border = 1.dp, glass = true), tintHue = 215f, tintSat = 0.5f),
    VibeSpec("Receipt Paper", "Monospace type, sharp corners, paper white", Color(0xFF3A3A3A), CardStyle(radius = 4.dp, border = 1.5.dp, strongBorder = true, shadow = ShadowKind.NONE, pill = false), face = Face.MONO, tintHue = 42f, tintSat = 0.3f),
    VibeSpec("Y2K Acid", "Acid yellow, hot pink, chunky outlines", Color(0xFFE0359F), CardStyle(radius = 8.dp, border = 2.dp, strongBorder = true, shadow = ShadowKind.HARD, hardOffset = 3.dp, pill = false), tintHue = 68f, tintSat = 0.75f),
    VibeSpec("Cozy Lo-Fi", "Warm browns, serif type, pillowy cards", Color(0xFF93643A), CardStyle(radius = 26.dp, border = 0.dp, shadow = ShadowKind.SOFT), face = Face.SERIF, tintHue = 32f, tintSat = 0.4f),
    VibeSpec("Material You", "Follows your wallpaper, flat tonal cards", Color(0xFF6750A4), CardStyle(radius = 16.dp, border = 0.dp, shadow = ShadowKind.NONE)),
    VibeSpec("Midnight Neon", "Glowing outlines on deep navy", Color(0xFF1DE9F6), CardStyle(radius = 18.dp, border = 1.5.dp, primaryBorder = true, shadow = ShadowKind.SOFT), tintHue = 240f, tintSat = 0.6f, bgDarkL = 0.045f),
    VibeSpec("Candy Pop", "Super round, bubblegum, chunky shadows", Color(0xFFF0449A), CardStyle(radius = 32.dp, border = 2.dp, strongBorder = true, shadow = ShadowKind.HARD, hardOffset = 4.dp, tintedShadow = true), tintHue = 335f, tintSat = 0.55f),
    VibeSpec("Minimal Mono", "Black and white, hairlines, nothing extra", Color(0xFF222222), CardStyle(radius = 6.dp, border = 1.dp, shadow = ShadowKind.NONE, pill = false), pureSurface = true),
    VibeSpec("Ocean Breeze", "Cool blues with a gentle gradient", Color(0xFF1E7FD8), CardStyle(radius = 22.dp, border = 0.dp, shadow = ShadowKind.SOFT, gradient = true), tintHue = 205f, tintSat = 0.5f),
    VibeSpec("Sunset Glow", "Warm orange gradient cards", Color(0xFFE5532D), CardStyle(radius = 24.dp, border = 0.dp, shadow = ShadowKind.SOFT, gradient = true), tintHue = 18f, tintSat = 0.55f)
)

private fun vibeSpec(name: String): VibeSpec = Vibes.firstOrNull { it.name == name } ?: when (name) {
    "Brutalist Lime" -> Vibes[1]
    "System" -> Vibes.first { it.name == "Material You" }
    else -> Vibes[0]
}

/** App colour presets. "Default" (not listed here) keeps each vibe's own colours. */
data class AccentPreset(val name: String, val swatch: Color)
val AccentPresets = listOf(
    AccentPreset("Green", Color(0xFF2E7D4F)), AccentPreset("Red", Color(0xFFD32F2F)), AccentPreset("Yellow", Color(0xFFF2B705)),
    AccentPreset("Pink", Color(0xFFE91E8C)), AccentPreset("Blue", Color(0xFF1E6FE0)), AccentPreset("Teal", Color(0xFF00897B)),
    AccentPreset("Purple", Color(0xFF7E57C2)), AccentPreset("Orange", Color(0xFFF57C00)), AccentPreset("Indigo", Color(0xFF3F51B5)),
    AccentPreset("Lime", Color(0xFF7CB518)), AccentPreset("Rose", Color(0xFFE5486A)), AccentPreset("Brown", Color(0xFF8D6E63)),
    AccentPreset("Slate", Color(0xFF546E7A)), AccentPreset("Mint", Color(0xFF2EC4A0)), AccentPreset("White", Color(0xFFF4F4F2)),
    AccentPreset("Black", Color(0xFF111111)), AccentPreset("Wallpaper", Color(0xFF7C9CF5))
)

/** A picked accent colour shows up for real: saturated brand colour, clearly tinted backgrounds. Dark mode is the same hue, just darker. */
private fun strongScheme(hue: Float, sat: Float, dark: Boolean): ColorScheme {
    fun hsl(h: Float, s: Float, l: Float) = Color.hsl(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    fun on(c: Color) = if (c.luminance() > 0.45f) Color(0xFF14110F) else Color.White
    return if (dark) {
        val primary = hsl(hue, sat, 0.58f); val container = hsl(hue, sat * 0.8f, 0.30f)
        darkColorScheme(
            primary = primary, onPrimary = on(primary), primaryContainer = container, onPrimaryContainer = hsl(hue, sat * 0.6f, 0.93f),
            secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
            secondaryContainer = container, onSecondaryContainer = hsl(hue, 0.2f, 0.93f),
            background = hsl(hue, sat * 0.45f, 0.10f), onBackground = hsl(hue, 0.15f, 0.94f),
            surface = hsl(hue, sat * 0.40f, 0.155f), onSurface = hsl(hue, 0.15f, 0.94f),
            surfaceVariant = hsl(hue, sat * 0.40f, 0.21f), onSurfaceVariant = hsl(hue, 0.15f, 0.82f),
            outline = hsl(hue, sat * 0.3f, 0.45f), outlineVariant = hsl(hue, sat * 0.3f, 0.28f)
        )
    } else {
        val primary = hsl(hue, sat, if (hue in 38f..70f) 0.45f else 0.42f); val container = hsl(hue, sat * 0.85f, 0.80f)
        lightColorScheme(
            primary = primary, onPrimary = on(primary), primaryContainer = container, onPrimaryContainer = hsl(hue, sat, 0.14f),
            secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
            secondaryContainer = container, onSecondaryContainer = hsl(hue, sat * 0.6f, 0.12f),
            background = hsl(hue, sat * 0.55f, 0.925f), onBackground = hsl(hue, 0.3f, 0.10f),
            surface = hsl(hue, sat * 0.40f, 0.975f), onSurface = hsl(hue, 0.3f, 0.10f),
            surfaceVariant = hsl(hue, sat * 0.45f, 0.87f), onSurfaceVariant = hsl(hue, 0.2f, 0.28f),
            outline = hsl(hue, sat * 0.3f, 0.55f), outlineVariant = hsl(hue, sat * 0.4f, 0.80f)
        )
    }
}

/** Builds a full light or dark colour scheme from one seed colour, shaped by the vibe's background tint. */
private fun buildScheme(seed: Color, dark: Boolean, spec: VibeSpec, amoled: Boolean, strong: Boolean = false): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val neutral = hsv[1] < 0.12f
    val hue = hsv[0]
    if (strong && !neutral) return strongScheme(hsv[0], hsv[1].coerceIn(0.55f, 1f), dark)
    val sat = if (neutral) 0f else (hsv[1] * 0.9f).coerceIn(0.35f, 0.95f)
    val tHue = if (neutral) (spec.tintHue ?: 0f) else (if (spec.tintHue != null && spec.name != "Midnight Neon") hue else hue)
    val tSat = if (neutral) (if (spec.tintHue != null) spec.tintSat * 0.5f else 0f) else spec.tintSat.coerceAtMost(0.5f)
    fun hsl(h: Float, s: Float, l: Float) = Color.hsl(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    return if (dark) {
        val primary = if (neutral) hsl(0f, 0f, 0.93f) else hsl(hue, sat, 0.72f)
        val bgL = if (amoled) 0f else spec.bgDarkL
        darkColorScheme(
            primary = primary, onPrimary = if (neutral) Color(0xFF111111) else hsl(hue, sat, 0.12f),
            primaryContainer = if (neutral) hsl(0f, 0f, 0.22f) else hsl(hue, sat * 0.6f, 0.26f), onPrimaryContainer = hsl(hue, if (neutral) 0f else sat, 0.92f),
            secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
            secondaryContainer = if (neutral) hsl(0f, 0f, 0.22f) else hsl(hue, sat * 0.6f, 0.26f), onSecondaryContainer = hsl(hue, 0.1f, 0.92f),
            background = if (amoled) Color.Black else hsl(tHue, tSat * 0.6f, bgL), onBackground = hsl(tHue, 0.1f, 0.93f),
            surface = if (amoled) Color(0xFF0E0E0E) else hsl(tHue, tSat * 0.55f, bgL + 0.045f), onSurface = hsl(tHue, 0.1f, 0.93f),
            surfaceVariant = if (amoled) Color(0xFF1A1A1A) else hsl(tHue, tSat * 0.6f, bgL + 0.10f), onSurfaceVariant = hsl(tHue, 0.1f, 0.8f),
            outline = hsl(tHue, 0.1f, 0.42f), outlineVariant = hsl(tHue, 0.1f, 0.24f)
        )
    } else {
        val primary = if (neutral) (if (seed.luminance() > 0.5f) Color(0xFF222222) else seed) else hsl(hue, sat, 0.34f)
        lightColorScheme(
            primary = primary, onPrimary = Color.White,
            primaryContainer = if (neutral) hsl(0f, 0f, 0.92f) else hsl(hue, sat * 0.8f, 0.90f), onPrimaryContainer = if (neutral) Color(0xFF161616) else hsl(hue, sat, 0.16f),
            secondary = Pop.Coral, onSecondary = Color.White, tertiary = Pop.Teal, onTertiary = Color.White,
            secondaryContainer = if (neutral) hsl(0f, 0f, 0.92f) else hsl(hue, sat * 0.8f, 0.90f), onSecondaryContainer = hsl(hue, 0.2f, 0.12f),
            background = if (spec.pureSurface) Color.White else hsl(tHue, tSat * 0.6f, 0.965f), onBackground = hsl(tHue, 0.2f, 0.10f),
            surface = if (spec.pureSurface) Color(0xFFFAFAFA) else hsl(tHue, tSat * 0.5f, 0.995f), onSurface = hsl(tHue, 0.2f, 0.10f),
            surfaceVariant = hsl(tHue, tSat * 0.7f, 0.93f), onSurfaceVariant = hsl(tHue, 0.1f, 0.30f),
            outline = if (spec.style.strongBorder) hsl(tHue, 0.2f, 0.12f) else hsl(tHue, 0.1f, 0.72f), outlineVariant = hsl(tHue, 0.12f, 0.88f)
        )
    }
}

@Composable
fun FinCipTheme(vibe: String, dark: Boolean, accent: String = "Default", content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val spec = vibeSpec(vibe)
    val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val wallpaper = dynamicOk && (accent == "Wallpaper" || (accent == "Default" && spec.name == "Material You"))
    val useDark = dark || (spec.name == "Material You" && systemDark)
    val preset = AccentPresets.firstOrNull { it.name == accent && it.name != "Wallpaper" }
    val scheme = when {
        wallpaper -> if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        preset != null -> buildScheme(preset.swatch, useDark, spec, amoled = preset.name == "Black", strong = true)
        spec.handcrafted != null -> spec.handcrafted.invoke(useDark)
        else -> buildScheme(spec.seed, useDark, spec, amoled = false)
    }
    val family = when (spec.face) { Face.MONO -> FontFamily.Monospace; Face.SERIF -> FontFamily.Serif; Face.POP -> PopFont }
    val typo = Typography().let { base ->
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = family), displayMedium = base.displayMedium.copy(fontFamily = family), displaySmall = base.displaySmall.copy(fontFamily = family),
            headlineSmall = base.headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
            titleSmall = base.titleSmall.copy(fontFamily = family, fontWeight = FontWeight.Bold),
            bodySmall = base.bodySmall.copy(fontFamily = family), labelMedium = base.labelMedium.copy(fontFamily = family), labelSmall = base.labelSmall.copy(fontFamily = family),
            headlineLarge = base.headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
            headlineMedium = base.headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 31.sp, letterSpacing = (-0.5).sp),
            titleLarge = base.titleLarge.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
            titleMedium = base.titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
            bodyLarge = base.bodyLarge.copy(fontFamily = family),
            bodyMedium = base.bodyMedium.copy(fontFamily = family),
            labelLarge = base.labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold)
        )
    }
    val r = spec.style.radius
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((r.value * 0.4f).dp), small = RoundedCornerShape((r.value * 0.6f).dp),
        medium = RoundedCornerShape((r.value * 0.75f).dp), large = RoundedCornerShape(r), extraLarge = RoundedCornerShape((r.value * 1.3f).dp)
    )
    CompositionLocalProvider(LocalCardStyle provides spec.style) {
        MaterialTheme(colorScheme = scheme, typography = typo, shapes = shapes, content = content)
    }
}
