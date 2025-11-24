package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.rootNavigation.ui.saveableMutableValue
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PageAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.RemoteSyncStatus
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesPanelConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.FavoritesTitleBar
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.annotation.Factory

@OptIn(ExperimentalDecomposeApi::class)
@Factory(binds = [FavoritesHostComponent::class])
class FavoritesHost(
    private val repository: ArtistsRepository,
    private val componentContext: ComponentContext,
    nickname: String
) : FavoritesHostComponent, ComponentContext by componentContext {

    private val _state by saveableMutableValue(FavoritesHostState.serializer(), init = ::FavoritesHostState)
    override val state: Value<FavoritesHostState> = _state

    private val avatarState = MutableValue(AvatarState(nickname))
    private val scope = componentContext.coroutineScope() + SupervisorJob()

    private val _events = MutableSharedFlow<FavoritesHostEvent>()
    override val events = _events.asSharedFlow()

    init {
        lifecycle.doOnStart {
            scope.launch {
                repository.syncStatus.collect { status ->
                    avatarState.update { it.copy(
                        isSyncing = status is RemoteSyncStatus.Process
                    ) }
                    if (status is RemoteSyncStatus.Error) {
                        val text = status.errors.joinToString()
                        _events.emit(FavoritesHostEvent.Error(text))
                    }
                }
            }
            scope.launch {
                repository.checkPendingActions()
                repository.hasPendingActions.collect { hasPending ->
                    avatarState.update { it.copy(
                        hasPending = hasPending
                    ) }
                }
            }
        }
    }

    private val navigation =
        PanelsNavigation<Unit, FavoritesPanelConfig.ReleasesConfig, FavoritesPanelConfig.MediaDetailsConfig>()

    private val _panels = childPanels(
        source = navigation,
        serializers = FavoritesPanelConfig.SERIALIZERS,
        initialPanels = { Panels(main = Unit) },
        onStateChanged = { new, _ ->
            _state.update {
                it.copy(artistIdSelected = new.details?.artistId, releaseIdSelected = new.extra?.releaseId)
            }
        },
        handleBackButton = true,
        mainFactory = { _, context -> FavoritesList(context) },
        detailsFactory = ::getReleasesList,
        extraFactory = ::getMediaDetails
    )

    override val panels: Value<ChildPanels<*, FavoritesList, *, ReleasesList, *, MediaDetails>> = _panels

    override fun invoke(action: PageAction) {
        when (action) {
            PageAction.Back -> navigation.pop()
        }
    }

    override fun invoke(action: FavoritesHostAction) {
        when (action) {
            FavoritesHostAction.CloseMediaDetails -> {
                _state.update { it.copy(releaseIdSelected = null) }
                navigation.navigate { state -> state.copy(extra = null) }
            }

            is FavoritesHostAction.SetMode -> navigation.navigate { state -> state.copy(mode = action.mode) }

            is FavoritesHostAction.ShowMediaDetails -> {
                _state.update { it.copy(releaseIdSelected = action.releaseId, backVisible = true) }
                navigation.navigate { state ->
                    state.copy(extra = FavoritesPanelConfig.MediaDetailsConfig(releaseId = action.releaseId))
                }
            }

            is FavoritesHostAction.ShowReleases -> {
                _state.update {
                    it.copy(
                        artistIdSelected = action.artistId,
                        backVisible = panels.value.mode == ChildPanelsMode.SINGLE
                    )
                }
                navigation.navigate { state ->
                    state.copy(details = FavoritesPanelConfig.ReleasesConfig(artistId = action.artistId))
                }
            }

            is FavoritesHostAction.SyncRequested -> syncWithRemote()
        }
    }

    private fun syncWithRemote() {
        scope.launch {
            repository.syncRemote()
        }
    }

    private fun getReleasesList(config: FavoritesPanelConfig.ReleasesConfig, context: ComponentContext) =
        ReleasesList(config.artistId, context)

    private fun getMediaDetails(config: FavoritesPanelConfig.MediaDetailsConfig, context: ComponentContext) =
        MediaDetails(config.releaseId, context)

    override val contentIsVisible = true

    override val content = @Composable {
        val state = avatarState.subscribeAsState()
        FavoritesTitleBar(
            avatarState = state.value,
            onSyncRequest = ::syncWithRemote
        )
    }
}