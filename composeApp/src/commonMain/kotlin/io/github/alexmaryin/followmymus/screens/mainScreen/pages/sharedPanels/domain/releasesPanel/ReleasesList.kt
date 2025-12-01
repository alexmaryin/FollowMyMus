package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.BrainzApiError
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.ReleasesListTitle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class ReleasesList(
    private val repository: ReleasesRepository,
    private val artistId: String,
    private val artistName: String,
    private val context: ComponentContext,
    private val openMedia: (releaseId: String) -> Unit,
) : ComponentContext by context, ScaffoldSlots by DefaultScaffoldSlots {

    private val _state by saveableMutableValue(ReleasesListState.serializer(), init = ::ReleasesListState)
    val state: Value<ReleasesListState> = _state
    private val scope = context.coroutineScope()

    private val errors = Channel<String>(Channel.CONFLATED)
    override val snackbarMessages get() = errors.receiveAsFlow()

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
                    val message = when (it) {
                        BrainzApiError.InvalidResponse -> getString(Res.string.invalid_response_api)

                        BrainzApiError.MappingError -> getString(Res.string.mapping_error)

                        is BrainzApiError.NetworkError if it.message != null ->
                            getString(Res.string.network_error_msg, it.message)

                        is BrainzApiError.ServerError -> getString(Res.string.server_error_code, it.message ?: "")

                        BrainzApiError.Timeout -> getString(Res.string.timeout_error_msg)

                        is BrainzApiError.Unknown -> getString(Res.string.network_error_msg, it.message ?: "")

                        else -> null
                    }
                    message?.let { errors.send(message) }
                }
            }
        }
    }

    operator fun invoke(action: ReleasesListAction) {
        when (action) {
            is ReleasesListAction.OpenFullCover -> _state.update { it.copy(openedCover = action.coverUrl) }
            is ReleasesListAction.SelectRelease -> openMedia(action.releaseId)
            ReleasesListAction.CloseFullCover -> _state.update { it.copy(openedCover = null) }
            ReleasesListAction.LoadFromRemote -> scope.launch { repository.syncReleases(artistId) }
        }
    }

    override val titleContent = @Composable {
        ReleasesListTitle(
            artistName = artistName,
            actionsHandler = ::invoke
        )
    }
}