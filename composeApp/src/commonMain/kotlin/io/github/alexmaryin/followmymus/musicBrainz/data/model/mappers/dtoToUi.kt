package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist

suspend fun ArtistDto.toArtist(isFavorite: Boolean) = Artist(
    id = id,
    name = name,
    description = disambiguation,
    details = buildDetails {
        country()
        area()
        lifeSpan()
        tags()
    },
    isFavorite = isFavorite,
    score = score,
    dtoSource = this
)
