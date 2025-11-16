package eu.jelinek.hranolky.ui.start

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
internal fun ButtonPad(
    offset: Float,
    isPressed: Boolean,
    modifier: Modifier = Modifier
) {
    val padColor = Color(0xFFFFB839)

    // Define colors close to the original yellow for subtle pulsing
    val lightYellow = Color(0xFFFFDD41)   // Slightly lighter yellow
    val originalYellow = padColor         // Original yellow

    // Infinite traveling wave animation - light flows smoothly up and down
    val infiniteTransition = rememberInfiniteTransition(label = "colorPulse")
    val wavePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse // Smooth back-and-forth flow
        ),
        label = "wavePosition"
    )

    // Create a traveling wave effect - the light spot moves smoothly
    // Each position gets brighter when the wave passes through it
    val positions = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    val colors = positions.map { position ->
        // Calculate distance from wave position
        val distance = kotlin.math.abs(wavePosition - position)
        // Light is brightest when wave is at this position (distance = 0)
        // Smoothly fade to darker when wave is far away
        // Using a wider spread (3f instead of 2f) for smoother gradient
        val brightness = (1f - (distance * 3f).coerceIn(0f, 1f)).coerceIn(0f, 1f)

        // Interpolate between colors based on brightness
        lerp(
            originalYellow, // Base color when wave is far
            lightYellow,    // Peak brightness when wave is here
            brightness
        )
    }

    Box(
        modifier = modifier
            .width(40.dp)
            .height(122.dp)
            .offset { IntOffset(offset.dp.roundToPx(), 55.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 8.dp else 4.dp,
                shape = RoundedCornerShape(50.dp),
                spotColor = padColor.copy(alpha = 0.3f)
            )
            .blur(radius = 1.dp) // Soft, smooth edges
            .background(
                brush = Brush.verticalGradient(
                    colors = colors
                ),
                shape = RoundedCornerShape(50.dp)
            )
    )
}

