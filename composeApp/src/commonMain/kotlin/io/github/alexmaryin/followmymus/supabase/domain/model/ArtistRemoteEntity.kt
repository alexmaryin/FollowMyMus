package io.github.alexmaryin.followmymus.supabase.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistRemoteEntity(
    @SerialName("artist_id") val artistId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userid: String? = null
)
