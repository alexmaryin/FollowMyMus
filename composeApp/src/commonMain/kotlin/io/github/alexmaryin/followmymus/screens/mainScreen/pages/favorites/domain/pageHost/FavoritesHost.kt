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
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.RemoteSyncStatus
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
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
) : FavoritesHostComponent, ComponentContext by componentContext,
    ScaffoldSlots by DefaultScaffoldSlots {

    private val _state by saveableMutableValue(FavoritesHostState.serializer(), init = ::FavoritesHostState)
    override val state: Value<FavoritesHostState> = _state

    private val avatarState = MutableValue(AvatarState(nickname))
    private val scope = componentContext.coroutineScope() + SupervisorJob()

    private val _events = MutableSharedFlow<String>()
    override val snackbarMessages = _events.asSharedFlow()

    init {
        lifecycle.doOnStart {
            scope.launch {
                repository.syncStatus.collect { status ->
                    avatarState.update {
                        it.copy(isSyncing = status is RemoteSyncStatus.Process)
                    }
                    if (status is RemoteSyncStatus.Error) {
                        _events.emit(status.errors.joinToString())
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

    private val navigation =
        PanelsNavigation<FavoritesPanelConfig.ListConfig, FavoritesPanelConfig.ReleasesConfig, FavoritesPanelConfig.MediaDetailsConfig>()

    private val _panels = childPanels(
        source = navigation,
        serializers = FavoritesPanelConfig.SERIALIZERS,
        initialPanels = { Panels(main = FavoritesPanelConfig.ListConfig(SortArtists.NONE)) },
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
                    selectedSorting = new.main.sortingType,
                    backVisible = backIsVisible
                )
            }
        },
        handleBackButton = true,
        mainFactory = ::getFavoritesList,
        detailsFactory = ::getReleasesList,
        extraFactory = ::getMediaDetails
    )

    override val panels: Value<ChildPanels<*, FavoritesList, *, ReleasesList, *, MediaDetails>> = _panels

    private fun onBack() {
        navigation.pop()
    }

    override val leadingIcon = @Composable {
        val state = state.subscribeAsState()
        if (state.value.backVisible) BackIcon(::onBack)
    }

    override fun invoke(action: FavoritesHostAction) {
        when (action) {
            FavoritesHostAction.CloseMediaDetails -> {
                navigation.navigate { state -> state.copy(extra = null) }
            }

            FavoritesHostAction.CloseReleases -> {
                navigation.navigate { state -> state.copy(details = null, extra = null) }
            }

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
                    state.copy(details = FavoritesPanelConfig.ReleasesConfig(artistId = action.artistId))
                }
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

    private fun getFavoritesList(config: FavoritesPanelConfig.ListConfig, context: ComponentContext) =
        FavoritesList(config.sortingType, context, ::invoke)

    private fun getReleasesList(config: FavoritesPanelConfig.ReleasesConfig, context: ComponentContext) =
        ReleasesList(config.artistId, context)

    private fun getMediaDetails(config: FavoritesPanelConfig.MediaDetailsConfig, context: ComponentContext) =
        MediaDetails(config.releaseId, context)

    override val titleContent = @Composable {
        val avatarState = avatarState.subscribeAsState()
        FavoritesTitleBar(
            avatarState = avatarState.value,
            onSyncRequest = ::syncWithRemote,
            selectedSorting = state.value.selectedSorting,
            onFilterChange = { new ->
                navigation.navigate { state ->
                    state.copy(main = FavoritesPanelConfig.ListConfig(new))
                }
            }
        )
    }
}