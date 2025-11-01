package io.github.alexmaryin.followmymus.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primaryContainer)
    ) {
        val text = "FollowMyMus"
        val charAnimates = remember(text) {
            List(text.length) { Animatable(0f) }
        }

        LaunchedEffect(key1 = text) {
            charAnimates.forEach { animatable ->
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f, // Less damping creates a bouncier effect
                        stiffness = Spring.StiffnessMedium // Controls the speed of the spring
                    )
                )
            }
        }

        Row(Modifier.align(Alignment.Center)) {
            text.forEachIndexed { index, char ->
                val animatable = charAnimates[index]
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle()) { append(char) }
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = animatable.value
                            scaleX = scale
                            scaleY = scale
                            translationY = (1f - animatable.value) * 200f // Start from below and move up
                            alpha = animatable.value // Fade in as it animates
                        }
                )
            }
        }
    }
}