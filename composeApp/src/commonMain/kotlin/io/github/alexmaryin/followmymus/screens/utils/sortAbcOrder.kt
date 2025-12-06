package io.github.alexmaryin.followmymus.screens.utils

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist

fun Map<Char, List<FavoriteArtist>>.sortAbcOrder(): Map<SortKeyType.Abc, List<FavoriteArtist>> =
    entries.sortedBy { it.key }
        .associate { (letter, list) -> SortKeyType.Abc(letter.toString()) to list }

fun Map<SortKeyType.Country, List<FavoriteArtist>>.sortCountryOrder(): Map<SortKeyType.Country, List<FavoriteArtist>> =
    entries.sortedBy { it.key.country.country }
        .associate { (country, list) -> country to list }