package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.*
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ImageType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ThumbnailSize
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.ArtistReleases
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource

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

fun ArtistDto.toArtistReleases() = ArtistReleases(
    id = id,
    name = name,
    resources = resources.map(ResourceDto::toUiResource).groupBy { it.resourceName },
    releases = releases
        .map(ReleaseDto::toRelease)
        .sortedByDescending { it.firstReleaseDate }
        .groupBy { it.primaryType }.toList()
        .sortedWith(compareBy { it.first.ordinal })
        .toMap()
)

fun ResourceDto.toUiResource() = Resource(
    resourceName = type,
    url = url
)

fun ReleaseDto.toRelease() = Release(
    id = id,
    title = title,
    disambiguation = disambiguation,
    firstReleaseDate = firstReleaseDate,
    primaryType = primaryType,
    secondaryTypes = secondaryTypes,
    previewCoverUrl = coverImages.selectCover { selectPreview() },
    largeCoverUrl = coverImages.selectCover { url }
)

internal fun List<CoverImageDto>.selectCover(selector: CoverImageDto.() -> String): String? {
    if (isEmpty()) return null

    val front = firstOrNull { it.type == ImageType.FRONT }
    if (front != null) return front.selector()

    val back = firstOrNull { it.type == ImageType.BACK }
    if (back != null) return back.selector()

    return firstOrNull()?.selector()
}

internal fun CoverImageDto.selectPreview(): String {
    return (thumbnails withSize ThumbnailSize.SIZE_500)
        ?: (thumbnails withSize ThumbnailSize.SIZE_250)
        ?: (thumbnails withSize ThumbnailSize.SMALL)
        ?: url
}

internal fun CoverImageDto.selectFull(): String {
    return (thumbnails withSize ThumbnailSize.SIZE_1200)
        ?: (thumbnails withSize ThumbnailSize.LARGE)
        ?: url
}

internal infix fun List<ThumbnailDto>.withSize(size: ThumbnailSize): String? =
    firstOrNull { it.size == size }?.url
