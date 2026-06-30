package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel

import kotlinx.serialization.Serializable

/**
 * Shared config for the media-details panel. The per-host `FavoritesPanelConfig`
 * and `ArtistsPanelConfig` sealed types used to declare their own identical
 * `MediaDetailsConfig`; they now both reference this single type so the
 * top-level `getMediaDetails` factory can serve every host.
 */
@Serializable
data class MediaDetailsConfig(
    val releaseId: String,
    val releaseName: String,
)
