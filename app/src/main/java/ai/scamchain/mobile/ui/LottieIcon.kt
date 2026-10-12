package ai.scamchain.mobile.ui

import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

@Composable
fun LottieIcon(
    animationRes: Int,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    tint: Color,
    isPlaying: Boolean = true,
    repeat: Boolean = false,
) {
    val context = LocalContext.current
    val reduceMotion = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying && !reduceMotion,
        iterations = if (repeat && !reduceMotion) LottieConstants.IterateForever else 1,
    )
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = tint.toArgb(),
            keyPath = arrayOf("**", "Stroke 1"),
        ),
    )
    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 1f else progress },
        modifier = modifier.size(size).semantics { contentDescription = description },
        dynamicProperties = dynamicProperties,
    )
}
