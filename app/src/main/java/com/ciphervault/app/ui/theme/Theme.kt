package com.ciphervault.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ObsidianDarkColorScheme = darkColorScheme(
    primary = Color(0xFF35E0B0),
    onPrimary = Color(0xFF04110D),
    primaryContainer = Color(0xFF123A32),
    onPrimaryContainer = Color(0xFFB9F5E6),
    secondary = Color(0xFF7CC7FF),
    onSecondary = Color(0xFF041018),
    background = Color(0xFF080C10),
    onBackground = Color(0xFFE8F6F2),
    surface = Color(0xFF0F171F),
    onSurface = Color(0xFFE8F6F2),
    surfaceVariant = Color(0xFF16222C),
    onSurfaceVariant = Color(0xFF80958F),
    outline = Color(0xFF2A3A42),
    outlineVariant = Color(0xFF1B2A33),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF2A0509),
)

val ObsidianTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp,
    ),
)

val ObsidianShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CipherVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ObsidianDarkColorScheme,
        typography = ObsidianTypography,
        shapes = ObsidianShapes,
        content = content,
    )
}
