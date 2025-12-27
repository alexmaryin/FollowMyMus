package io.github.alexmaryin.followmymus.musicBrainz.data.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.*
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.*
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
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

fun MediaDto.toEntity(releaseId: String) = MediaEntity(
    id = id,
    releaseId = releaseId,
    title = title,
    disambiguation = disambiguation,
    status = status,
    country = country,
    date = date?.toString(),
    barcode = barcode,
    quality = quality,
)

fun MediaItemDto.toEntity(mediaId: String) = MediaItemEntity(
    id = id,
    mediaId = mediaId,
    position = position,
    format = format,
    title = title,
    trackCount = trackCount,
)

fun TrackDto.toEntity(mediaId: String, mediaItemId: String) = TrackEntity(
    id = id,
    mediaId = mediaId,
    mediaItemId = mediaItemId,
    position = position,
    title = recording.title,
    lengthMs = recording.length,
    disambiguation = recording.disambiguation
)

fun ResourceDto.toMediaResourceEntity(mediaId: String) = MediaResourceEntity(
    id = url.id,
    type = type,
    url = url.resource,
    mediaId = mediaId,
)