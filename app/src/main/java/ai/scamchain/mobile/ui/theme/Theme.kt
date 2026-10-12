package ai.scamchain.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = Color(0xFF087E78),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F2EC),
    onPrimaryContainer = Color(0xFF063E3A),
    secondary = Color(0xFF476E69),
    onSecondary = Color.White,
    background = Color(0xFFF4F7F8),
    onBackground = Color(0xFF101C2C),
    surface = Color.White,
    onSurface = Color(0xFF101C2C),
    surfaceVariant = Color(0xFFE9EFF1),
    onSurfaceVariant = Color(0xFF526373),
    outline = Color(0xFFD7E0E3),
    error = Color(0xFFB6473F)
)

@Composable
fun ScamChainTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
