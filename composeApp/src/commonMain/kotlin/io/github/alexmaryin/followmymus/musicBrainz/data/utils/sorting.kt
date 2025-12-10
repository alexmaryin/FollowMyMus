package io.github.alexmaryin.followmymus.musicBrainz.data.utils

import io.github.alexmaryin.followmymus.musicBrainz.domain.models.DateCategory
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist

fun Map<Char, List<FavoriteArtist>>.sortAbcOrder() = entries
    .sortedBy { it.key }
    .associate { (letter, list) -> SortKeyType.Abc(letter.toString()) to list }

fun Map<SortKeyType.Country, List<FavoriteArtist>>.sortCountryOrder() = entries
    .sortedBy { it.key.country.country }
    .associate { (country, list) -> country to list }

fun Map<SortKeyType.Type, List<FavoriteArtist>>.sortTypeOrder() = entries
    .sortedBy { it.key.value.ordinal }
    .associate { (type, list) -> type to list }

fun Map<DateCategory, List<FavoriteArtist>>.sortDateCategoryGroups(): Map<SortKeyType.Date, List<FavoriteArtist>> {

    val sorted = entries.sortedWith { (key1, _), (key2, _) ->
        when {
            key1 is DateCategory.Recent && key2 is DateCategory.Recent ->
                key1.type.ordinal.compareTo(key2.type.ordinal)

            key1 is DateCategory.Recent -> -1
            key2 is DateCategory.Recent -> 1

            key1 is DateCategory.ByYear && key2 is DateCategory.ByYear ->
                key2.year.compareTo(key1.year)

            else -> 0
        }
    }.associate { (category, list) -> SortKeyType.Date(category) to list }

    return sorted
}