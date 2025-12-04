package io.github.alexmaryin.followmymus.core.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.PositionalThreshold
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.vinyl_placeholder
import org.jetbrains.compose.resources.painterResource

@Composable
fun RefreshVinylIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.pullToRefresh(
            state = state,
            isRefreshing = isRefreshing,
            threshold = PositionalThreshold,
            onRefresh = {}
        ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = isRefreshing,
            animationSpec = tween(),
            modifier = Modifier.align(Alignment.Center)
        ) { refreshing ->
            if (refreshing) {
                VinylLoadingIndicator(Modifier.size(100.dp))
            } else {
                val distanceFraction = { state.distanceFraction.coerceIn(0f, 1f) }
                Image(
                    painter = painterResource(Res.drawable.vinyl_placeholder),
                    alpha = 0.8f,
                    contentDescription = null,
                    modifier = modifier
                        .size(100.dp)
                        .graphicsLayer {
                            val progress = distanceFraction()
                            alpha = progress
                            scaleX = progress
                            scaleY = progress
                            rotationZ = progress * 360
                        }
                )
            }
        }

    }
}