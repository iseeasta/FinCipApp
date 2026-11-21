package com.fincip.nativeapp.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/** FinCip "Pastel Pop" design tokens: soft colour-blocking, big radii, ink-dark accents. */
object Pop {
    val Cream = Color(0xFFFFF8E1)
    val Ink = Color(0xFF1E1B2E)
    val Coral = Color(0xFFF26D85)
    val Teal = Color(0xFF2E8C7F)
    val Sun = Color(0xFFF7B733)
    val Lavender = Color(0xFFB7A8F5)
    val Peach = Color(0xFFF8A66B)
    val Mint = Color(0xFFCFEBD8)
    val Indigo = Color(0xFF26214A)
    val IndigoHi = Color(0xFF332D63)
    val Gain = Color(0xFF1F9D63)
    val Loss = Color(0xFFE5484D)
    val Radius = 28.dp
    val RadiusSmall = 18.dp
}

/** Readable text colour for any block colour, in light and dark mode. */
fun contentOn(bg: Color): Color = if (bg.luminance() > 0.42f) Pop.Ink else Pop.Cream

private fun popLight() = lightColorScheme(
    primary = Pop.Ink, onPrimary = Pop.Cream,
    secondary = Pop.Coral, onSecondary = Color.White,
    tertiary = Pop.Teal, onTertiary = Color.White,
    background = Pop.Cream, onBackground = Pop.Ink,
    surface = Color.White, onSurface = Pop.Ink,
    surfaceVariant = Color(0xFFF3ECCB), onSurfaceVariant = Color(0xFF4A4660),
    secondaryContainer = Color(0xFFFFE08A), onSecondaryContainer = Pop.Ink,
    primaryContainer = Pop.Lavender, onPrimaryContainer = Pop.Ink,
    outline = Color(0xFFCFC8A8), outlineVariant = Color(0xFFE6DFBC)
)

private fun popDark() = darkColorScheme(
    primary = Color(0xFFF6C98D), onPrimary = Pop.Ink,
    secondary = Pop.Coral, onSecondary = Color.White,
    tertiary = Pop.Teal, onTertiary = Color.White,
    background = Pop.Indigo, onBackground = Pop.Cream,
    surface = Pop.IndigoHi, onSurface = Pop.Cream,
    surfaceVariant = Color(0xFF3D3775), onSurfaceVariant = Color(0xFFD9D3F2),
    secondaryContainer = Color(0xFF4C4488), onSecondaryContainer = Pop.Cream,
    primaryContainer = Color(0xFF5A4FA3), onPrimaryContainer = Pop.Cream,
    outline = Color(0xFF5B5499), outlineVariant = Color(0xFF443E7D)
)

@Composable
fun FinCipTheme(vibe: String, dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = dark || ((vibe == "System" || vibe == "Material You") && systemDark)

    val scheme = when (vibe) {
        "Pastel Pop" -> if (useDark) popDark() else popLight()
        "Material You", "System" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (useDark) fallbackDark() else fallbackLight()
        "Y2K Acid" -> if (useDark) darkColorScheme(
            primary = Color(0xFFD7FF24), onPrimary = Color(0xFF111300), secondary = Color(0xFFFF5BD6),
            background = Color(0xFF171B13), onBackground = Color(0xFFF4F7E8), surface = Color(0xFF252B1E),
            onSurface = Color(0xFFF4F7E8), tertiary = Color(0xFF7CDFFF), outline = Color(0xFF151515)
        ) else lightColorScheme(
            primary = Color(0xFFB9E600), onPrimary = Color(0xFF111500), secondary = Color(0xFFE84CBF),
            background = Color(0xFFF5F9D8), onBackground = Color(0xFF161A0F), surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF161A0F), tertiary = Color(0xFF0088B6), outline = Color(0xFF171717)
        )
        "Cozy Lo-Fi" -> if (useDark) darkColorScheme(
            primary = Color(0xFFD7A66D), onPrimary = Color(0xFF21170F), secondary = Color(0xFFB9C28D),
            background = Color(0xFF27231F), onBackground = Color(0xFFF1E4D1), surface = Color(0xFF38312A),
            onSurface = Color(0xFFF1E4D1), tertiary = Color(0xFFD18C83), outline = Color(0xFF1D1A17)
        ) else lightColorScheme(
            primary = Color(0xFF93643A), onPrimary = Color.White, secondary = Color(0xFF66764F),
            background = Color(0xFFF5EBDD), onBackground = Color(0xFF2B251E), surface = Color(0xFFFFF9EF),
            onSurface = Color(0xFF2B251E), tertiary = Color(0xFFB66F67), outline = Color(0xFF25211B)
        )
        else -> if (useDark) darkColorScheme(
            primary = Color(0xFFFFD166), onPrimary = Color(0xFF211A08), secondary = Color(0xFFD9D3C8),
            background = Color(0xFF242321), onBackground = Color(0xFFF1EEE6), surface = Color(0xFF33312D),
            onSurface = Color(0xFFF1EEE6), tertiary = Color(0xFFFFE9B3), outline = Color(0xFF111111)
        ) else lightColorScheme(
            primary = Color(0xFF252525), onPrimary = Color.White, secondary = Color(0xFF6D675D),
            background = Color(0xFFF8F4E9), onBackground = Color(0xFF171717), surface = Color(0xFFFFFDF7),
            onSurface = Color(0xFF171717), tertiary = Color(0xFFB18B3D), outline = Color(0xFF111111)
        )
    }

    val typo = Typography().let { base ->
        val family = if (vibe == "Receipt Paper") FontFamily.Monospace else FontFamily.SansSerif
        base.copy(
            headlineLarge = base.headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
            headlineMedium = base.headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 31.sp, letterSpacing = (-0.5).sp),
            titleLarge = base.titleLarge.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
            titleMedium = base.titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
            bodyLarge = base.bodyLarge.copy(fontFamily = family),
            bodyMedium = base.bodyMedium.copy(fontFamily = family),
            labelLarge = base.labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold)
        )
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(Pop.RadiusSmall), large = RoundedCornerShape(Pop.Radius),
        extraLarge = RoundedCornerShape(32.dp)
    )
    MaterialTheme(colorScheme = scheme, typography = typo, shapes = shapes, content = content)
}

private fun fallbackLight() = lightColorScheme(
    primary = Color(0xFF516E58), onPrimary = Color.White, secondary = Color(0xFFE2B650),
    background = Color(0xFFF4F0E7), onBackground = Color(0xFF1B1B18), surface = Color(0xFFFFFCF4),
    onSurface = Color(0xFF1B1B18), tertiary = Color(0xFFB97871), outline = Color(0xFF111111)
)
private fun fallbackDark() = darkColorScheme(
    primary = Color(0xFFA8C8A9), onPrimary = Color(0xFF152118), secondary = Color(0xFFE5C46D),
    background = Color(0xFF20231F), onBackground = Color(0xFFF0EEE7), surface = Color(0xFF2D332D),
    onSurface = Color(0xFFF0EEE7), tertiary = Color(0xFFD0968F), outline = Color(0xFF050505)
)
