package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.vinyl_fallback
import followmymus.composeapp.generated.resources.vinyl_placeholder
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.formatToDayMonthYear
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ResourcesFlow
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReleasesPanelUi(component: ReleasesList) {
    val artistDetails by component.details.collectAsStateWithLifecycle()
    val releasesState = rememberLazyListState()
    val state by component.state.subscribeAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else {
        Column {
            ResourcesFlow(artistDetails.resources)

            LazyColumn(state = releasesState) {
                artistDetails.releases.forEach { (type, releases) ->
                    stickyHeader {
                        Text(
                            text = type.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        )
                    }
                    items(releases, key = { it.id }) { release ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = release.title,
                                    modifier = Modifier.clickable {
                                        component(ReleasesListAction.SelectRelease(release.id))
                                    }
                                )
                            },
                            overlineContent = { Text(text = release.disambiguation ?: "") },
                            supportingContent = { Text(text = release.firstReleaseDate.formatToDayMonthYear()) },
                            trailingContent = {
                                if (release.previewCoverUrl != null) {
                                    AsyncImage(
                                        model = release.previewCoverUrl,
                                        modifier = Modifier.size(150.dp)
                                            .clickable {
                                                release.largeCoverUrl?.let {
                                                    component(ReleasesListAction.OpenFullCover(it))
                                                }
                                            },
                                        contentDescription = "cover image for ${release.title}",
                                        fallback = painterResource(Res.drawable.vinyl_fallback),
                                        placeholder = painterResource(Res.drawable.vinyl_placeholder),
                                        error = painterResource(Res.drawable.vinyl_fallback)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(Res.drawable.vinyl_placeholder),
                                        contentDescription = "no cover for ${release.title}",
                                        modifier = Modifier.size(150.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    state.openedCover?.let {
        Dialog(
            onDismissRequest = { component(ReleasesListAction.CloseFullCover) }
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val state = rememberTransformableState { zoomChange, panChange, _ ->
                scale *= zoomChange
                offset += panChange

            }

            SubcomposeAsyncImage(
                model = it,
                contentDescription = "full cover image",
                loading = { CircularProgressIndicator(Modifier.size(100.dp)) },
                contentScale = ContentScale.Inside,
                modifier = Modifier.padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ).transformable(state = state)
            )
        }
    }
}
