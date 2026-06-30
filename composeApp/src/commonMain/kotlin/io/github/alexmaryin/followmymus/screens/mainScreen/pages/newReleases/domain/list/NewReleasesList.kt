package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.core.paging.GroupedItem
import io.github.alexmaryin.followmymus.core.paging.groupedBy
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.getPartialSyncMessage
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.getUiDescription
import io.github.alexmaryin.followmymus.musicBrainz.domain.NewReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui.NewReleasesPanelSlots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class NewReleasesList(
    private val repository: NewReleasesRepository,
    private val context: ComponentContext,
    private val openMedia: (releaseId: String, releaseName: String) -> Unit,
) : Page, ComponentContext by context, KoinComponent {

    override val key = "NewReleasesList"
    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = NewReleasesPanelSlots(this)

    private val _state by saveableMutableValue(
        NewReleasesListState.serializer(),
        init = ::NewReleasesListState
    )
    val state: Value<NewReleasesListState> = _state

    private val pager = retainedInstance { NewReleasesPager(repository) }
    val releases: Flow<PagingData<GroupedItem<NewReleaseEntity, String>>> = pager.releases

    private val scope = context.coroutineScope()

    init {
        lifecycle.doOnStart {
            scope.launch {
                repository.workState.collect { working ->
                    _state.update { it.copy(isLoading = working == WorkState.LOADING) }
                    if (working == WorkState.PARTIAL_SYNC) {
                        events.send(SnackbarMsg(key = key, message = getPartialSyncMessage()))
                    }
                }
            }
            scope.launch {
                repository.errors.collect { error ->
                    error.getUiDescription()?.let { message ->
                        events.send(SnackbarMsg(key = key, message = message))
                    }
                }
            }
        }
    }

    operator fun invoke(action: NewReleasesListAction) {
        when (action) {
            is NewReleasesListAction.SelectRelease -> openMedia(
                action.releaseId,
                action.releaseName
            )

            is NewReleasesListAction.Dismiss -> {
                scope.launch { repository.markDismissed(action.releaseId) }
                _state.update { it.copy(dismissedIds = it.dismissedIds + action.releaseId) }
            }

            NewReleasesListAction.LoadFromRemote -> scope.launch { repository.syncNewReleases() }

            is NewReleasesListAction.OnMediaOpened -> scope.launch { repository.markSeen(action.releaseId) }

            NewReleasesListAction.RestoreAllDismissed -> {
                scope.launch { repository.restoreAllDismissed() }
                _state.update { it.copy(dismissedIds = emptyList()) }
            }

            NewReleasesListAction.UndoLastDismissal -> {
                if (_state.value.hasDismissals) {
                    val lastId = _state.value.dismissedIds.last()
                    scope.launch { repository.markUnseen(lastId) }
                    _state.update { it.copy(dismissedIds = _state.value.dismissedIds.dropLast(1)) }
                }
            }
        }
    }
}

private class NewReleasesPager(
    private val repository: NewReleasesRepository
) : InstanceKeeper.Instance {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val releases: Flow<PagingData<GroupedItem<NewReleaseEntity, String>>> =
        repository.getNewReleases()
            .map { page -> page.groupedBy { it.artistName } }
            .cachedIn(scope)

    override fun onDestroy() {
        scope.cancel()
    }
}

