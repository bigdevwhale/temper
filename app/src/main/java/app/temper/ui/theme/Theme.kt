package app.temper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.temper.R
import app.temper.data.ThemeMode

/** A smithy: ash and steel by day, coals in a dark forge at night. Ember for heat, ice for cold. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val ice: Color,
    val iceSoft: Color,
    val steel: Color,
    val steelSoft: Color,
    val gold: Color,
    val goldSoft: Color,
    val sage: Color,
    val sageSoft: Color,
    val danger: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFF3F0EB),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF9F7F3),
    line = Color(0xFFE5DFD7),
    ink = Color(0xFF1D1A18),
    muted = Color(0xFF6C635C),
    faint = Color(0xFFA49A91),
    accent = Color(0xFFD5521C),
    accentSoft = Color(0xFFFBE2D4),
    onAccent = Color(0xFFFFFFFF),
    ice = Color(0xFF2A7AB6),
    iceSoft = Color(0xFFDCECF8),
    steel = Color(0xFF5B6873),
    steelSoft = Color(0xFFE6EAEE),
    gold = Color(0xFFB4821A),
    goldSoft = Color(0xFFF6EACC),
    sage = Color(0xFF3A8B61),
    sageSoft = Color(0xFFDDEFE4),
    danger = Color(0xFFD0412F),
    dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF110F0E),
    surface = Color(0xFF1A1715),
    surface2 = Color(0xFF211D1A),
    line = Color(0xFF2E2824),
    ink = Color(0xFFF2EAE2),
    muted = Color(0xFFB0A398),
    faint = Color(0xFF786C62),
    accent = Color(0xFFFF7A3D),
    accentSoft = Color(0xFF3A2016),
    onAccent = Color(0xFF1E0F07),
    ice = Color(0xFF72C4F2),
    iceSoft = Color(0xFF12283A),
    steel = Color(0xFFAAB6C0),
    steelSoft = Color(0xFF242A30),
    gold = Color(0xFFE8BA5E),
    goldSoft = Color(0xFF372B14),
    sage = Color(0xFF7BC99C),
    sageSoft = Color(0xFF1A3125),
    danger = Color(0xFFFF6B5C),
    dark = true,
)

/** The oxide colours of tempered steel, from raw iron to grey: one per level. */
val TemperColors = listOf(
    Color(0xFF8C8F93),
    Color(0xFFE9DDA4),
    Color(0xFFD8B256),
    Color(0xFFAF773A),
    Color(0xFF834C93),
    Color(0xFF3553A3),
    Color(0xFF6BA0D6),
    Color(0xFF9BA7B1),
)

fun temperColor(level: Int): Color = TemperColors[(level - 1).coerceIn(0, TemperColors.size - 1)]

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Stamped, heavy letters for titles and the timer. */
val Forge = FontFamily(Font(R.font.russo_one, FontWeight.Normal))

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(28.dp)
    val RLg = RoundedCornerShape(22.dp)
    val RMd = RoundedCornerShape(16.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Forge, fontSize = 30.sp, lineHeight = 36.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Forge, fontSize = 24.sp, lineHeight = 30.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.01).em, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Forge, fontSize = 22.sp, color = U.c.ink, fontFeatureSettings = "tnum")
    val Timer: TextStyle @Composable get() = TextStyle(fontFamily = Forge, fontSize = 64.sp, color = U.c.ink, fontFeatureSettings = "tnum")
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val Quote: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 18.sp, lineHeight = 26.sp, color = U.c.ink)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun TemperTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.accentSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
