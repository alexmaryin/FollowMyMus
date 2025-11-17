package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.ArtistWithRelations
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist

fun ArtistWithRelations.toArtist() = Artist(
    id = artist.id,
    name = artist.name,
    description = artist.disambiguation,
    details = buildString {
        artist.country?.let { append(it) }
        artist.lifeSpan?.begin?.let { append(it) }
        artist.lifeSpan?.end?.let { append(it) }
        if (tags.isNotEmpty())
            append(tags.joinToString(separator = ", ", postfix = ".") { it.name })
    },
    isFavorite = artist.isFavorite,
    score = 100
)