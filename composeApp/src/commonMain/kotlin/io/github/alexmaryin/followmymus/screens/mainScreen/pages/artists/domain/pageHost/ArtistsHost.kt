package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsPanelConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import org.koin.core.annotation.Factory

@OptIn(ExperimentalDecomposeApi::class)
@Factory(binds = [ArtistsHostComponent::class])
class ArtistsHost(
    private val componentContext: ComponentContext
) : ArtistsHostComponent, ComponentContext by componentContext,
    ScaffoldSlots by DefaultScaffoldSlots {

    private val _state = MutableValue(ArtistsHostState())
    override val state: Value<ArtistsHostState> = _state

    private val navigation =
        PanelsNavigation<Unit, ArtistsPanelConfig.ReleasesConfig, ArtistsPanelConfig.MediaDetailsConfig>()

    private val _panels = childPanels(
        source = navigation,
        serializers = ArtistsPanelConfig.SERIALIZERS,
        initialPanels = { Panels(main = Unit) },
        onStateChanged = { new, _ ->
            _state.update {
                it.copy(artistIdSelected = new.details?.artistId, releaseIdSelected = new.extra?.releaseId)
            }
        },
        handleBackButton = true,
        mainFactory = { _, context -> ArtistsList(context, ::invoke) },
        detailsFactory = ::getReleasesList,
        extraFactory = ::getMediaDetails
    )

    override val panels: Value<ChildPanels<*, ArtistsList, *, ReleasesList, *, MediaDetails>> = _panels

    private fun getReleasesList(config: ArtistsPanelConfig.ReleasesConfig, context: ComponentContext) =
        ReleasesList(config.artistId, context)

    private fun getMediaDetails(config: ArtistsPanelConfig.MediaDetailsConfig, context: ComponentContext) =
        MediaDetails(config.releaseId, context)

    private fun onBack() {
        navigation.pop()
    }

    private fun showReleases(artistId: String) {
        _state.update {
            it.copy(
                artistIdSelected = artistId,
                backVisible = panels.value.mode == ChildPanelsMode.SINGLE
            )
        }
        navigation.navigate { state ->
            state.copy(details = ArtistsPanelConfig.ReleasesConfig(artistId))
        }
    }

    override fun invoke(action: ArtistsHostAction) {
        when (action) {
            is ArtistsHostAction.ShowReleases -> showReleases(action.artistId)

            ArtistsHostAction.CloseReleases -> {
                _state.update { it.copy(artistIdSelected = null) }
                navigation.navigate { state -> state.copy(details = null) }
            }

            ArtistsHostAction.CloseMediaDetails -> {
                _state.update { it.copy(releaseIdSelected = null) }
                navigation.navigate { state -> state.copy(extra = null) }
            }

            is ArtistsHostAction.SetMode -> navigation.navigate { state -> state.copy(mode = action.mode) }

            is ArtistsHostAction.ShowMediaDetails -> {
                _state.update { it.copy(releaseIdSelected = action.releaseId, backVisible = true) }
                navigation.navigate { state ->
                    state.copy(extra = ArtistsPanelConfig.MediaDetailsConfig(action.releaseId))
                }
            }

            ArtistsHostAction.OnBack -> onBack()
        }
    }

    override val leadingIcon = @Composable {
        val state = state.subscribeAsState()
        if (state.value.backVisible) BackIcon(::onBack)
    }

    override val titleContent = @Composable {
        val panelState by panels.subscribeAsState()
        val isSearchVisible = panelState.mode != ChildPanelsMode.SINGLE || panelState.details == null
        if (isSearchVisible) panelState.main.instance.ProvideArtistsSearchBar()
        else DefaultScaffoldSlots.titleContent()
    }

    override val fabContent: @Composable () -> Unit = { panels.value.main.instance.FabContent() }
}