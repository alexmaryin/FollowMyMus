package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.AreaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AreaDto(
    val id: String,
    val name: String,
    @SerialName("sort-name") val sortName: String,
    val type: AreaType = AreaType.UNKNOWN,
    @SerialName("life-span") val lifeSpan: LifeSpanDto?
)
