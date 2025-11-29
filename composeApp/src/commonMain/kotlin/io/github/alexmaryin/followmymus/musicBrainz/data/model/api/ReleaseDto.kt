package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ReleaseType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SecondaryType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ReleaseDto(
    val id: String,
    val title: String,
    @SerialName("disambiguation") val disambiguation: String? = null,
    @Serializable(with = ReleaseDateSerializer::class)
    @SerialName("first-release-date") val firstReleaseDate: LocalDate,
    @SerialName("primary-type") val primaryType: ReleaseType = ReleaseType.OTHER,
    @SerialName("secondary-types") val secondaryTypes: List<SecondaryType> = emptyList(),
    val coverImages: List<CoverImageDto> = emptyList()
)

object ReleaseDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("ReleaseDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        val parts = decoder.decodeString().split('-')
        require(parts.isNotEmpty() && parts.size <= 3) {
            "Invalid date format: $decoder. Expected YYYY, YYYY-MM, or YYYY-MM-DD"
        }

        val numericParts = parts.map { text ->
            text.toIntOrNull() ?: throw SerializationException("Invalid numeric value in date: $text")
        }

        return when (numericParts.size) {
            1 -> LocalDate(numericParts[0], 1, 1)
            2 -> LocalDate(numericParts[0], numericParts[1], 1)
            else -> LocalDate(numericParts[0], numericParts[1], numericParts[2])
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }
}
