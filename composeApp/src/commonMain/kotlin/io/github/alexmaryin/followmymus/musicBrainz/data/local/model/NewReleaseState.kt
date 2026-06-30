package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

/**
 * Lifecycle state of a release-group row in the local `new_releases` table.
 *
 * State transitions (one-way, per the new-releases-page spec):
 * - `UNSEEN` is the default for newly-inserted rows.
 * - `UNSEEN` -> `SEEN` when the user opens the release in the media details panel.
 * - `SEEN` -> `DISMISSED` when the user swipes the row to dismiss.
 * - `DISMISSED` rows are filtered out of the list and never re-surfaced.
 *
 * `String` form is what Room stores on disk; the Kotlin enum name is the
 * wire format. The `getNewReleases` query filters on this string literal
 * (e.g. `WHERE state != 'DISMISSED'`).
 */
enum class NewReleaseState {
    UNSEEN,
    SEEN,
    DISMISSED,
}
