package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One entry in the `artist-credit` array returned by the MusicBrainz release-group
 * search endpoint. A release-group by a single artist has one entry; a
 * collaboration has several, joined by [joinphrase] (e.g. " & ").
 */
@Serializable
data class ArtistCreditDto(
    val name: String,
    val joinphrase: String? = null,
    val artist: ArtistRefDto,
)

/**
 * Lightweight projection of an artist as carried by the search response's
 * `artist-credit[].artist` field. We only model the fields the new-releases
 * mapper needs (id, name) plus a few that may be useful for debugging or
 * future sorting (`sortName`, `disambiguation`).
 *
 * `aliases` is intentionally omitted — `ignoreUnknownKeys = true` on the
 * Ktor client drops it without complaint.
 */
@Serializable
data class ArtistRefDto(
    val id: String,
    val name: String,
    @SerialName("sort-name") val sortName: String,
    val disambiguation: String? = null,
)
