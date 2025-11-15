package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.CountryISO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val type: ArtistType?,
    val score: Int,
    val name: String,
    @SerialName("sort-name") val sortName: String,
    val country: CountryISO?,
    val area: AreaDto?,
    @SerialName("begin-area") val beginArea: AreaDto?,
    val disambiguation: String?,
    @SerialName("life-span") val lifeSpan: LifeSpanDto?,
    val tags: List<TagDto> = emptyList()
)

