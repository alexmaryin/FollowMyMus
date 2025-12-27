package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.asFlow
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.domain.SyncRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.RemoteSyncStatus
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesPanelConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.FavoriteHostSlots
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
    private val syncRepository: SyncRepository,
    private val componentContext: ComponentContext,
    nickname: String
) : FavoritesHostComponent, ComponentContext by componentContext, KoinComponent {

    private val _state by saveableMutableValue(FavoritesHostState.serializer(), init = {
        FavoritesHostState(avatar = AvatarState(nickname))
    })
    override val state: Value<FavoritesHostState> = _state

    private val scope = componentContext.coroutineScope()

    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = FavoriteHostSlots(this)

    private val navigation =
        PanelsNavigation<FavoritesPanelConfig.ListConfig, FavoritesPanelConfig.ReleasesConfig, FavoritesPanelConfig.MediaDetailsConfig>()

    private val _panels = childPanels(
        source = navigation,
        serializers = FavoritesPanelConfig.SERIALIZERS,
        initialPanels = { Panels(main = FavoritesPanelConfig.ListConfig) },
        key = "FavoritesPanels",
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

    init {
        panels.asFlow().flatMapLatest { updatePanels ->
            merge(
                updatePanels.main.instance.scaffoldSlots.snackbarMessages,
                updatePanels.details?.instance?.scaffoldSlots?.snackbarMessages ?: emptyFlow(),
                updatePanels.extra?.instance?.scaffoldSlots?.snackbarMessages ?: emptyFlow()
            )
        }
            .distinctUntilChanged()
            .onEach { message -> events.send(message) }
            .launchIn(scope)


        lifecycle.doOnStart {
            scope.launch {
                syncRepository.syncStatus.collect { status ->
                    _state.update {
                        it.copy(avatar = it.avatar.copy(isSyncing = status is RemoteSyncStatus.Process))
                    }
                    if (status is RemoteSyncStatus.Error) {
                        events.send(
                            SnackbarMsg(key, status.errors.joinToString())
                        )
                    }
                }
            }
            scope.launch {
                syncRepository.checkPendingActions()
                syncRepository.hasPendingActions.collect { hasPending ->
                    _state.update {
                        it.copy(avatar = it.avatar.copy(hasPending = hasPending))
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
                    state.copy(extra = FavoritesPanelConfig.MediaDetailsConfig(
                        releaseId = action.releaseId,
                        releaseName = action.releaseName
                    ))
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

            FavoritesHostAction.CloseMediaDetails -> navigation.dismissExtra()

            FavoritesHostAction.RefreshReleases -> {
                panels.value.details?.instance?.invoke(ReleasesListAction.LoadFromRemote)
            }

            is FavoritesHostAction.SyncRequested -> syncWithRemote()

            FavoritesHostAction.OnBack -> onBack()
        }
    }

    private fun syncWithRemote() = scope.launch {
        syncRepository.syncRemote()
    }

    private fun getFavoritesList(context: ComponentContext) =
        FavoritesList(get(), syncRepository, context, ::invoke)

    private fun getReleasesList(config: FavoritesPanelConfig.ReleasesConfig, context: ComponentContext) =
        ReleasesList(
            get(), config.artistId, config.artistName, context,
            openMedia = { releaseId, releaseName ->
                navigation.navigate { state ->
                    state.copy(extra = FavoritesPanelConfig.MediaDetailsConfig(releaseId, releaseName))
                }
            }
        )

    private fun getMediaDetails(config: FavoritesPanelConfig.MediaDetailsConfig, context: ComponentContext) =
        MediaDetails(config.releaseId, config.releaseName, get(), context)
}