package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist

@Composable
fun ArtistListItem(
    artist: Artist,
    isOpened: Boolean,
    action: (ArtistsListAction) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { action(ArtistsListAction.OpenReleases(artist)) }
            )
        },
        overlineContent = {
            artist.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        supportingContent = {
            Text(
                text = artist.details,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingContent = {
            ArtistFavoriteIcon(artist.isFavorite) {
                action(ArtistsListAction.ToggleArtistFavorite(artist.id, artist.isFavorite))
            }
        },
        trailingContent = {
            ArtistTrailingIcons(
                score = artist.score,
                name = artist.name,
                isOpened = isOpened
            ) {
                if (isOpened) action(ArtistsListAction.CloseReleases) else
                    action(ArtistsListAction.OpenReleases(artist))
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isOpened) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
            leadingIconColor = if (artist.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
    HorizontalDivider(Modifier.padding(horizontal = 10.dp))
}

@Preview
@Composable
fun ArtistItemPreview() {
    Column {
        ArtistListItem(
            artist = Artist(
                id = "",
                name = "Alkonost",
                description = "Russian folk metal band",
                details = "Russia. Founded in 1995. Folk metal, Doom metal.",
                isFavorite = false,
                score = 100
            ),
            isOpened = false
        ) {}
        ArtistListItem(
            artist = Artist(
                id = "",
                name = "Alkonost",
                description = "Russian folk metal band",
                details = "Russia. Founded in 1995. Folk metal, Doom metal.",
                isFavorite = true,
                score = null
            ),
            isOpened = true
        ) {}
    }
}
