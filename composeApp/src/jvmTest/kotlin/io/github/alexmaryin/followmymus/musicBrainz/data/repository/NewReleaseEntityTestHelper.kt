package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseState
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseType
import kotlin.time.Instant

/** Convenience builder used by [NewReleasesRepositoryTest]. */
internal fun newReleaseEntity(
    id: String,
    title: String = "Title $id",
    artistId: String = "artist-1",
    artistName: String = "Artist 1",
    firstReleaseDate: String? = null,
    state: NewReleaseState = NewReleaseState.UNSEEN,
): NewReleaseEntity = NewReleaseEntity(
    id = id,
    artistId = artistId,
    artistName = artistName,
    title = title,
    disambiguation = null,
    firstReleaseDate = firstReleaseDate,
    primaryType = ReleaseType.ALBUM,
    secondaryTypes = null,
    coverFrontUrl = null,
    state = state,
    discoveredAt = Instant.parse("2026-06-28T14:23:11.482Z"),
)
