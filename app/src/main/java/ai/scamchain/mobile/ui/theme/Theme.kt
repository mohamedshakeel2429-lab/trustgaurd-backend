package ai.scamchain.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TrustGuard Security Palette (Emerald & Sapphire Cyber Security)
val NeuralBg = Color(0xFF0B0E14)
val NeuralBgSecondary = Color(0xFF121722)
val NeuralCard = Color(0xFF1A2130)
val CyanAccent = Color(0xFF00C896) // Emerald Cyber Teal
val VioletAccent = Color(0xFF3B82F6) // Royal Sapphire Blue
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val NeuralOutline = Color(0xFF263346)

private val NeuralShieldColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = NeuralBg,
    primaryContainer = Color(0xFF0A3A2F),
    onPrimaryContainer = CyanAccent,
    secondary = VioletAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = VioletAccent,
    background = NeuralBg,
    onBackground = TextPrimary,
    surface = NeuralCard,
    onSurface = TextPrimary,
    surfaceVariant = NeuralBgSecondary,
    onSurfaceVariant = TextSecondary,
    outline = NeuralOutline,
    outlineVariant = Color(0xFF202C3D),
    error = DangerRed,
    onError = Color.White,
)

@Composable
fun ScamChainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeuralShieldColorScheme,
        content = content
    )
}

@Composable
fun TrustGuardTheme(content: @Composable () -> Unit) {
    ScamChainTheme(content = content)
}
