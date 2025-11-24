package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.ArtistWithRelations
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.Area
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.LifeSpan
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.MusicTag

fun ArtistWithRelations.toFavoriteArtist() = FavoriteArtist(
    id = artist.id,
    type = artist.type ?: ArtistType.OTHER,
    name = artist.name,
    sortName = artist.sortName,
    description = artist.disambiguation,
    country = artist.country,
    area = area?.let { Area(it.name) },
    beginArea = beginArea?.let { Area(it.name) },
    lifeSpan = LifeSpan(artist.lifeSpan?.begin, artist.lifeSpan?.end, artist.lifeSpan?.ended),
    tags = tags.map { tagEntity -> MusicTag(name = tagEntity.name) },
    syncStatus = artist.syncStatus,
    createdAt = artist.createdAt
)