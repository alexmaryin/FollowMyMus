package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.vinyl_fallback
import followmymus.composeapp.generated.resources.vinyl_placeholder
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.formatToDayMonthYear
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ResourcesFlow
import org.jetbrains.compose.resources.painterResource

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
            ResourcesFlow(artistDetails.resources.groupBy { it.resourceName })

            LazyColumn(state = releasesState) {
                items(artistDetails.releases, key = { it.id }) { release ->
                    ListItem(
                        headlineContent = { Text(text = release.title) },
                        overlineContent = { Text(text = release.disambiguation ?: "") },
                        supportingContent = { Text(text = release.firstReleaseDate.formatToDayMonthYear()) },
                        trailingContent = {
                            if (release.coverUrl != null) {
                                AsyncImage(
                                    model = release.coverUrl,
                                    modifier = Modifier.size(150.dp),
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