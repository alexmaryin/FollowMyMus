package io.github.alexmaryin.followmymus.screens.utils

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.SortKeyType
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