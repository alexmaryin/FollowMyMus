package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.CoverImageDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ReleaseDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ResourceDto
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
    resources = resources.map(ResourceDto::toUiResource),
    releases = releases.map(ReleaseDto::toRelease)
)

fun ResourceDto.toUiResource() = Resource(
    resourceName = type,
    url = url
)

fun ReleaseDto.toRelease() = Release(
    id = id,
    title = title,
    disambiguation = disambiguation,
    firstReleaseDate = firstReleaseDate.toString(),     // TODO localized date
    primaryType = primaryType,
    secondaryTypes = secondaryTypes,
    coverUrl = coverImages.selectCover()
)

fun List<CoverImageDto>.selectCover(): String? {
    if (isEmpty()) return null

    val front = firstOrNull { it.type == ImageType.FRONT }
    if (front != null) return front.thumbnails.firstOrNull { it.size == ThumbnailSize.SIZE_250 }?.url
        ?: front.thumbnails.firstOrNull { it.size == ThumbnailSize.SIZE_500 }?.url ?: front.url

    val back = firstOrNull { it.type == ImageType.BACK }
    if (back != null) return back.thumbnails.firstOrNull { it.size == ThumbnailSize.SIZE_250 }?.url
        ?: back.thumbnails.firstOrNull { it.size == ThumbnailSize.SIZE_500 }?.url ?: back.url

    return firstOrNull()?.thumbnails?.firstOrNull { it.size == ThumbnailSize.SIZE_250  }?.url
        ?: firstOrNull()?.thumbnails?.firstOrNull { it.size == ThumbnailSize.SIZE_500 }?.url
        ?:firstOrNull()?.url
}
