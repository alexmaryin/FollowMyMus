package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.data.asFlow
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.core.system.FileHandler
import io.github.alexmaryin.followmymus.musicBrainz.domain.FavoritesImportExportError
import io.github.alexmaryin.followmymus.musicBrainz.domain.FavoritesImportExportRepository
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
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetailsConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.getMediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.time.Clock

@OptIn(ExperimentalDecomposeApi::class, ExperimentalCoroutinesApi::class)
@Factory(binds = [FavoritesHostComponent::class])
class FavoritesHost(
    private val syncRepository: SyncRepository,
    private val fileHandler: FileHandler,
    private val favoritesImportExportRepository: FavoritesImportExportRepository,
    @InjectedParam private val componentContext: ComponentContext,
    @InjectedParam nickname: String
) : FavoritesHostComponent, ComponentContext by componentContext, KoinComponent {

    private val _state by saveableMutableValue(FavoritesHostState.serializer(), init = {
        FavoritesHostState(avatar = AvatarState(nickname))
    })
    override val state: Value<FavoritesHostState> = _state

    private val scope = componentContext.coroutineScope()

    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = FavoriteHostSlots(this)

    private val navigation =
        PanelsNavigation<FavoritesPanelConfig.ListConfig, FavoritesPanelConfig.ReleasesConfig, MediaDetailsConfig>()

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
        if (state.value.releaseIdSelected != null) {
            panels.value.details?.instance(ReleasesListAction.DeselectRelease)
        } else if (state.value.artistIdSelected != null) {
            panels.value.main.instance(FavoritesListAction.DeselectArtist)
        }
        navigation.pop()
    }

    override fun invoke(action: FavoritesHostAction) {
        when (action) {

            is FavoritesHostAction.SetMode -> {
                navigation.navigate { state -> state.copy(mode = action.mode) }
            }

            is FavoritesHostAction.ShowMediaDetails -> {
                navigation.navigate { state ->
                    state.copy(
                        extra = MediaDetailsConfig(
                            releaseId = action.releaseId,
                            releaseName = action.releaseName
                        )
                    )
                }
            }

            is FavoritesHostAction.ShowReleases -> {
                navigation.navigate { state ->
                    state.copy(
                        details = FavoritesPanelConfig.ReleasesConfig(
                            artistId = action.artistId,
                            artistName = action.artistName
                        ),
                        extra = null
                    )
                }
            }

            FavoritesHostAction.CloseReleases -> {
                navigation.dismissExtra()
                navigation.dismissDetails()
            }
            FavoritesHostAction.CloseMediaDetails -> navigation.dismissExtra()

            FavoritesHostAction.RefreshReleases -> {
                panels.value.details?.instance?.invoke(ReleasesListAction.LoadFromRemote)
            }

            is FavoritesHostAction.SyncRequested -> syncWithRemote()

            FavoritesHostAction.ExportRequested -> exportFavorites()
            FavoritesHostAction.ImportRequested -> importFavorites()

            FavoritesHostAction.OnBack -> onBack()
        }
    }

    private fun exportFavorites() = scope.launch {
        _state.update { it.copy(isExporting = true) }
        try {
            val result = favoritesImportExportRepository.serializeExport()
            result.forSuccess { payload ->
                val suggestedName = "favorites-${today()}.json"
                val path = fileHandler.saveFile(suggestedName, "application/json", payload.bytes)
                if (path != null) {
                    events.send(
                        SnackbarMsg(
                            key = "favorites.export.success",
                            message = getString(Res.string.favorites_export_success, payload.count)
                        )
                    )
                }
            }
            result.forError { _, _ ->
                events.send(
                    SnackbarMsg(
                        key = "favorites.export.error",
                        message = getString(Res.string.favorites_export_error_data_read)
                    )
                )
            }
        } catch (_: Throwable) {
            events.send(
                SnackbarMsg(
                    key = "favorites.export.error",
                    message = getString(Res.string.favorites_import_error_unknown)
                )
            )
        } finally {
            _state.update { it.copy(isExporting = false) }
        }
    }

    private fun importFavorites() = scope.launch {
        _state.update { it.copy(isImporting = true) }
        try {
            val picked = fileHandler.openFile("application/json") ?: return@launch
            val (path, bytes) = picked
            val result = favoritesImportExportRepository.importFromBytes(bytes, sourceName = path)
            result.forSuccess { summary ->
                val message = if (summary.failed == 0) {
                    getString(Res.string.favorites_import_success_clean, summary.imported, summary.skipped)
                } else {
                    getString(Res.string.favorites_import_success_with_failures, summary.imported, summary.skipped, summary.failed)
                }
                events.send(SnackbarMsg(key = "favorites.import.success", message = message))
            }
            result.forError { type, _ ->
                val message = when (type) {
                    is FavoritesImportExportError.Malformed -> getString(Res.string.favorites_import_error_malformed)
                    is FavoritesImportExportError.UnsupportedFormat -> getString(Res.string.favorites_import_error_unsupported_format)
                    is FavoritesImportExportError.UnsupportedVersion -> getString(Res.string.favorites_import_error_unsupported_version, type.version)
                    is FavoritesImportExportError.MissingArtistsField -> getString(Res.string.favorites_import_error_missing_artists)
                    is FavoritesImportExportError.EmptyArtistEntry -> getString(Res.string.favorites_import_error_empty_artist_entry, type.index)
                    is FavoritesImportExportError.NetworkError -> getString(Res.string.favorites_import_error_network)
                    else -> getString(Res.string.favorites_import_error_unknown)
                }
                events.send(SnackbarMsg(key = "favorites.import.error", message = message))
            }
        } catch (_: Throwable) {
            events.send(
                SnackbarMsg(
                    key = "favorites.import.error",
                    message = getString(Res.string.favorites_import_error_unknown)
                )
            )
        } finally {
            _state.update { it.copy(isImporting = false) }
        }
    }

    private fun today(): String {
        val now = Clock.System.now()
        val localDt = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        return "${localDt.year}-${localDt.month.number.toString().padStart(2, '0')}-${localDt.day.toString().padStart(2, '0')}"
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
                    state.copy(extra = MediaDetailsConfig(releaseId, releaseName))
                }
            }
        )
}