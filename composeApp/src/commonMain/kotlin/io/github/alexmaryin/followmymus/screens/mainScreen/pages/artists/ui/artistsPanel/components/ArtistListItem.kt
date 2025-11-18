package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
            val rotation by animateFloatAsState(
                targetValue = if (artist.isFavorite) 360f else 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "rotation"
            )

            AnimatedContent(
                targetState = artist.isFavorite,
                transitionSpec = {
                    (slideInVertically() + fadeIn() togetherWith slideOutVertically() + fadeOut()).using(
                        SizeTransform(clip = false)
                    )
                },
                label = "icon_animation"
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.clickable { action(ArtistsListAction.ToggleArtistFavorite(artist)) }
                        .animateContentSize()
                        .rotate(rotation)
                )
            }
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