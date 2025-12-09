package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ArtistType {
    @SerialName("Person") PERSON,
    @SerialName("Group") GROUP,
    @SerialName("Orchestra") ORCHESTRA,
    @SerialName("Choir") CHOIR,
    @SerialName("Character") CHARACTER,
    @SerialName("Other") OTHER
}