package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
data class ResourceDto(
    @SerialName("type") val type: String,

    @Serializable(with = UrlSerializer::class)
    @SerialName("url") val url: String
)

// Custom serializer to extract 'resource' from nested object
object UrlSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UrlAsString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        // Extract the nested "resource" field as string
        return element.jsonObject["resource"]?.jsonPrimitive?.content ?: ""
    }

    override fun serialize(encoder: Encoder, value: String) {
        require(encoder is JsonEncoder)
        // For serialization, wrap the string back into the nested structure
        val json = JsonObject(mapOf("resource" to JsonPrimitive(value)))
        encoder.encodeJsonElement(json)
    }
}