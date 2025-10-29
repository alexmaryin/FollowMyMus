package io.github.alexmaryin.followmymus.sessionManager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionPayload(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String,
)
