package io.github.alexmaryin.followmymus.screens.mainScreen.pages.releases.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toArtist
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import org.koin.compose.koinInject

@Composable
fun SearchPage() {
    val searchEngine = koinInject<SearchEngine>()

    var search by remember { mutableStateOf("Taylor Swift") }

    var result by remember { mutableStateOf(emptyList<Artist>()) }

    LaunchedEffect(search) {
        val res = searchEngine.searchArtists(search)
        res.forSuccess { result = it.artists.map { it.toArtist(false) } }
        res.forError { println(it) }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(result) { artist ->
            Text(text = artist.toString(), modifier = Modifier.fillMaxWidth().padding(6.dp))
        }
    }
}