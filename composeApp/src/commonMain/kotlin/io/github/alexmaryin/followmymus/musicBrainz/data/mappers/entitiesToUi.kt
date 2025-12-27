package io.github.alexmaryin.followmymus.musicBrainz.data.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.*
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SecondaryType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.Area
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.LifeSpan
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.MusicTag
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.*
import kotlinx.datetime.LocalDate

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

internal fun List<ResourceEntity>.groupByType() =
    map { Resource(it.type, it.url) }
        .groupBy { it.resourceName }

internal fun List<ReleaseEntity>.groupByCategories() = asSequence()
    .map { it.toRelease() }
    .sortedByDescending { it.firstReleaseDate }
    .groupBy { (listOf(it.primaryType) + it.secondaryTypes).joinToString(" + ") }.toList()
    .sortedWith(compareBy { it.first }).toList()
    .toMap()

internal fun ReleaseEntity.toRelease() = Release(
    id = id,
    title = title,
    disambiguation = disambiguation,
    firstReleaseDate = firstReleaseDate?.let { LocalDate.parse(it) },
    primaryType = primaryType,
    secondaryTypes = if (secondaryTypes.isNullOrBlank()) emptyList() else {
        secondaryTypes.split(",").map { SecondaryType.valueOf(it) }
    },
    previewCoverUrl = previewCoverUrl,
    largeCoverUrl = fullCoverUrl
)

internal fun MediaWithData.toMedia() = Media(
    id = media.id,
    title = media.title,
    disambiguation = media.disambiguation,
    status = media.status,
    country = media.country,
    date = media.date?.let { LocalDate.parse(it) },
    barcode = media.barcode,
    quality = media.quality,
    items = items.map { item -> item.toMediaItem(tracks.filter { it.mediaItemId == item.id }) },
    resources = resources.map { it.toResource() },
    previewCoverUrl = media.previewCoverUrl,
    fullCoverUrl = media.fullCoverUrl
)

internal fun MediaItemEntity.toMediaItem(tracks: List<TrackEntity>) = MediaItem(
    id = id,
    position = position,
    format = format,
    title = title,
    trackCount = trackCount,
    tracks = tracks.map { it.toTrack() }
)

internal fun MediaResourceEntity.toResource() = Resource(
    resourceName = type,
    url = url
)

internal fun TrackEntity.toTrack() = Track(
    id = id,
    position = position,
    title = title,
    lengthMs = lengthMs
)