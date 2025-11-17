package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.AreaDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.LifeSpanDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.TagDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.AreaEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.ArtistEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.LifeSpanEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.TagEntity

fun ArtistDto.toEntity(isFavorite: Boolean) = ArtistEntity(
    id = id,
    type = type,
    name = name,
    sortName = sortName,
    country = country,
    areaId = area?.id,
    beginAreaId = beginArea?.id,
    disambiguation = disambiguation,
    lifeSpan = lifeSpan?.toEntity(),
    isFavorite = isFavorite
)

fun LifeSpanDto.toEntity() = LifeSpanEntity(
    begin = begin,
    end = end,
    ended = ended
)

fun TagDto.toEntity(artistId: String) = TagEntity(
    artistId = artistId,
    count = count,
    name = name
)

fun AreaDto.toEntity() = AreaEntity(
    id = id,
    name = name,
    sortName = sortName,
    lifeSpan = lifeSpan?.toEntity()
)