package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.LocalDateSerializer
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SecondaryType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Release-group as returned by the MusicBrainz search endpoint
 * `GET /ws/2/release-group?query=...&fmt=json`.
 *
 * The MB response does NOT include cover URLs (those come from the Cover Art
 * Archive). The response also omits `secondary-types` from the search result
 * (the field is only populated on the full release-group resource), so the
 * DTO field stays at its `emptyList()` default for these calls.
 *
 * `primaryType` is nullable because the API may omit the field for items
 * with no clear primary type.
 *
 * `artistCredit` carries the source of truth for the entity's denormalized
 * `artistId` / `artistName` — the repository picks the first credit whose
 * id is in the user's favorites set, falling back to the first credit (so
 * a collaboration between a favorite and a non-favorite artist is
 * attributed to the favorite one).
 */
@Serializable
data class ReleaseGroupDto(
    val id: String,
    val title: String,
    val disambiguation: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("first-release-date") val firstReleaseDate: LocalDate? = null,
    @SerialName("primary-type") val primaryType: ReleaseType? = null,
    @SerialName("secondary-types") val secondaryTypes: List<SecondaryType> = emptyList(),
    @SerialName("artist-credit") val artistCredit: List<ArtistCreditDto> = emptyList(),
)
