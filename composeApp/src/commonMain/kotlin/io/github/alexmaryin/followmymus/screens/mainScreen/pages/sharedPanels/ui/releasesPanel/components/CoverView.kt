package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.SubcomposeAsyncImage

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoverView(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceAtLeast(0.8f)
            offset += panChange * scale
        }

        SubcomposeAsyncImage(
            model = url,
            contentDescription = "full cover image",
            loading = {
                Box(Modifier.fillMaxSize()) {
                    LoadingIndicator(Modifier.align(Alignment.Center).size(50.dp))
                }
            },
            contentScale = ContentScale.Inside,
            modifier = Modifier.fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ).transformable(state = state)
        )
    }
}