package com.rork.mycity.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

object CityColors {
    // Midnight Navy (dark) palette. Token names are kept from the original light theme.
    val Indigo = Color(0xFF6C7BFF)        // periwinkle: links, selection, info
    val IndigoDeep = Color(0xFFC9D0FF)    // light periwinkle: emphasized text on navy
    val IndigoSoft = Color(0xFF253055)    // periwinkle-tinted container
    val IndigoMist = Color(0xFF2E3A66)
    val Canvas = Color(0xFF0F172A)        // midnight navy background
    val Surface = Color(0xFF1E293B)       // cards
    val SurfaceHigh = Color(0xFF273449)   // raised controls inside cards
    val Glass = Color(0xD91E293B)         // frosted card over map/photos
    val GlassBorder = Color(0x33FFFFFF)
    val Ink = Color(0xFFF1F5F9)
    val Muted = Color(0xFF94A3B8)
    val Outline = Color(0xFF334155)
    val Saffron = Color(0xFFF58220)
    val SaffronSoft = Color(0xFF3A2A1F)
    val SaffronText = Color(0xFFFDBA74)
    val Green = Color(0xFF22C55E)
    val GreenSoft = Color(0xFF13322A)
    val GreenText = Color(0xFF86EFAC)
    val Red = Color(0xFFEF4444)
    val RedSoft = Color(0xFF3B1D26)
    val Amber = Color(0xFFF59E0B)
    val MapBase = Color(0xFF16213A)
    val MapBlock = Color(0xFF1C2944)
    val MapPark = Color(0xFF173A33)
}

private val DarkColors = darkColorScheme(
    primary = CityColors.Saffron,
    onPrimary = Color.White,
    primaryContainer = CityColors.SaffronSoft,
    onPrimaryContainer = CityColors.SaffronText,
    secondary = CityColors.Indigo,
    onSecondary = Color.White,
    secondaryContainer = CityColors.IndigoSoft,
    onSecondaryContainer = CityColors.IndigoDeep,
    tertiary = CityColors.Green,
    tertiaryContainer = CityColors.GreenSoft,
    error = CityColors.Red,
    errorContainer = CityColors.RedSoft,
    background = CityColors.Canvas,
    onBackground = CityColors.Ink,
    surface = CityColors.Surface,
    onSurface = CityColors.Ink,
    surfaceVariant = CityColors.SurfaceHigh,
    onSurfaceVariant = CityColors.Muted,
    surfaceContainerLowest = CityColors.Canvas,
    surfaceContainerLow = Color(0xFF172033),
    surfaceContainer = CityColors.Surface,
    surfaceContainerHigh = CityColors.SurfaceHigh,
    surfaceContainerHighest = Color(0xFF2F3D55),
    outline = Color(0xFF475569),
    outlineVariant = CityColors.Outline,
    inverseSurface = CityColors.Ink,
    inverseOnSurface = CityColors.Canvas,
)

private val CityTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp),
)

private val CityShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = CityTypography,
        shapes = CityShapes,
        content = content
    )
}
