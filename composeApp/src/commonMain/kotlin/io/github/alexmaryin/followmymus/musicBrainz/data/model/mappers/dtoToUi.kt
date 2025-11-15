package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.character_lifespan_start
import followmymus.composeapp.generated.resources.group_lifespan_end
import followmymus.composeapp.generated.resources.group_lifespan_start
import followmymus.composeapp.generated.resources.person_lifespan_end
import followmymus.composeapp.generated.resources.person_lifespan_start
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.toLocalizedName
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.parseLifeSpanDate
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import org.jetbrains.compose.resources.getString

suspend fun ArtistDto.toArtist(isFavorite: Boolean) = Artist(
    id = id,
    name = name,
    description = disambiguation,
    details = buildString {
        country?.toLocalizedName()?.let { append("$it. ") }
        lifeSpan?.begin?.parseLifeSpanDate()?.let {
            when (type) {
                ArtistType.PERSON -> append(getString(Res.string.person_lifespan_start, it))
                ArtistType.CHARACTER -> append(getString(Res.string.character_lifespan_start, it))
                else -> append(getString(Res.string.group_lifespan_start, it))
            }
        }
        lifeSpan?.end?.parseLifeSpanDate()?.let {
            when (type) {
                ArtistType.PERSON -> append(getString(Res.string.person_lifespan_end, it))
                else -> append(getString(Res.string.group_lifespan_end, it))
            }
        }
        if (tags.isNotEmpty()) append(tags.joinToString(separator = ",", postfix = ".") { it.name })
    },
    isFavorite = isFavorite,
    score = score
)