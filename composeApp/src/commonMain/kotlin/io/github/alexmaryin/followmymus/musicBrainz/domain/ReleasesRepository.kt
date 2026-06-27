package io.github.alexmaryin.followmymus.musicBrainz.domain

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ReleasesRepository {
    val workState: StateFlow<WorkState>
    val errors: Flow<ErrorType>
    val totalNetworkReleases: Flow<Int?>
    fun getArtistResources(artistId: String): Flow<Map<String, List<Resource>>>
    fun getArtistReleases(artistId: String): Flow<PagingData<Release>>
    fun getArtistReleasesCount(artistId: String): Flow<Int?>
    suspend fun syncReleases(artistId: String)
    suspend fun clearDetails(artistId: String)
}
