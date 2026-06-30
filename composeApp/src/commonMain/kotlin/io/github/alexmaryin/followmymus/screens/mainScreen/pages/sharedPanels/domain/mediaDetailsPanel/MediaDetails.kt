package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.getPartialSyncMessage
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.getUiDescription
import io.github.alexmaryin.followmymus.musicBrainz.domain.MediaRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.mediaPanel.ReleasePanelSlots
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MediaDetails(
    private val releaseId: String,
    private val releaseName: String,
    private val context: ComponentContext
) : Page, ComponentContext by context, KoinComponent {

    private val repository by inject<MediaRepository>()

    override val events = Channel<SnackbarMsg>()
    override val key = "MediaDetails"
    override val scaffoldSlots = ReleasePanelSlots(this)

    private val _state by saveableMutableValue(
        MediaDetailsState.serializer(),
        init = { MediaDetailsState(releaseName = releaseName) }
    )
    val state: Value<MediaDetailsState> = _state

    val media = repository.getReleaseMedia(releaseId)

    val mediaCount = repository.getMediaCount(releaseId)

    private val scope = context.coroutineScope()

    init {
        lifecycle.doOnStart {
            // If Room is empty for this release, run a one-shot API sync.
            // The Pager will pick up the freshly inserted rows automatically.
            scope.launch {
                if (mediaCount.filterNotNull().first() == 0) {
                    repository.fetchReleasesMedia(releaseId)
                }
            }

            scope.launch {
                mediaCount.filterNotNull().collect { count ->
                    _state.update { it.copy(mediaCount = count) }
                }
            }

            scope.launch {
                repository.workState.collect { working ->
                    _state.update { it.copy(isLoading = working == WorkState.LOADING) }
                    if (working == WorkState.PARTIAL_SYNC) {
                        events.send(
                            SnackbarMsg(key = releaseId, message = getPartialSyncMessage())
                        )
                    }
                }
            }

            scope.launch {
                repository.errors.collect {
                    val message = it.getUiDescription()
                    message?.let { msg ->
                        events.send(
                            SnackbarMsg(key = releaseId, message = msg)
                        )
                    }
                }
            }
        }
    }
}
