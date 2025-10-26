package io.github.alexmaryin.followmymus.sessionManager.domain.model

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.Serializable

@Serializable
data class SessionPayload(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String,
    val user: UserInfo?
)
