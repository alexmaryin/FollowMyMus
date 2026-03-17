package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.toDateCategory
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists.*
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist

/**
 * Represents a grouped section header for Paging 3
 */
sealed class GroupedFavoriteItem {
    data class Header(val key: SortKeyType, val uniqueId: String) : GroupedFavoriteItem()
    data class Artist(val artist: FavoriteArtist) : GroupedFavoriteItem()
}

/**
 * Determines if a header should be inserted based on grouping type
 */
private fun getHeaderKeyForItem(
    after: GroupedFavoriteItem.Artist?,
    before: GroupedFavoriteItem.Artist?,
    sortType: SortArtists
): SortKeyType? {
    val afterArtist = after?.artist ?: return null

    // Always show header for first item
    if (before == null) {
        return createHeaderKeyForArtist(afterArtist, sortType)
    }

    val beforeArtist = before.artist

    return when (sortType) {
        NONE -> null
        DATE -> {
            val afterCategory = afterArtist.createdAt.toDateCategory()
            val beforeCategory = beforeArtist.createdAt.toDateCategory()
            if (afterCategory != beforeCategory) {
                SortKeyType.Date(afterCategory)
            } else null
        }

        COUNTRY -> {
            if (afterArtist.country != beforeArtist.country) {
                SortKeyType.Country(afterArtist.country)
            } else null
        }

        ABC -> {
            val afterLetter = afterArtist.sortName.first().uppercaseChar().toString()
            val beforeLetter = beforeArtist.sortName.first().uppercaseChar().toString()
            if (afterLetter != beforeLetter) {
                SortKeyType.Abc(afterLetter)
            } else null
        }

        TYPE -> {
            if (afterArtist.type != beforeArtist.type) {
                SortKeyType.Type(afterArtist.type)
            } else null
        }
    }
}

/**
 * Transforms flat PagingData<FavoriteArtist> into grouped items with headers
 * for use with LazyColumn sticky headers
 */
fun PagingData<FavoriteArtist>.groupedBy(sortType: SortArtists): PagingData<GroupedFavoriteItem> {
    var headerCounter = 0
    return this.map { artist -> GroupedFavoriteItem.Artist(artist) }
        .insertSeparators { before, after ->
            // Determine if we need a header between before and after
            val headerKey = getHeaderKeyForItem(after, before, sortType)
            headerKey?.let { 
                // Generate a unique ID for each header to avoid duplicate key crashes
                GroupedFavoriteItem.Header(it, uniqueId = "header_${headerKey.key}_${headerCounter++}")
            }
        }
}

/**
 * Creates header key for the first item in a group
 */
private fun createHeaderKeyForArtist(
    artist: FavoriteArtist,
    sortType: SortArtists
): SortKeyType {
    return when (sortType) {
        NONE -> SortKeyType.None
        DATE -> {
            SortKeyType.Date(artist.createdAt.toDateCategory())
        }

        COUNTRY -> {
            SortKeyType.Country(artist.country)
        }

        ABC -> {
            SortKeyType.Abc(artist.sortName.first().uppercaseChar().toString())
        }

        TYPE -> {
            SortKeyType.Type(artist.type)
        }
    }
}