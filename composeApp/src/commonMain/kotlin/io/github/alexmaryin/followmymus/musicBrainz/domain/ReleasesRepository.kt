package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.ArtistReleases
import kotlinx.coroutines.flow.Flow

interface ReleasesRepository {
    fun searchArtistReleases(artistId: String): Flow<ArtistReleases>
}