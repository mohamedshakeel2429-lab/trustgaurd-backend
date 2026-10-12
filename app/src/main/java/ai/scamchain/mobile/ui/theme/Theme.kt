package ai.scamchain.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.Font
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp
import ai.scamchain.mobile.R

data class TrustGuardPalette(
    val background: Color, val surface: Color, val raised: Color, val border: Color,
    val text: Color, val muted: Color, val signal: Color,
    val high: Color, val suspicious: Color, val safe: Color, val inconclusive: Color,
)

private val InkMoss = TrustGuardPalette(
    Color(0xFF0C1411), Color(0xFF15201B), Color(0xFF1D2B25), Color(0xFF2A3A33),
    Color(0xFFF1EBDD), Color(0xFF9AA79F), Color(0xFFC8F560),
    Color(0xFFFF5A36), Color(0xFFFFB627), Color(0xFF5EE3A1), Color(0xFF8A948E),
)
private val WarmPaper = TrustGuardPalette(
    Color(0xFFF1EBDD), Color(0xFFE8E2D2), Color(0xFFDDD7C8), Color(0xFFB8B3A4),
    Color(0xFF15201B), Color(0xFF536159), Color(0xFF48620B),
    Color(0xFFB52C12), Color(0xFF8D5A00), Color(0xFF176B45), Color(0xFF59635D),
)

private val LocalTrustGuardPalette = staticCompositionLocalOf { InkMoss }

object Space {
    val xxs = 4.dp; val xs = 8.dp; val sm = 12.dp; val md = 16.dp
    val lg = 20.dp; val xl = 24.dp; val xxl = 32.dp
}
object Corner {
    val stamp = 4.dp; val small = 12.dp; val medium = 16.dp; val large = 22.dp; val extraLarge = 28.dp
}
object Elevation { val hairline = 1.dp; val card = 2.dp; val floating = 6.dp }
object Motion { const val quick = 140; const val standard = 240; const val reveal = 360; const val scan = 1800 }
private val DisplayFace = FontFamily(Font(R.font.bricolage_grotesque, FontWeight.Normal), Font(R.font.bricolage_grotesque, FontWeight.Bold))
private val BodyFace = FontFamily(Font(R.font.dm_sans, FontWeight.Normal), Font(R.font.dm_sans, FontWeight.Medium), Font(R.font.dm_sans, FontWeight.Bold))
val EvidenceMono = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal), Font(R.font.jetbrains_mono, FontWeight.Bold))

private val DarkScheme @Composable get() = darkColorScheme(
    primary = LocalTrustGuardPalette.current.signal, onPrimary = LocalTrustGuardPalette.current.background,
    primaryContainer = LocalTrustGuardPalette.current.raised, onPrimaryContainer = LocalTrustGuardPalette.current.signal,
    secondary = LocalTrustGuardPalette.current.safe, onSecondary = LocalTrustGuardPalette.current.background,
    background = LocalTrustGuardPalette.current.background, onBackground = LocalTrustGuardPalette.current.text,
    surface = LocalTrustGuardPalette.current.surface, onSurface = LocalTrustGuardPalette.current.text,
    surfaceVariant = LocalTrustGuardPalette.current.raised, onSurfaceVariant = LocalTrustGuardPalette.current.muted,
    outline = LocalTrustGuardPalette.current.border, outlineVariant = LocalTrustGuardPalette.current.border,
    error = LocalTrustGuardPalette.current.high, onError = LocalTrustGuardPalette.current.background,
)

private val LightScheme @Composable get() = lightColorScheme(
    primary = LocalTrustGuardPalette.current.signal, onPrimary = Color.White,
    primaryContainer = LocalTrustGuardPalette.current.raised, onPrimaryContainer = LocalTrustGuardPalette.current.text,
    secondary = LocalTrustGuardPalette.current.safe, onSecondary = Color.White,
    background = LocalTrustGuardPalette.current.background, onBackground = LocalTrustGuardPalette.current.text,
    surface = LocalTrustGuardPalette.current.surface, onSurface = LocalTrustGuardPalette.current.text,
    surfaceVariant = LocalTrustGuardPalette.current.raised, onSurfaceVariant = LocalTrustGuardPalette.current.muted,
    outline = LocalTrustGuardPalette.current.border, outlineVariant = LocalTrustGuardPalette.current.border,
    error = LocalTrustGuardPalette.current.high, onError = Color.White,
)

private val TrustGuardTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 54.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily = BodyFace), bodyMedium = TextStyle(fontFamily = BodyFace),
    bodySmall = TextStyle(fontFamily = BodyFace), labelLarge = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Bold),
)

// Screen-level aliases remain palette tokens so the existing Compose screens share the same design system.
val Palette: TrustGuardPalette @Composable get() = LocalTrustGuardPalette.current
val NeuralBg: Color @Composable get() = Palette.background
val NeuralBgSecondary: Color @Composable get() = Palette.surface
val NeuralCard: Color @Composable get() = Palette.raised
val CyanAccent: Color @Composable get() = Palette.signal
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

@Composable
fun ScamChainTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTrustGuardPalette provides if (darkTheme) InkMoss else WarmPaper) {
        MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, typography = TrustGuardTypography, content = content)
    }
}

@Composable
fun TrustGuardTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) = ScamChainTheme(darkTheme, content)
