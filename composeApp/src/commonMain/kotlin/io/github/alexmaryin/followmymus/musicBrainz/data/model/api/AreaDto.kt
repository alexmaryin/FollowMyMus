package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AreaDto(
    val id: String,
    val name: String,
    @SerialName("sort-name") val sortName: String,
    val type: String,
    @SerialName("life-span") val lifeSpan: LifeSpanDto?
)
