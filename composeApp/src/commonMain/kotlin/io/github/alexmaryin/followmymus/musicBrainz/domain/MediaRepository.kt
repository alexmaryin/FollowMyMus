package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Media
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val workState: StateFlow<WorkState>
    val errors: Flow<ErrorType>

    fun getReleaseMedia(releaseId: String): Flow<List<Media>>

    suspend fun fetchReleasesMedia(releaseId: String)
}