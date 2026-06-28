package io.github.alexmaryin.followmymus.musicBrainz.domain

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseEntity
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NewReleasesRepository {
    val workState: StateFlow<WorkState>
    val errors: Flow<ErrorType>

    fun getNewReleases(): Flow<PagingData<NewReleaseEntity>>

    suspend fun syncNewReleases(): Result<Unit>

    suspend fun markSeen(releaseId: String)

    suspend fun markDismissed(releaseId: String)
}
