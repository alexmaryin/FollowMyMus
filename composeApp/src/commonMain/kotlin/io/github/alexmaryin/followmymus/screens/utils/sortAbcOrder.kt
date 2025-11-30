package io.github.alexmaryin.followmymus.screens.utils

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist

fun Map<Char, List<FavoriteArtist>>.sortAbcOrder(): Map<SortKeyType.Abc, List<FavoriteArtist>> =
    toList().sortedBy { it.first }
        .associate { (letter, list) -> SortKeyType.Abc(letter.toString()) to list }