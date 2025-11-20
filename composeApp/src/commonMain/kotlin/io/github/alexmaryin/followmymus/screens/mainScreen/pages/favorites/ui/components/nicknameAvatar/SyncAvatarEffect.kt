package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun SyncAvatarEffect(shimmerOffset: Float) {
val gradient = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.35f),
            Color.Transparent
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val bandWidth = width * 0.5f
        val x = shimmerOffset * width

        drawRect(
            brush = gradient,
            topLeft = Offset(x - bandWidth, -height * 0.2f),
            size = Size(bandWidth, height * 1.4f),
            alpha = 1f,
            blendMode = BlendMode.Lighten
        )
    }
}