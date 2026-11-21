package com.fincip.nativeapp.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun FinCipTheme(vibe: String, dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = dark || ((vibe == "System" || vibe == "Material You") && systemDark)

    val scheme = when (vibe) {
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
            headlineLarge = base.headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 34.sp),
            headlineMedium = base.headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
            titleLarge = base.titleLarge.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
            titleMedium = base.titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
            bodyLarge = base.bodyLarge.copy(fontFamily = family),
            bodyMedium = base.bodyMedium.copy(fontFamily = family),
            labelLarge = base.labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold)
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typo, content = content)
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
