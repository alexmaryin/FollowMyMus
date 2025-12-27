package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.mediaPanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.vinyl_loading
import io.github.alexmaryin.followmymus.core.ui.VinylLoadingIndicator
import io.github.alexmaryin.followmymus.core.ui.toCompactDuration
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.toLocalizedResourceName
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.formatToDayMonthYear
import io.github.alexmaryin.followmymus.screens.commonUi.SoftCornerBlock
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.components.ListHeader
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPanelUi(component: MediaDetails) {
    val state by component.state.subscribeAsState()
    val media by component.media.collectAsStateWithLifecycle(emptyList())
    val carouselState = rememberCarouselState { media.size }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { VinylLoadingIndicator(Modifier.align(Alignment.Center)) }
    } else {
        HorizontalCenteredHeroCarousel(
            state = carouselState,
            modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colorScheme.surfaceContainerLow),
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(8.dp),
        ) { index ->
            val current = media[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState())
                    .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SoftCornerBlock {
                    ListHeader("${index + 1} / ${media.size}")
                    AsyncImage(
                        model = current.fullCoverUrl,
                        modifier = Modifier.padding(16.dp),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = "cover image for ${current.title}",
                        placeholder = painterResource(Res.drawable.vinyl_loading),
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                text = current.title,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                        },
                        overlineContent = { current.disambiguation?.let { Text(it) } },
                    )
                    val extraDetails = buildString {
                        append(current.status)
                        current.country.toLocalizedResourceName()?.let {
                            append(" | " + stringResource(it))
                        }
                        current.date?.let {
                            append(" | " + it.formatToDayMonthYear())
                        }
                    }
                    ListHeader(extraDetails)
                }

                SoftCornerBlock {
                    val hasSeveral = current.items.size > 1
                    current.items.forEach { item ->
                        ListHeader("${item.format} ${if (hasSeveral) item.position else ""}")
                        item.tracks.forEach { track ->
                            ListItem(
                                leadingContent = { Text("${track.position}.") },
                                headlineContent = { Text(track.title) },
                                overlineContent = {
                                    if (!track.disambiguation.isNullOrBlank()) Text(text = track.disambiguation)
                                },
                                trailingContent = { track.lengthMs?.let { Text(it.toCompactDuration()) } }
                            )
                        }
                    }
                }
            }
        }
    }
}