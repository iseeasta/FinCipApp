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

private fun popLight() = lightColorScheme(
    primary = Pop.Forest, onPrimary = Color.White,
    secondary = Pop.Coral, onSecondary = Color.White,
    tertiary = Pop.Teal, onTertiary = Color.White,
    background = Pop.Cream, onBackground = Pop.Ink,
    surface = Color(0xFFFFFDF8), onSurface = Pop.Ink,
    surfaceVariant = Color(0xFFF1EDDF), onSurfaceVariant = Color(0xFF45524A),
    secondaryContainer = Pop.Mint, onSecondaryContainer = Pop.Ink,
    primaryContainer = Pop.Mint, onPrimaryContainer = Pop.Forest,
    outline = Color(0xFFC9CDBE), outlineVariant = Color(0xFFE8E6D8)
)

private fun popDark() = darkColorScheme(
    primary = Pop.Lime, onPrimary = Pop.Ink,
    secondary = Pop.Coral, onSecondary = Color.White,
    tertiary = Pop.Teal, onTertiary = Color.White,
    background = Pop.Indigo, onBackground = Pop.Cream,
    surface = Pop.IndigoHi, onSurface = Pop.Cream,
    surfaceVariant = Color(0xFF262926), onSurfaceVariant = Color(0xFFCFD3CF),
    secondaryContainer = Color(0xFF2E3A22), onSecondaryContainer = Pop.Cream,
    primaryContainer = Color(0xFF2E3A22), onPrimaryContainer = Pop.Cream,
    outline = Color(0xFF555955), outlineVariant = Color(0xFF2A2D2A)
)

@Composable
fun FinCipTheme(vibe: String, dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = dark || ((vibe == "System" || vibe == "Material You") && systemDark)

    val scheme = when (vibe) {
        "Pastel Pop", "Brutalist Lime", "Leaf" -> if (useDark) popDark() else popLight()
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
        val family = if (vibe == "Receipt Paper") FontFamily.Monospace else PopFont
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
