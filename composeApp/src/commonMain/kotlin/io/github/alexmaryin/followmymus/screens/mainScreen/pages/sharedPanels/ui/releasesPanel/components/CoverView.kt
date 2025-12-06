package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.SubcomposeAsyncImage
import kotlin.math.min

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoverView(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {

        var gestureScale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        val state = rememberTransformableState { zoom, pan, _ ->
            gestureScale = (gestureScale * zoom).coerceIn(0.8f, 5f)
            offset += pan * gestureScale
        }

        // Load image first and get dimensions
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            loading = {
                Box(Modifier.fillMaxSize()) {
                    LoadingIndicator(Modifier.align(Alignment.Center).size(100.dp))
                }
            },
            success = { imageState ->
                val painter = imageState.painter
                val intrinsicSize = painter.intrinsicSize

                // If Coil didn't return size → fallback
                val imageWidth = intrinsicSize.width.takeIf { it > 0 } ?: 1f
                val imageHeight = intrinsicSize.height.takeIf { it > 0 } ?: 1f

                BoxWithConstraints(
                    Modifier.fillMaxSize()
                ) {

                    val maxW = constraints.maxWidth.toFloat()
                    val maxH = constraints.maxHeight.toFloat()

                    // compute initial "fit into window" scale only once
                    val baseScale = remember(maxW, maxH) {
                        val scaleX = maxW / imageWidth
                        val scaleY = maxH / imageHeight
                        min(scaleX, scaleY)
                    }

                    val totalScale = baseScale * gestureScale

                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                            .graphicsLayer(
                                scaleX = totalScale,
                                scaleY = totalScale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state)
                    )
                }
            }
        )
    }
}
