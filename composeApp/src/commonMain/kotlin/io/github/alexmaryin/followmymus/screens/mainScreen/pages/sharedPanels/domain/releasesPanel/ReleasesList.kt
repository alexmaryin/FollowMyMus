package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.getUiDescription
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.ReleasesPanelSlots
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReleasesList(
    private val repository: ReleasesRepository,
    private val artistId: String,
    private val artistName: String,
    private val context: ComponentContext,
    private val openMedia: (releaseId: String) -> Unit,
) : Page, ComponentContext by context {

    private val _state by saveableMutableValue(ReleasesListState.serializer(), init = {
        ReleasesListState(artistName = artistName)
    })
    val state: Value<ReleasesListState> = _state
    private val scope = context.coroutineScope()

    override val key = "ReleasesPanel"

    override val events = Channel<SnackbarMsg>()

    override val scaffoldSlots = ReleasesPanelSlots(this)

    val resources = repository.getArtistResources(artistId)

    val releases = repository.getArtistReleases(artistId)

    init {
        lifecycle.doOnStart {

            scope.launch {
                if (releases.first().isEmpty()) {
                    repository.syncReleases(artistId)
                }
            }

            scope.launch {
                repository.workState.collect { working ->
                    _state.update { it.copy(isLoading = working == WorkState.LOADING) }
                }
            }

            scope.launch {
                repository.errors.collect {
                    val message = it.getUiDescription()
                    message?.let { msg ->
                        events.send(
                            SnackbarMsg(key = artistId, message = msg)
                        )
                    }
                }
            }
        }
    }

    operator fun invoke(action: ReleasesListAction) {
        when (action) {
            is ReleasesListAction.OpenFullCover -> _state.update { it.copy(openedCover = action.coverUrl) }
            is ReleasesListAction.SelectRelease -> openMedia(action.releaseId)
            ReleasesListAction.DeselectRelease -> _state.update { it.copy(selectedRelease = null) }
            ReleasesListAction.CloseFullCover -> _state.update { it.copy(openedCover = null) }
            ReleasesListAction.LoadFromRemote -> scope.launch { repository.syncReleases(artistId) }
        }
    }
}