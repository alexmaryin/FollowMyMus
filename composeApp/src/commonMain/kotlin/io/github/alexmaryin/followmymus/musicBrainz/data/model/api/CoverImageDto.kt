package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ImageType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ThumbnailSize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Main image data class with custom serialization.
 * Deserializes from full API format, serializes to simplified format.
 */
@Serializable(with = ImageSerializer::class)
data class CoverImageDto(
    val type: ImageType,
    val url: String,
    val thumbnails: List<ThumbnailDto>
)

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
            val size = ThumbnailSize.fromJsonKey(key)
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
