package io.github.alexmaryin.followmymus.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.vinyl_placeholder
import io.github.alexmaryin.followmymus.core.ui.modifiers.animatedShimmerBrush
import org.jetbrains.compose.resources.painterResource

@Preview
@Composable
fun VinylLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Image(
        painter = painterResource(Res.drawable.vinyl_placeholder),
        alpha = 0.8f,
        contentDescription = null,
        modifier = modifier
            .rotate(angle)
            .animatedShimmerBrush(true, CircleShape)
    )
}