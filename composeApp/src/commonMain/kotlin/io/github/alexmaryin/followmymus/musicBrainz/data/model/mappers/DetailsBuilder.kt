package io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers

import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.toLocalizedResourceName
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.parseLifeSpanDate
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

class DetailsBuilder(
    private val artist: ArtistDto,
    private val separator: String
) {
    private val content = mutableListOf<String>()
    fun build(): String = content.joinToString(separator = separator, postfix = ".")

    suspend fun country() {
        artist.country?.toLocalizedResourceName()?.let {
            content += getString(it)
        }
    }

    suspend fun area(includeBegin: Boolean = true) {
        artist.area?.let {
            content -= it.name
            var area = it.name
            if (includeBegin && artist.beginArea != null) {
                area += " " + getString(Res.string.begin_area_text, artist.beginArea.name)
            }
            if (area.isNotBlank()) content += area
        }
    }

    suspend fun lifeSpan() {
        artist.lifeSpan?.begin?.parseLifeSpanDate()?.let {
            content += when (artist.type) {
                ArtistType.PERSON ->
                    getString(Res.string.person_lifespan_start, it)

                ArtistType.CHARACTER ->
                    getString(Res.string.character_lifespan_start, it)

                else ->
                    getString(Res.string.group_lifespan_start, it)
            }
        }
        artist.lifeSpan?.end?.parseLifeSpanDate()?.let {
            content += when (artist.type) {
                ArtistType.PERSON ->
                    getString(Res.string.person_lifespan_end, it)

                else ->
                    getString(Res.string.group_lifespan_end, it)
            }
        }
    }

    fun tags() {
        if (artist.tags.isNotEmpty())
            content += artist.tags.joinToString(separator = ", ") { it.name.capitalizeTag() }
    }

    private fun String.capitalizeTag() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}

suspend fun ArtistDto.buildDetails(
    separator: String = ". ",
    builder: suspend DetailsBuilder.() -> Unit
): String {
    return DetailsBuilder(this, separator).apply {
        withContext(Dispatchers.IO) { launch { builder() } }
    }.build()
}

suspend fun FavoriteArtist.buildDetails(
    separator: String = ". ",
    builder: suspend DetailsBuilder.() -> Unit
) = toDto().buildDetails(separator, builder)
