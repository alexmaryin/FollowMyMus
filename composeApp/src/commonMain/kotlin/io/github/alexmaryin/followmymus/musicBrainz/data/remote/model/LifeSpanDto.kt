package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LifeSpanDto(
    val begin: String?,
    val end: String?,
    val ended: Boolean?
)
