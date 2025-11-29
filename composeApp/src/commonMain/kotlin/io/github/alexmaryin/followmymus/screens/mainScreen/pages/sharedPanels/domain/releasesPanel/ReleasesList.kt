package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.WorkState
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class ReleasesList(
    private val repository: ReleasesRepository,
    private val artistId: String,
    private val context: ComponentContext,
    private val openMedia: (releaseId: String) -> Unit,
) : ComponentContext by context {

    private val _state by saveableMutableValue(ReleasesListState.serializer(), init = ::ReleasesListState)
    val state: Value<ReleasesListState> = _state

    private val scope = context.coroutineScope() + SupervisorJob()

    val resources = repository.getArtistResources(artistId)

    val releases = repository.getArtistReleases(artistId)

    init {
        lifecycle.doOnStart {

            scope.launch {
                if (releases.first().isEmpty()) repository.syncReleases(artistId)
            }

            scope.launch {
                repository.workState.collect { working ->
                    _state.update { it.copy(isLoading = working == WorkState.LOADING) }
                }
            }
        }
    }

    operator fun invoke(action: ReleasesListAction) {
        when (action) {
            is ReleasesListAction.OpenFullCover -> _state.update { it.copy(openedCover = action.coverUrl) }
            is ReleasesListAction.SelectRelease -> openMedia(action.releaseId)
            ReleasesListAction.CloseFullCover -> _state.update { it.copy(openedCover = null) }
            ReleasesListAction.LoadFromRemote -> scope.launch {
                repository.syncReleases(artistId)
            }

        }
    }
}