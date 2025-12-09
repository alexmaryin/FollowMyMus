package io.github.alexmaryin.followmymus.musicBrainz.data.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.CoverImageDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ThumbnailDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ImageType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ThumbnailSize
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

fun List<CoverImageDto>.selectCover(selector: CoverImageDto.() -> String): String? {
    if (isEmpty()) return null

    val front = firstOrNull { it.type == ImageType.FRONT }
    if (front != null) return front.selector()

    val back = firstOrNull { it.type == ImageType.BACK }
    if (back != null) return back.selector()

    return firstOrNull()?.selector()
}

fun CoverImageDto.selectPreview(): String {
    return (thumbnails withSize ThumbnailSize.SIZE_250)
        ?: (thumbnails withSize ThumbnailSize.SMALL)
        ?: (thumbnails withSize ThumbnailSize.SIZE_500)
        ?: url
}

infix fun List<ThumbnailDto>.withSize(size: ThumbnailSize): String? =
    firstOrNull { it.size == size }?.url
