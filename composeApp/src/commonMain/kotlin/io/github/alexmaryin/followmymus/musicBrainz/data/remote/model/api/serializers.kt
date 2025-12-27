package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.CoverImageDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ThumbnailDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ImageType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ThumbnailSize
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object LocalDateSerializer : KSerializer<LocalDate?> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate? {
        val decoded = decoder.decodeString()
        if (decoded.isBlank()) return null
        val parts = decoded.split('-')
        if(parts.isEmpty() && parts.size > 3) return null

        val numericParts = parts.map { text ->
            text.toIntOrNull() ?: throw SerializationException("Invalid numeric value in date: $text")
        }

        return when (numericParts.size) {
            1 -> LocalDate(numericParts[0], 1, 1)
            2 -> LocalDate(numericParts[0], numericParts[1], 1)
            else -> LocalDate(numericParts[0], numericParts[1], numericParts[2])
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDate?) {
        encoder.encodeString(value?.toString() ?: "")
    }
}

/**
 * Custom serializer that handles the transformation between API format
 * and simplified internal format
 */
object ImageSerializer : KSerializer<CoverImageDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CoverImage")

    override fun deserialize(decoder: Decoder): CoverImageDto {
        require(decoder is JsonDecoder)
        val jsonObject = decoder.decodeJsonElement().jsonObject

        // Extract full-size image URL
        val url = jsonObject["image"]?.jsonPrimitive?.content
            ?: throw SerializationException("Missing required field 'image'")

        // Determine image type from boolean flags
        val isFront = jsonObject["front"]?.jsonPrimitive?.boolean ?: false
        val isBack = jsonObject["back"]?.jsonPrimitive?.boolean ?: false

        val type = when {
            isFront -> ImageType.FRONT
            isBack -> ImageType.BACK
            else -> ImageType.OTHER
        }

        // Parse thumbnails object into list.
        // Note: Both deprecated (small/large) and numeric keys are included
        // if present in the API response. You may want to deduplicate URLs.
        val thumbnails = jsonObject["thumbnails"]?.jsonObject?.entries?.mapNotNull { (key, value) ->
            val size = ThumbnailSize.Companion.fromJsonKey(key)
            val thumbUrl = value.jsonPrimitive.content
            size?.let { ThumbnailDto(it, thumbUrl) }
        } ?: emptyList()

        return CoverImageDto(type, url, thumbnails)
    }

    override fun serialize(encoder: Encoder, value: CoverImageDto) {
        require(encoder is JsonEncoder)
        val json = buildJsonObject {
            put("type", value.type.name)
            put("url", value.url)
            putJsonArray("thumbnails") {
                value.thumbnails.forEach { thumbnail ->
                    addJsonObject {
                        put("size", thumbnail.size.jsonKey)
                        put("url", thumbnail.url)
                    }
                }
            }
        }
        encoder.encodeJsonElement(json)
    }
}

