package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistListItem
import kotlinx.coroutines.launch
import kotlin.random.Random

@Preview
@Composable
fun SearchPage() {
    val items = remember {
        List(300) {
            Artist(
                id = "",
                name = "Alkonost",
                description = "Russian folk metal band",
                details = "Russia. Founded in 1995. Folk metal, Doom metal.",
                isFavorite = Random.nextBoolean(),
                score = it
            )
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState
        ) {
            items(items) { item ->
                ArtistListItem(artist = item, false) {}
            }
        }

        FloatingActionButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("up")
        }
    }
}