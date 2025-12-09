package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AreaType {
    @SerialName("Country") COUNTRY,
    @SerialName("City") CITY,
    @SerialName("Municipality") MUNICIPALITY,
    @SerialName("District") DISTRICT,
    @SerialName("Subdivision") SUBDIVISION,
    @SerialName("Island") ISLAND,
    @SerialName("County") COUNTY,
    UNKNOWN
}
