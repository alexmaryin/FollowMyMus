package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class TagDto(
    val count: Int,
    val name: String
)
