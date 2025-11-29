package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
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

    val resources = repository.getArtistResources(artistId).shareIn(
        scope, SharingStarted.WhileSubscribed(5000L), 1
    )

    val releases = repository.getArtistReleases(artistId).shareIn(
        scope, SharingStarted.WhileSubscribed(5000L), 1
    )

    init {
        lifecycle.doOnStart {
            scope.launch {
                releases.collect {
                    if (it.isEmpty()) {
                        _state.update { state -> state.copy(isLoading = true) }
                        repository.syncReleases(artistId)
                    } else {
                        _state.update { state -> state.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    operator fun invoke(action: ReleasesListAction) {
        when (action) {
            is ReleasesListAction.OpenFullCover -> _state.update { it.copy(openedCover = action.coverUrl) }
            is ReleasesListAction.SelectRelease -> openMedia(action.releaseId)
            ReleasesListAction.CloseFullCover -> _state.update { it.copy(openedCover = null) }
        }
    }
}