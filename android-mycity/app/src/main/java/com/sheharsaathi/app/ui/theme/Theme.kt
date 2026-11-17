package com.sheharsaathi.app.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

/**
 * Every colour the app draws with. There are two sets, [DarkPalette] and [LightPalette];
 * which one is in use is decided by the appearance setting in Profile (see [ThemeStore]).
 *
 * Screens never name a palette directly. They read [CityColors], which follows whichever one
 * is active, so a single assignment repaints the whole app.
 */
data class CityPalette(
    val isLight: Boolean,
    val indigo: Color,
    val indigoDeep: Color,
    val indigoSoft: Color,
    val indigoMist: Color,
    val canvas: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val glass: Color,
    val glassBorder: Color,
    val ink: Color,
    val muted: Color,
    val outline: Color,
    val saffron: Color,
    val saffronSoft: Color,
    val saffronText: Color,
    val green: Color,
    val greenSoft: Color,
    val greenText: Color,
    val red: Color,
    val redSoft: Color,
    val redText: Color,
    val amber: Color,
    val headerWash: Color,
    val splashGlow: Color,
    val mapBase: Color,
    val mapBlock: Color,
    val mapPark: Color,
)

/** Midnight Navy: the app's original look. */
val DarkPalette = CityPalette(
    isLight = false,
    indigo = Color(0xFF6C7BFF),        // periwinkle: links, selection, info
    indigoDeep = Color(0xFFC9D0FF),    // emphasised text on a periwinkle container
    indigoSoft = Color(0xFF253055),    // periwinkle-tinted container
    indigoMist = Color(0xFF2E3A66),
    canvas = Color(0xFF0F172A),        // midnight navy background
    surface = Color(0xFF1E293B),       // cards
    surfaceHigh = Color(0xFF273449),   // raised controls inside cards
    glass = Color(0xD91E293B),         // frosted card over the map or a photo
    glassBorder = Color(0x33FFFFFF),
    ink = Color(0xFFF1F5F9),
    muted = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    saffron = Color(0xFFF58220),
    saffronSoft = Color(0xFF3A2A1F),
    saffronText = Color(0xFFFDBA74),
    green = Color(0xFF22C55E),
    greenSoft = Color(0xFF13322A),
    greenText = Color(0xFF86EFAC),
    red = Color(0xFFEF4444),
    redSoft = Color(0xFF3B1D26),
    redText = Color(0xFFFCA5A5),
    amber = Color(0xFFF59E0B),
    headerWash = Color(0xFF1A2547),
    splashGlow = Color(0xFF1E2A4A),
    mapBase = Color(0xFF16213A),
    mapBlock = Color(0xFF1C2944),
    mapPark = Color(0xFF173A33),
)

/**
 * Daylight: the same app in light. The saffron and periwinkle of the brand stay; everything
 * that carries words was picked to stay readable on a pale background (every text colour here
 * reaches at least 4.5:1 against the surface it sits on).
 */
val LightPalette = CityPalette(
    isLight = true,
    indigo = Color(0xFF4F57D8),
    indigoDeep = Color(0xFF2A2F86),
    indigoSoft = Color(0xFFE8EAFD),
    indigoMist = Color(0xFFD9DEFB),
    canvas = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEEF1F6),
    glass = Color(0xF0FFFFFF),
    glassBorder = Color(0x1F0F172A),
    ink = Color(0xFF0F172A),
    muted = Color(0xFF5A6878),
    outline = Color(0xFFDDE3EA),
    saffron = Color(0xFFF58220),
    saffronSoft = Color(0xFFFFF0E0),
    saffronText = Color(0xFF9A4B06),
    green = Color(0xFF15A34A),
    greenSoft = Color(0xFFE2F7E9),
    greenText = Color(0xFF10672F),
    red = Color(0xFFD92D20),
    redSoft = Color(0xFFFDE7E6),
    redText = Color(0xFF9B1C14),
    amber = Color(0xFFC77700),
    headerWash = Color(0xFFE4E9F7),
    splashGlow = Color(0xFFE8ECF8),
    mapBase = Color(0xFFE9EEF5),
    mapBlock = Color(0xFFDBE3ED),
    mapPark = Color(0xFFD8EBDC),
)

/**
 * The colours screens draw with. Each one follows the active palette, so switching appearance
 * repaints everything without a single screen having to know which theme is on.
 */
object CityColors {
    internal var palette by mutableStateOf(DarkPalette)

    /** True while the light theme is showing; for the rare place that needs to know. */
    val isLight: Boolean get() = palette.isLight

    val Indigo: Color get() = palette.indigo
    val IndigoDeep: Color get() = palette.indigoDeep
    val IndigoSoft: Color get() = palette.indigoSoft
    val IndigoMist: Color get() = palette.indigoMist
    val Canvas: Color get() = palette.canvas
    val Surface: Color get() = palette.surface
    val SurfaceHigh: Color get() = palette.surfaceHigh
    val Glass: Color get() = palette.glass
    val GlassBorder: Color get() = palette.glassBorder
    val Ink: Color get() = palette.ink
    val Muted: Color get() = palette.muted
    val Outline: Color get() = palette.outline
    val Saffron: Color get() = palette.saffron
    val SaffronSoft: Color get() = palette.saffronSoft
    val SaffronText: Color get() = palette.saffronText
    val Green: Color get() = palette.green
    val GreenSoft: Color get() = palette.greenSoft
    val GreenText: Color get() = palette.greenText
    val Red: Color get() = palette.red
    val RedSoft: Color get() = palette.redSoft
    val RedText: Color get() = palette.redText
    val Amber: Color get() = palette.amber
    val HeaderWash: Color get() = palette.headerWash
    val SplashGlow: Color get() = palette.splashGlow
    val MapBase: Color get() = palette.mapBase
    val MapBlock: Color get() = palette.mapBlock
    val MapPark: Color get() = palette.mapPark

    /** White text sits on saffron and periwinkle fills in both themes. */
    val OnBrand: Color get() = Color.White
}

/**
 * Keeps a bright accent readable. The topic and category colours were picked to glow on navy,
 * so several are too pale to read on white; in the light theme each is deepened until it reaches
 * 4.5:1 against a white card, keeping its hue. In the dark theme it is returned untouched.
 */
fun Color.readableAccent(): Color {
    if (!CityColors.isLight) return this
    var c = this
    var guard = 0
    while (contrastOnWhite(c) < 4.5f && guard++ < 16) {
        c = Color(c.red * 0.88f, c.green * 0.88f, c.blue * 0.88f, c.alpha)
    }
    return c
}

private fun contrastOnWhite(c: Color): Float {
    val l = c.luminance()
    return 1.05f / (l + 0.05f)
}

private fun schemeFor(p: CityPalette) = if (p.isLight) {
    lightColorScheme(
        primary = p.saffron, onPrimary = Color.White,
        primaryContainer = p.saffronSoft, onPrimaryContainer = p.saffronText,
        secondary = p.indigo, onSecondary = Color.White,
        secondaryContainer = p.indigoSoft, onSecondaryContainer = p.indigoDeep,
        tertiary = p.green, tertiaryContainer = p.greenSoft,
        error = p.red, errorContainer = p.redSoft, onErrorContainer = p.redText,
        background = p.canvas, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink,
        surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.muted,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAFBFD),
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surfaceHigh,
        surfaceContainerHighest = Color(0xFFE6EAF1),
        outline = Color(0xFFB9C2CE), outlineVariant = p.outline,
        inverseSurface = p.ink, inverseOnSurface = p.canvas,
    )
} else {
    darkColorScheme(
        primary = p.saffron, onPrimary = Color.White,
        primaryContainer = p.saffronSoft, onPrimaryContainer = p.saffronText,
        secondary = p.indigo, onSecondary = Color.White,
        secondaryContainer = p.indigoSoft, onSecondaryContainer = p.indigoDeep,
        tertiary = p.green, tertiaryContainer = p.greenSoft,
        error = p.red, errorContainer = p.redSoft, onErrorContainer = p.redText,
        background = p.canvas, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink,
        surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.muted,
        surfaceContainerLowest = p.canvas,
        surfaceContainerLow = Color(0xFF172033),
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surfaceHigh,
        surfaceContainerHighest = Color(0xFF2F3D55),
        outline = Color(0xFF475569), outlineVariant = p.outline,
        inverseSurface = p.ink, inverseOnSurface = p.canvas,
    )
}

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
fun AppTheme(choice: ThemeChoice = ThemeChoice.SYSTEM, content: @Composable () -> Unit) {
    val dark = choice.isDark(isSystemInDarkTheme())
    val palette = if (dark) DarkPalette else LightPalette
    // Set before the children compose, so the first frame is already in the right theme.
    if (CityColors.palette !== palette) CityColors.palette = palette

    // Dark theme needs pale status-bar icons, light theme needs dark ones.
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(dark) {
            val window = view.context.findActivity()?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = schemeFor(palette),
        typography = CityTypography,
        shapes = CityShapes,
        content = content,
    )
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
