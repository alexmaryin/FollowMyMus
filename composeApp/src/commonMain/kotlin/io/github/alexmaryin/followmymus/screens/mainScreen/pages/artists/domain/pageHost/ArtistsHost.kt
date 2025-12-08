package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsPanelConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.ArtistsHostSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import kotlinx.coroutines.channels.Channel
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(ExperimentalDecomposeApi::class)
@Factory(binds = [ArtistsHostComponent::class])
class ArtistsHost(
    private val componentContext: ComponentContext
) : ArtistsHostComponent, ComponentContext by componentContext, KoinComponent {

    private val _state by saveableMutableValue(ArtistsHostState.serializer(), init = ::ArtistsHostState)
    override val state: Value<ArtistsHostState> = _state

    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = ArtistsHostSlots(this)

    private val navigation =
        PanelsNavigation<Unit, ArtistsPanelConfig.ReleasesConfig, ArtistsPanelConfig.MediaDetailsConfig>()

    private val _panels = childPanels(
        source = navigation,
        serializers = ArtistsPanelConfig.SERIALIZERS,
        initialPanels = { Panels(main = Unit) },
        key = "ArtistsPanels",
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
        mainFactory = { _, context -> ArtistsList(get(), context, ::invoke) },
        detailsFactory = ::getReleasesList,
        extraFactory = ::getMediaDetails
    )

    override val panels: Value<ChildPanels<*, ArtistsList, *, ReleasesList, *, MediaDetails>> = _panels

    private fun getReleasesList(config: ArtistsPanelConfig.ReleasesConfig, context: ComponentContext) =
        ReleasesList(
            get(), config.artistId, config.artistName, context,
            openMedia = { releaseId ->
                navigation.navigate { state ->
                    state.copy(extra = ArtistsPanelConfig.MediaDetailsConfig(releaseId))
                }
            }
        )

    private fun getMediaDetails(config: ArtistsPanelConfig.MediaDetailsConfig, context: ComponentContext) =
        MediaDetails(config.releaseId, context)

    private fun onBack() {
        if (state.value.releaseIdSelected != null)
            panels.value.details?.instance(ReleasesListAction.DeselectRelease)
        else if (state.value.artistIdSelected != null)
            panels.value.main.instance(ArtistsListAction.CloseReleases)
        navigation.pop()
    }

    override fun invoke(action: ArtistsHostAction) {
        when (action) {
            is ArtistsHostAction.ShowReleases -> {
                navigation.navigate { state ->
                    state.copy(
                        details = ArtistsPanelConfig.ReleasesConfig(
                            artistId = action.artistId,
                            artistName = action.artistName
                        )
                    )
                }
            }

            ArtistsHostAction.CloseReleases -> navigation.dismissDetails()

            ArtistsHostAction.CloseMediaDetails -> navigation.dismissExtra()

            is ArtistsHostAction.SetMode -> {
                navigation.navigate { state -> state.copy(mode = action.mode) }
            }

            is ArtistsHostAction.ShowMediaDetails -> {
                navigation.navigate { state ->
                    state.copy(extra = ArtistsPanelConfig.MediaDetailsConfig(action.releaseId))
                }
            }

            ArtistsHostAction.OnBack -> onBack()
        }
    }
}