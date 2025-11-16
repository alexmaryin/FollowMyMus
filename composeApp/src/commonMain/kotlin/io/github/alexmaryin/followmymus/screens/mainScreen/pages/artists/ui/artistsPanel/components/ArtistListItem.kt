package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.add
import followmymus.composeapp.generated.resources.favorite
import followmymus.composeapp.generated.resources.forward
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ArtistListItem(
    artist: Artist,
    action: (ArtistsListAction) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
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
            val iconRes = if (artist.isFavorite) Res.drawable.favorite else Res.drawable.add
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .clickable { action(ArtistsListAction.ToggleArtistFavorite(artist)) }
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artist.score.toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
                Icon(
                    painter = painterResource(Res.drawable.forward),
                    contentDescription = "Open releases for ${artist.name}",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { action(ArtistsListAction.SelectArtist(artist.id)) },
                )
            }
        },
        colors = ListItemDefaults.colors(
            leadingIconColor = if (artist.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
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
            )
        ) {}
        ArtistListItem(
            artist = Artist(
                id = "",
                name = "Alkonost",
                description = "Russian folk metal band",
                details = "Russia. Founded in 1995. Folk metal, Doom metal.",
                isFavorite = true,
                score = 100
            )
        ) {}
    }
}