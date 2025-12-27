package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.LocalDateSerializer
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseStatus
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaDto(
    val id: String,
    val title: String,
    val disambiguation: String? = null,
    val status: ReleaseStatus = ReleaseStatus.Official,
    val country: CountryISO = CountryISO("unknown"),
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate? = null,
    val barcode: String? = null,
    val quality: String? = null,
    @SerialName("media") val items: List<MediaItemDto> = emptyList(),
    @SerialName("relations") val resources: List<ResourceDto> = emptyList(),
    val coverImages: List<CoverImageDto> = emptyList()
)