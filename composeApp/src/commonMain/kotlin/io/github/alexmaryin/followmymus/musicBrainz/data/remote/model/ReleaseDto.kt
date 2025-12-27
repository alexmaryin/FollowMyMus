package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.LocalDateSerializer
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SecondaryType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseDto(
    val id: String,
    val title: String,
    @SerialName("disambiguation") val disambiguation: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("first-release-date") val firstReleaseDate: LocalDate?,
    @SerialName("primary-type") val primaryType: ReleaseType = ReleaseType.OTHER,
    @SerialName("secondary-types") val secondaryTypes: List<SecondaryType> = emptyList(),
    val coverImages: List<CoverImageDto> = emptyList()
)

