package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import io.github.alexmaryin.followmymus.musicBrainz.data.utils.toDateCategory
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists.ABC
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists.COUNTRY
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists.DATE
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists.NONE
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists.TYPE
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist

/**
 * Factory for the `keySelector` used by `PagingData<FavoriteArtist>.groupedBy`.
 *
 * The sort mode chosen by the user selects which facet of the
 * FavoriteArtist becomes the group key. `NONE` returns a single
 * constant key so the entire list groups under one header.
 */
fun favoriteArtistKeySelector(sortType: SortArtists): (FavoriteArtist) -> SortKeyType = { artist ->
    when (sortType) {
        NONE -> SortKeyType.None
        DATE -> SortKeyType.Date(artist.createdAt.toDateCategory())
        COUNTRY -> SortKeyType.Country(artist.country)
        ABC -> SortKeyType.Abc(artist.sortName.first().uppercaseChar().toString())
        TYPE -> SortKeyType.Type(artist.type)
    }
}
