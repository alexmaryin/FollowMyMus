package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.mediaPanel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.vinyl_fallback
import followmymus.composeapp.generated.resources.vinyl_loading
import followmymus.composeapp.generated.resources.vinyl_placeholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPanelUi(component: MediaDetails) {
    val state by component.state.subscribeAsState()
    val media by component.media.collectAsStateWithLifecycle(emptyList())
    val carouselState = rememberCarouselState { media.size }

    HorizontalCenteredHeroCarousel(
        state = carouselState,
        modifier = Modifier.fillMaxWidth(),
        itemSpacing = 8.dp
    ) { index ->
        val current = media[index]
        Column(
            modifier = Modifier.verticalScroll(state = rememberScrollState())
        ) {
            AsyncImage(
                model = current.fullCoverUrl ?: Res.drawable.vinyl_placeholder,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
                contentDescription = "cover image for ${current.title}",
                fallback = painterResource(Res.drawable.vinyl_placeholder),
                placeholder = painterResource(Res.drawable.vinyl_loading),
                error = painterResource(Res.drawable.vinyl_fallback)
            )
            Text(current.title)
            Text(current.disambiguation ?: "")
            current.items.forEach { item ->
                Text("${item.format} ${item.position}")
                item.tracks.forEach { track ->
                    Text("${track.position}. ${track.title} - ${track.lengthMs} ms")
                }
            }
        }
    }
}