package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist

@Composable
fun FavoriteListItem(
    artist: FavoriteArtist,
    isSelected: Boolean,
    onAction: (FavoritesListAction) -> Unit
) {
    val hasDetails = remember {
        listOf(artist.country, artist.area, artist.beginArea, artist.lifeSpan).any { it != null } ||
                artist.tags.isNotEmpty()
    }
    var detailsVisible by remember { mutableStateOf(false) }

    ListItem(
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SyncStatusIcon(artist.syncStatus)
                DetailsOpenIcon(hasDetails) { detailsVisible = it }
            }
        },
        headlineContent = artistNameLabel(artist.name),
        overlineContent = { artistDescription(artist.description) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionsIcon(artist, onAction)
                OpenReleasesIcon { onAction(FavoritesListAction.SelectArtist(artist.id, artist.name)) }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified
        )
    )
    AnimatedVisibility(visible = detailsVisible) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp)
        ) {
            DetailsText(artist)
            TagsFlow(artist.tags)
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 10.dp))
}
