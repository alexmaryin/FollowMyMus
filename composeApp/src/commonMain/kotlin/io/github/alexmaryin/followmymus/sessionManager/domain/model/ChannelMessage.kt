package io.github.alexmaryin.followmymus.sessionManager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChannelMessage(
    val message: String
)
