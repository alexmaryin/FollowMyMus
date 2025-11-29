package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.*
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun ArtistDto.toEntity(isFavorite: Boolean, status: SyncStatus) = ArtistEntity(
    id = id,
    type = type,
    name = name,
    sortName = sortName,
    country = country,
    areaId = area?.id,
    beginAreaId = beginArea?.id,
    disambiguation = disambiguation,
    lifeSpan = lifeSpan?.toEntity(),
    isFavorite = isFavorite,
    syncStatus = status,
    createdAt = Clock.System.now()
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

fun ResourceDto.toEntity(artistId: String) = ResourceEntity(
    id = url.id,
    type = type,
    url = url.resource,
    artistId = artistId
)

fun ReleaseDto.toEntity(artistId: String) = ReleaseEntity(
    id = id,
    artistId = artistId,
    title = title,
    disambiguation = disambiguation,
    firstReleaseDate = firstReleaseDate?.toString(),
    primaryType = primaryType,
    secondaryTypes = secondaryTypes.joinToString(",")
)