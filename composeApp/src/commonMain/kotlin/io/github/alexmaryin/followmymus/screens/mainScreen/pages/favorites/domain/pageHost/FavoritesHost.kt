package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.asFlow
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.RemoteSyncStatus
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesPanelConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar.Avatar
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(ExperimentalDecomposeApi::class, ExperimentalCoroutinesApi::class)
@Factory(binds = [FavoritesHostComponent::class])
class FavoritesHost(
    private val repository: ArtistsRepository,
    private val componentContext: ComponentContext,
    nickname: String
) : FavoritesHostComponent, ComponentContext by componentContext,
    ScaffoldSlots by DefaultScaffoldSlots, KoinComponent {

    private val _state by saveableMutableValue(FavoritesHostState.serializer(), init = ::FavoritesHostState)
    override val state: Value<FavoritesHostState> = _state

    private val avatarState = MutableValue(AvatarState(nickname))
    private val scope = componentContext.coroutineScope()

    private val navigation =
        PanelsNavigation<FavoritesPanelConfig.ListConfig, FavoritesPanelConfig.ReleasesConfig, FavoritesPanelConfig.MediaDetailsConfig>()

    private val _panels = childPanels(
        source = navigation,
        serializers = FavoritesPanelConfig.SERIALIZERS,
        initialPanels = { Panels(main = FavoritesPanelConfig.ListConfig) },
        onStateChanged = { new, _ ->
            val backIsVisible = when {
                new.extra != null -> true
                new.details != null && new.mode == ChildPanelsMode.SINGLE -> true
                else -> false
            }
            _state.update {
                it.copy(
                    artistIdSelected = new.details?.artistId,
                    releaseIdSelected = new.extra?.releaseId,
                    backVisible = backIsVisible
                )
            }
        },
        handleBackButton = true,
        mainFactory = { _, ctx -> getFavoritesList(ctx) },
        detailsFactory = ::getReleasesList,
        extraFactory = ::getMediaDetails
    )

    override val panels: Value<ChildPanels<*, FavoritesList, *, ReleasesList, *, MediaDetails>> = _panels

    private val _events = Channel<SnackbarMsg>()
    override val snackbarMessages = _events.receiveAsFlow().distinctUntilChanged()

    init {
        panels.asFlow().flatMapLatest { updatePanels ->
            merge(
                updatePanels.main.instance.snackbarMessages,
                updatePanels.details?.instance?.snackbarMessages ?: emptyFlow(),
                updatePanels.extra?.instance?.snackbarMessages ?: emptyFlow()
            )
        }
            .distinctUntilChanged()
            .onEach { message -> _events.send(message) }
            .launchIn(scope)


        lifecycle.doOnStart {
            scope.launch {
                repository.syncStatus.collect { status ->
                    avatarState.update {
                        it.copy(isSyncing = status is RemoteSyncStatus.Process)
                    }
                    if (status is RemoteSyncStatus.Error) {
                        _events.send(
                            SnackbarMsg(key, status.errors.joinToString())
                        )
                    }
                }
            }
            scope.launch {
                repository.checkPendingActions()
                repository.hasPendingActions.collect { hasPending ->
                    avatarState.update {
                        it.copy(hasPending = hasPending)
                    }
                }
            }
        }
    }

    private fun onBack() {
        if (state.value.releaseIdSelected != null)
            panels.value.details?.instance(ReleasesListAction.DeselectRelease)
        else if (state.value.artistIdSelected != null)
            panels.value.main.instance(FavoritesListAction.DeselectArtist)
        navigation.pop()
    }

    override fun invoke(action: FavoritesHostAction) {
        when (action) {

            is FavoritesHostAction.SetMode -> {
                navigation.navigate { state -> state.copy(mode = action.mode) }
            }

            is FavoritesHostAction.ShowMediaDetails -> {
                navigation.navigate { state ->
                    state.copy(extra = FavoritesPanelConfig.MediaDetailsConfig(releaseId = action.releaseId))
                }
            }

            is FavoritesHostAction.ShowReleases -> {
                navigation.navigate { state ->
                    state.copy(
                        details = FavoritesPanelConfig.ReleasesConfig(
                            artistId = action.artistId,
                            artistName = action.artistName
                        )
                    )
                }
            }

            FavoritesHostAction.CloseReleases -> navigation.dismissDetails()

            FavoritesHostAction.RefreshReleases -> {
                panels.value.details?.instance?.invoke(ReleasesListAction.LoadFromRemote)
            }

            is FavoritesHostAction.SyncRequested -> syncWithRemote()

            FavoritesHostAction.OnBack -> onBack()
        }
    }

    private fun syncWithRemote() {
        scope.launch {
            repository.syncRemote()
        }
    }

    private fun getFavoritesList(context: ComponentContext) =
        FavoritesList(get(), context, ::invoke)

    private fun getReleasesList(config: FavoritesPanelConfig.ReleasesConfig, context: ComponentContext) =
        ReleasesList(
            get(), config.artistId, config.artistName, context,
            openMedia = { releaseId ->
                navigation.navigate { state ->
                    state.copy(extra = FavoritesPanelConfig.MediaDetailsConfig(releaseId))
                }
            }
        )

    private fun getMediaDetails(config: FavoritesPanelConfig.MediaDetailsConfig, context: ComponentContext) =
        MediaDetails(config.releaseId, context)


    // Title bar overriding and propagating to children panels

    override val leadingIcon = @Composable {
        val state = state.subscribeAsState()
        if (state.value.backVisible) BackIcon(::onBack)
    }

    override val titleContent = @Composable {
        val panelsState by panels.subscribeAsState()
        val releasesPanel = panelsState.details?.instance
        val mediaPanel = panelsState.extra?.instance
        val singleMode = panelsState.mode == ChildPanelsMode.SINGLE
        val title = when {
            mediaPanel != null -> mediaPanel.titleContent
            singleMode && releasesPanel != null -> releasesPanel.titleContent
            else -> panelsState.main.instance.titleContent
        }
        title()
    }

    override val trailingIcon: @Composable (RowScope.() -> Unit) = {
        val avatarState = avatarState.subscribeAsState()
        Avatar(
            state = avatarState.value,
            modifier = Modifier.padding(4.dp),
            onSyncRequest = ::syncWithRemote
        )
    }
}