package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ReleaseType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SecondaryType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

@Serializable
data class ReleaseDto(
    val id: String,
    val title: String,
    @SerialName("disambiguation") val disambiguation: String? = null,
    @SerialName("first-release-date")
    @Serializable(with = DateStringToInstantSerializer::class)
    val firstReleaseDate: Instant,
    @SerialName("primary-type") val primaryType: ReleaseType = ReleaseType.OTHER,
    @SerialName("secondary-types") val secondaryTypes: List<SecondaryType> = emptyList(),
    val coverImages: List<CoverImageDto> = emptyList()
)

object DateStringToInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DateStringToInstant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        val dateString = decoder.decodeString()
        // Parse YYYY-MM-DD as start of day UTC
        return Instant.parse("${dateString}T00:00:00Z")
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        val dateString = value.toString().substringBefore('T')
        encoder.encodeString(dateString)
    }
}
