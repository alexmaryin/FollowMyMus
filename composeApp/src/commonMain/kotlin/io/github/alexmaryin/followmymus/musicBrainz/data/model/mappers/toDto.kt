package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.AreaDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.LifeSpanDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.TagDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.AreaType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.Area
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.LifeSpan
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.MusicTag

fun FavoriteArtist.toDto() = ArtistDto(
    id = id,
    type = type,
    score = null,
    name = name,
    sortName = name,
    country = country,
    area = area?.toDto(),
    beginArea = beginArea?.toDto(),
    disambiguation = description,
    lifeSpan = lifeSpan?.toDto(),
    tags = tags.map(MusicTag::toDto),
)

fun Area.toDto() = AreaDto(
    id = "",
    name = name,
    sortName = "",
    type = AreaType.UNKNOWN,
    lifeSpan = null
)

fun LifeSpan.toDto() = LifeSpanDto(
    begin = begin,
    end = end,
    ended = ended
)

fun MusicTag.toDto() = TagDto(count = 1, name = name)
