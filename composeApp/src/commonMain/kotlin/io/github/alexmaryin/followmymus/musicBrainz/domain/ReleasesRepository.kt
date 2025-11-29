package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ReleaseType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import kotlinx.coroutines.flow.Flow

interface ReleasesRepository {
    fun getArtistResources(artistId: String): Flow<Map<String, List<Resource>>>
    fun getArtistReleases(artistId: String): Flow<Map<ReleaseType, List<Release>>>
    suspend fun syncReleases(artistId: String)
    suspend fun clearDetails(artistId: String)
}