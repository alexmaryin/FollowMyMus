package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ResourceDto(
    val type: String,
    val url: UrlDto
)

@Serializable
data class UrlDto(
    val id: String,
    val resource: String
)