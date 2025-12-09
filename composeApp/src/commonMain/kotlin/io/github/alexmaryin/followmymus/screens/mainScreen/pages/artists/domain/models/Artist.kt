package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto

data class Artist(
    val id: String,
    val name: String,
    val description: String? = null,
    val details: String,
    val isFavorite: Boolean = false,
    val score: Int? = null,
    val dtoSource: ArtistDto? = null
)