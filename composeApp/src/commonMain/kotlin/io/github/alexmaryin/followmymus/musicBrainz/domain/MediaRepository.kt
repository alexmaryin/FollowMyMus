package io.github.alexmaryin.followmymus.musicBrainz.domain

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Media
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val workState: StateFlow<WorkState>
    val errors: Flow<ErrorType>
    val totalNetworkMedia: Flow<Int?>

    fun getReleaseMedia(releaseId: String): Flow<PagingData<Media>>
    fun getMediaCount(releaseId: String): Flow<Int?>

    suspend fun fetchReleasesMedia(releaseId: String)
}
