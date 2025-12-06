package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.vinyl_fallback
import followmymus.composeapp.generated.resources.vinyl_loading
import followmymus.composeapp.generated.resources.vinyl_placeholder
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.formatToDayMonthYear
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import org.jetbrains.compose.resources.painterResource

@Composable
fun ReleaseListItem(release: Release, actionHandler: (ReleasesListAction) -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = release.title,
                modifier = Modifier.clickable {
                    actionHandler(ReleasesListAction.SelectRelease(release.id))
                }
            )
        },
        overlineContent = { Text(text = release.disambiguation ?: "") },
        supportingContent = { Text(text = release.firstReleaseDate?.formatToDayMonthYear() ?: "") },
        trailingContent = {
            if (release.previewCoverUrl != null) {
                AsyncImage(
                    model = release.previewCoverUrl,
                    modifier = Modifier.size(150.dp)
                        .clickable {
                            release.largeCoverUrl?.let {
                                println("Open large cover $it")
                                actionHandler(ReleasesListAction.OpenFullCover(it))
                            }
                        },
                    contentDescription = "cover image for ${release.title}",
                    fallback = painterResource(Res.drawable.vinyl_placeholder),
                    placeholder = painterResource(Res.drawable.vinyl_loading),
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