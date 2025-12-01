package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ReleasesRepository {
    val workState: StateFlow<WorkState>
    val errors: Flow<ErrorType>
    fun getArtistResources(artistId: String): Flow<Map<String, List<Resource>>>
    fun getArtistReleases(artistId: String): Flow<Map<String, List<Release>>>
    suspend fun syncReleases(artistId: String)
    suspend fun clearDetails(artistId: String)
}

enum class WorkState {
    IDLE, LOADING, COVERING
}