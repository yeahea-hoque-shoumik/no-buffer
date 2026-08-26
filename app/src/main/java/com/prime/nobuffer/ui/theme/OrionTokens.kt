package com.prime.nobuffer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class OrionColors(
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val borderHi: Color,
    val accent: Color,
    val accentGlow: Color,
    val teal: Color,
    val text: Color,
    val textMid: Color,
    val textDim: Color,
    val pill: Color,
    val isDark: Boolean
)

object OrionTokens {
    val Dark = OrionColors(
        bg = Color(0xFF08080F),
        surface = Color(0xFF101018),
        elevated = Color(0x8017172A),
        border = Color(0xFF252538),
        borderHi = Color(0xFF35355A),
        accent = Color(0xFF7B6EF5),
        accentGlow = Color(0x407B6EF5),
        teal = Color(0xFF36C9B0),
        text = Color(0xFFF0EFF8),
        textMid = Color(0xFF9994B8),
        textDim = Color(0xFF504E68),
        pill = Color(0x80141420),
        isDark = true
    )

    val Light = OrionColors(
        bg = Color(0xFFF4F3F8),
        surface = Color(0xFFFFFFFF),
        elevated = Color(0xFFECE9F8),
        border = Color(0xFFE0DCF3),
        borderHi = Color(0xFFC8C2EE),
        accent = Color(0xFF6B5EE4),
        accentGlow = Color(0x406B5EE4),
        teal = Color(0xFF1DB89D),
        text = Color(0xFF14122A),
        textMid = Color(0xFF6B6585),
        textDim = Color(0xFFBAAFCC),
        pill = Color(0xD0FFFFFF),
        isDark = false
    )

    val Incognito = Dark.copy(
        bg = Color(0xFF1A1A1A),
        surface = Color(0xFF1A1A1A),
        elevated = Color(0xCC262626)
    )
}

val LocalOrionColors = staticCompositionLocalOf { OrionTokens.Dark }

object Orion {
    val colors: OrionColors
        @Composable get() = LocalOrionColors.current
}
