package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ArtistsList(
    private val context: ComponentContext
) : ComponentContext by context, KoinComponent {

    private val repository by inject<ArtistsRepository>()
}