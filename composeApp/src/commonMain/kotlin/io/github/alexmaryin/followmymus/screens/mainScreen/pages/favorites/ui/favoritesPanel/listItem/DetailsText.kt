package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.buildDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import kotlinx.coroutines.launch

@Composable
fun DetailsText(artist: FavoriteArtist) {
    var details: String? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    if (details == null) scope.launch {
        details = artist.buildDetails {
            country()
            area()
            lifeSpan()
        }
    }
    details?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }

}