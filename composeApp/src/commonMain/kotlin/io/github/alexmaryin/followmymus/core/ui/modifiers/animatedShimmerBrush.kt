package io.github.alexmaryin.followmymus.core.ui.modifiers

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize

fun Modifier.animatedShimmerBrush(
    showShimmer: Boolean = true,
    shape: Shape = RectangleShape,
    colors: List<Color>? = null
): Modifier = composed {
    if (showShimmer) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val transition = rememberInfiniteTransition(label = "shimmerTransition")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1000)
            ),
            label = "shimmerStartOffsetX"
        )
        val brushColors = colors ?: listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surfaceVariant,
                )
        background(
            brush = Brush.linearGradient(
                colors =  brushColors,
                start = Offset(startOffsetX, 0f),
                end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
            ),
            shape = shape
        )
            .onGloballyPositioned {
                size = it.size
            }
    } else {
        this
    }
}

@Preview
@Composable
fun ShimmerPreview() {
    Surface {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.animatedShimmerBrush(
                true,
                ButtonDefaults.shape
            )
        ) {
            Text("Shimmer")
        }
    }
}