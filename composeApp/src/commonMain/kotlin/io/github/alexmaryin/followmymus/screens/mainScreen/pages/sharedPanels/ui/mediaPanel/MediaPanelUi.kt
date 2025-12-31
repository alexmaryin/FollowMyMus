package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.mediaPanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.cancel
import followmymus.composeapp.generated.resources.vinyl_loading
import io.github.alexmaryin.followmymus.core.ui.VinylLoadingIndicator
import io.github.alexmaryin.followmymus.core.ui.toCompactDuration
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.toLocalizedResourceName
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.components.ListHeader
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPanelUi(component: MediaDetails) {
    val state by component.state.subscribeAsState()
    val media by component.media.collectAsStateWithLifecycle(emptyList())
    val gridState = rememberLazyStaggeredGridState()

    var openedDetailsIndex: Int? by remember { mutableStateOf(null) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { VinylLoadingIndicator(Modifier.align(Alignment.Center)) }
    } else if (media.isNotEmpty()) {

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 200.dp),
            verticalItemSpacing = 4.dp,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(4.dp),
            state = gridState
        ) {
            items(media.size, key = { index -> media[index].id }) { index ->
                val currentMedia = media[index]
                Box(
                    modifier = Modifier.clickable(onClick = { openedDetailsIndex = index })
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = currentMedia.previewCoverUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = "cover image for ${currentMedia.title}",
                        placeholder = painterResource(Res.drawable.vinyl_loading),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = currentMedia.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                            },
                            overlineContent = {
                                if (!currentMedia.disambiguation.isNullOrBlank()) {
                                    Text(currentMedia.disambiguation)
                                }
                            },
                            supportingContent = {
                                val extraDetails = buildString {
                                    append(currentMedia.status)
                                    currentMedia.country.toLocalizedResourceName()?.let {
                                        append(" | " + stringResource(it))
                                    }
                                }
                                Text(extraDetails)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            ),
                        )
                    }
                }
            }
        }

        if (openedDetailsIndex != null) Dialog(onDismissRequest = { openedDetailsIndex = null }) {
            openedDetailsIndex?.let { index ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ListItem(
                            headlineContent = { Text(state.releaseName) },
                            trailingContent = {
                                IconButton(
                                    onClick = { openedDetailsIndex = null }
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.cancel),
                                        contentDescription = "close"
                                    )
                                }
                            }
                        )

                        val openedMedia = media[index]
                        val hasSeveral = openedMedia.items.size > 1
                        openedMedia.items.forEach { item ->
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
}