package ai.scamchain.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.scamchain.mobile.R

/** Semantic colors represented in the Nightwatch Signal Figma foundations. */
object TrustGuardColors {
    val background = Color(0xFF070B16)
    val surface = Color(0xFF0C1223)
    val raised = Color(0xFF131B2F)
    val elevated = Color(0xFF1A2139)
    val border = Color(0xFF303B59)
    val textPrimary = Color(0xFFE8F2FF)
    val textSecondary = Color(0xFF96A8C7)
    val textSubtle = Color(0xFF6B80A6)
    val cyan = Color(0xFF2EE3F7)
    val cyanMuted = Color(0xFF1A5E7A)
    val violet = Color(0xFFA67AFA)
    val textOnSignal = Color(0xFF07101C)
    val riskHigh = Color(0xFFFF575E)
    val riskSuspicious = Color(0xFFFFB547)
    val riskSafe = Color(0xFF47DEAD)
    val riskUnclear = Color(0xFF9CADD1)
}

data class TrustGuardPalette(
    val background: Color, val surface: Color, val raised: Color, val elevated: Color,
    val border: Color, val text: Color, val muted: Color, val subtle: Color,
    val signal: Color, val signalMuted: Color, val violet: Color, val textOnSignal: Color,
    val high: Color, val suspicious: Color, val safe: Color, val inconclusive: Color,
)

private val Nightwatch = TrustGuardPalette(
    background = TrustGuardColors.background,
    surface = TrustGuardColors.surface,
    raised = TrustGuardColors.raised,
    elevated = TrustGuardColors.elevated,
    border = TrustGuardColors.border,
    text = TrustGuardColors.textPrimary,
    muted = TrustGuardColors.textSecondary,
    subtle = TrustGuardColors.textSubtle,
    signal = TrustGuardColors.cyan,
    signalMuted = TrustGuardColors.cyanMuted,
    violet = TrustGuardColors.violet,
    textOnSignal = TrustGuardColors.textOnSignal,
    high = TrustGuardColors.riskHigh,
    suspicious = TrustGuardColors.riskSuspicious,
    safe = TrustGuardColors.riskSafe,
    inconclusive = TrustGuardColors.riskUnclear,
)

private val Daylight = TrustGuardPalette(
    background = Color(0xFFF2F6FC), surface = Color(0xFFFFFFFF), raised = Color(0xFFE9EEF8),
    elevated = Color(0xFFDFE7F5), border = Color(0xFFC7D1E6), text = Color(0xFF0E172B),
    muted = Color(0xFF3D4D69), subtle = Color(0xFF61708F), signal = Color(0xFF05829E),
    signalMuted = Color(0xFFC4E6ED), violet = Color(0xFF6140BA), textOnSignal = Color(0xFF0E172B),
    high = Color(0xFFB81F33), suspicious = Color(0xFF915705), safe = Color(0xFF0A7556),
    inconclusive = Color(0xFF4D5E82),
)

private val LocalTrustGuardPalette = staticCompositionLocalOf { Nightwatch }

object Space {
    val xxs = 4.dp; val xs = 8.dp; val sm = 12.dp; val md = 16.dp
    val lg = 20.dp; val xl = 24.dp; val xxl = 32.dp
}
object Corner {
    val stamp = 6.dp; val small = 12.dp; val medium = 16.dp; val large = 22.dp; val extraLarge = 28.dp
}
object Elevation { val hairline = 1.dp; val card = 2.dp; val floating = 8.dp }
object Motion { const val quick = 140; const val standard = 240; const val reveal = 360; const val scan = 1800 }

private val DisplayFace = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.Normal),
    Font(R.font.bricolage_grotesque, FontWeight.Bold),
)
private val BodyFace = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.Bold),
)
val EvidenceMono = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal), Font(R.font.jetbrains_mono, FontWeight.Bold))

private val TrustGuardTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 54.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFace, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFace, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = BodyFace, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
)

private val TrustGuardShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(Corner.small),
    medium = RoundedCornerShape(Corner.medium),
    large = RoundedCornerShape(Corner.large),
    extraLarge = RoundedCornerShape(Corner.extraLarge),
)

val Palette: TrustGuardPalette @Composable get() = LocalTrustGuardPalette.current
val NeuralBg: Color @Composable get() = Palette.background
val NeuralBgSecondary: Color @Composable get() = Palette.surface
val NeuralCard: Color @Composable get() = Palette.raised
val CyanAccent: Color @Composable get() = Palette.signal
val VioletAccent: Color @Composable get() = Palette.violet
val SuccessGreen: Color @Composable get() = Palette.safe
val WarningAmber: Color @Composable get() = Palette.suspicious
val DangerRed: Color @Composable get() = Palette.high
val TextPrimary: Color @Composable get() = Palette.text
val TextSecondary: Color @Composable get() = Palette.muted
val NeuralOutline: Color @Composable get() = Palette.border
val RiskHigh: Color @Composable get() = Palette.high
val RiskSuspicious: Color @Composable get() = Palette.suspicious
val RiskSafe: Color @Composable get() = Palette.safe
val RiskInconclusive: Color @Composable get() = Palette.inconclusive

private val DarkScheme @Composable get() = darkColorScheme(
    primary = Palette.signal, onPrimary = Palette.textOnSignal,
    primaryContainer = Palette.signalMuted, onPrimaryContainer = Palette.signal,
    secondary = Palette.violet, onSecondary = Color.White,
    tertiary = Palette.safe, onTertiary = Color(0xFF062117),
    background = Palette.background, onBackground = Palette.text,
    surface = Palette.surface, onSurface = Palette.text,
    surfaceVariant = Palette.raised, onSurfaceVariant = Palette.muted,
    outline = Palette.border, outlineVariant = Palette.border,
    error = Palette.high, onError = Color.White,
)

private val LightScheme @Composable get() = lightColorScheme(
    primary = Palette.signal, onPrimary = Palette.textOnSignal,
    primaryContainer = Palette.signalMuted, onPrimaryContainer = Palette.text,
    secondary = Palette.violet, onSecondary = Color.White,
    tertiary = Palette.safe, onTertiary = Color.White,
    background = Palette.background, onBackground = Palette.text,
    surface = Palette.surface, onSurface = Palette.text,
    surfaceVariant = Palette.raised, onSurfaceVariant = Palette.muted,
    outline = Palette.border, outlineVariant = Palette.border,
    error = Palette.high, onError = Color.White,
)

@Composable
fun ScamChainTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTrustGuardPalette provides if (darkTheme) Nightwatch else Daylight) {
        MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, typography = TrustGuardTypography, shapes = TrustGuardShapes, content = content)
    }
}

@Composable
fun TrustGuardTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) = ScamChainTheme(darkTheme, content)
