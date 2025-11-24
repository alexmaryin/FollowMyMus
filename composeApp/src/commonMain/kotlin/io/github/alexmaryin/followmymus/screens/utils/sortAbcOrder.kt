package io.github.alexmaryin.followmymus.screens.utils

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist

fun Map<Char, List<FavoriteArtist>>.sortAbcOrder(): Map<String, List<FavoriteArtist>> =
    toList().sortedBy { it.first }
        .associate { (letter, list) -> letter.toString() to list }